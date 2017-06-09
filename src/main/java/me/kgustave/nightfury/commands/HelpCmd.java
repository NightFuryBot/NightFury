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

import java.util.Objects;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import net.dv8tion.jda.core.entities.ChannelType;

/**
 *
 * @author Kaidan Gustave
 */
public class HelpCmd extends NightFuryCommand {

    public HelpCmd(NightFuryManager manager) {
        super(manager);
        this.name = "help";
        this.aliases = new String[]{"h","commands", "halp"};
        this.help = "gets a list of commands";
        this.guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        StringBuilder b = new StringBuilder();
        b.append("**Available commands in "+(event.isFromType(ChannelType.PRIVATE)?"DM":"<#"+event.getChannel().getId()+">")+"**\n");
        
        Command[] commands = event.getClient().getCommands().toArray(new Command[]{});
        String textPrefix = event.getClient().getTextualPrefix();
        String prefix = event.getClient().getPrefix();
        String ownerId = event.getClient().getOwnerId();
        String serverInvite = event.getClient().getServerInvite();
        Category category = null;
        for(Command command : commands) {
            if(!command.isOwnerCommand() || event.isOwner() || event.isCoOwner()) {
                if(!Objects.equals(category, command.getCategory())) {
                    category = command.getCategory();
                    if(category!=null && category.test(event))
                        b.append("\n\n**").append(category==null ? "No Category" : category.getName()).append("**\n");
                }
                if(category==null || (category!=null && category.test(event))) {
                    b.append("\n`").append(textPrefix).append(prefix==null?" ":"").append(command.getName())
                        .append(command.getArguments()==null ? "`" : " "+command.getArguments()+"`")
                        .append(" ").append(command.getHelp());
                }
            }
        }
        event.getJDA().retrieveUserById(ownerId).queue(owner -> {
            b.append("\n\nFor additional help, contact **").append(owner.getName()).append("**#").append(owner.getDiscriminator())
                .append(" or join his support server ").append(serverInvite);
        }, err -> {
            b.append("\n\nFor additional help, join my support server ").append(serverInvite);
        });
        event.reactSuccess();
        event.replyInDM(b.toString());

    }

}
