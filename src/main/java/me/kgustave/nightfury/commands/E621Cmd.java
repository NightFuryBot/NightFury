/*
 * Copyright 2017 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kgustave.nightfury.commands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.menu.slideshow.SlideshowBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;

import me.kgustave.nightfury.api.E621API;
import me.kgustave.nightfury.manager.NightFuryManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 *
 * @author Kaidan Gustave
 */
public class E621Cmd extends NightFuryCommand {
    
    private final Random random = new Random();
    private final E621API e621;
    private final SlideshowBuilder builder;
    private final EventWaiter waiter;
    
    public E621Cmd(NightFuryManager manager, E621API e621, EventWaiter waiter) {
        super(manager);
        this.name = "e621";
        this.aliases = new String[]{"lewd", "porn", "pron"};
        this.arguments = "[limit] <tags...>";
        this.help = "gets a slideshow of up to 100 images from a query with the given tags";
        this.category = new Category("NSFW Commands", event -> event.isFromType(ChannelType.TEXT) && event.getTextChannel().isNSFW());
        this.e621 = e621;
        this.builder = new SlideshowBuilder()
                .setTimeout(30, TimeUnit.SECONDS)
                .setEventWaiter(waiter);
        this.waiter = waiter;
        this.helpBiConsumer = (event, command) -> {
            if(!command.getCategory().test(event))
                return;
            event.reactSuccess();
            String prefix = event.getClient().getPrefix();
            String name = command.getName();
            String str = "**__Help for "+name+" Command__**:\n\n"
                       + "**Aliases**:\n";
            for(int i=0; i<command.getAliases().length; i++) {
                str += "`"+prefix+command.getAliases()[i]+"`";
                if(i!=command.getAliases().length-1)
                    str += ", ";
                else
                    str += "\n\n";
            }
            str += "**Usage**:\n`"+prefix+name+" "+command.getArguments()+"`\n\n";
            str += "**Notes**:\n -> Specifying a limit is optional.\n -> You must specify at least 1 tag.\n\n";
            str += "**SubCommands**:\n";
            for(Command child : command.getChildren()) {
                str += "`"+child.getName()+" "+child.getArguments()+"` - "+child.getHelp();
            }
            event.replyInDM(str);
        };
        this.children = new NightFuryCommand[]{
             new RandomCmd(manager, e621)
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty()) {
            event.replyError("Invalid arguments! See `"+event.getClient().getPrefix()+this.getName()+" "+event.getClient().getHelpWord()+"` for more info!");
            return;
        }
        int limit = 100;
        String[] args = event.getArgs().split("\\s+");
        String[] tags;
        if(args[0].matches("\\d+")) {
            limit = Integer.parseInt(args[0]);
            tags = Arrays.copyOfRange(args, 1, args.length-1);
            if(limit<1) {
                event.replyError("Cannot have a limit less than 1!");
                return;
            }
        } else {
            tags = args;
        }
        JSONArray arr = e621.search(limit, tags);
        if(arr==null || arr.length()==0) {
            event.replyError("Failed to find any posts matching the provided tags!");
            return;
        }
        List<String> list = new ArrayList<>();
        for(Object obj : arr){
            String url = ((JSONObject) obj).getString("file_url");
            if(url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".gif"))
                list.add(url);
        }
        String format = "[Link](https://e621.net/post/show/%d/)";
        event.getMessage().delete().queue();
        builder
        .setDescription((page, total) -> String.format(format, arr.getJSONObject(page-1).getLong("id")))
        .setText((page, total) -> String.format("Showing result %d out of %d", page, total))
        .setFinalAction((m) -> {
            m.clearReactions().queue();
            event.reply("Type `"+event.getClient().getPrefix()+"save` to prevent the image from being deleted", msg -> {
                waiter.waitForEvent(MessageReceivedEvent.class, (mre) -> {
                    return mre.getMessage().getContent().equalsIgnoreCase(event.getClient().getPrefix()+"save") && mre.getAuthor().equals(event.getAuthor());
                }, (mre) -> {
                    MessageEmbed embed = m.getEmbeds().get(0);
                    m.editMessage(new EmbedBuilder().setDescription(embed.getDescription()).setColor(embed.getColor()).setImage(embed.getImage().getUrl()).build()).queue();
                    mre.getMessage().delete().queue();
                    msg.delete().queue();
                }, 10, TimeUnit.SECONDS, () -> {
                    msg.delete().queue();
                    m.delete().queue();
                });
            });
        })
        .setColor((x,y) -> {
            int red = random.nextInt(256);
            int blue = random.nextInt(256);
            int green = random.nextInt(256);
            return new Color(red, blue, green);
        })
        .waitOnSinglePage(false)
        .setUrls(list.toArray(new String[]{}))
        .addUsers(event.getAuthor()).build()
        .display(event.getChannel());
    }
    
    private class RandomCmd extends NightFuryCommand {
        
        private final E621API e621;
        
        public RandomCmd(NightFuryManager manager, E621API e621) {
            super(manager);
            this.name = "random";
            this.arguments = "<tags...>";
            this.help = "gets a single random image from a query with the given tags";
            this.category = new Category("NSFW Commands", event -> event.isFromType(ChannelType.TEXT) && event.getTextChannel().isNSFW());
            this.e621 = e621;
        }
        
        @Override
        protected void execute(CommandEvent event) {
            JSONArray arr = e621.search(100, event.getArgs().split("\\s+"));
            event.reply(new MessageBuilder().append("Result for "+event.getArgs()).setEmbed(new EmbedBuilder()
                            .setImage(arr.getJSONObject(random.nextInt(arr.length())).getString("file_url"))
                            .setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                            .build()).build());
        }
        
    }
}
