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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.LogUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author Kaidan Gustave
 */
public class CleanCmd extends NightFuryCommand {
    
    private static final String ARGS_REGEX = "(\\d+)((\\s+for)?([\\s|\\S]+))?";
    
    public CleanCmd(NightFuryManager manager) {
        super(manager);
        this.name = "clean";
        this.aliases = new String[]{"clear", "prune"};
        this.arguments = "[number of messages] <reason>";
        this.help = "deletes a given number of messages from the channel";
        this.guildOnly = true;
        this.category = manager.MODERATOR;
        this.botPermissions = new Permission[]{Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY};
        this.children = new NightFuryCommand[]{
                new BotsCmd(manager)
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs();
        if(args.matches(ARGS_REGEX))
        {
            int numberToDelete = Integer.parseInt(args.replaceAll(ARGS_REGEX, "$1"));
            String reason = args.replaceAll(ARGS_REGEX, "$4");
            if(numberToDelete<2) {
                event.replyError(FormatUtils.errorWithHelp("Too few messages specified", this.getName()));
                return;
            }
            if(numberToDelete>100) {
                event.replyError(FormatUtils.errorWithHelp("Too many messages specified", this.getName()));
                return;
            }
            TextChannel target = event.getTextChannel();
            OffsetDateTime maxTime = OffsetDateTime.now().minusWeeks(2L);
            target.getHistory().retrievePast(numberToDelete).queue(history -> {
                List<Message> toDelete = new ArrayList<>();
                history.stream()
                .filter(message -> message.getCreationTime().minusWeeks(2L).isBefore(maxTime))
                .forEach(message -> toDelete.add(message));
                int number = toDelete.size();
                Settings settings = manager.getSettingsForGuild(event.getGuild());
                if(numberToDelete<2) {
                    event.replyError(FormatUtils.errorWithHelp("Too few messages found", this.getName()));
                } else {
                    target.deleteMessages(toDelete).reason((reason.isEmpty()? "No reason specified" : reason)).queue(v -> {
                        event.replySuccess("Deleted "+number+" messages!");
                        if(settings.hasModLogId()) {
                            TextChannel log = event.getGuild().getTextChannelById(settings.getModLogId());
                            if(!event.getSelfMember().hasPermission(log, Permission.MESSAGE_WRITE))
                                return;
                            else if(!reason.isEmpty())
                                LogUtils.cleanWithReason(settings, log, event.getAuthor(), target, number, reason);
                            else
                                LogUtils.cleanWithNoReason(settings, log, event.getAuthor(), target, number);
                        }
                    });
                }
            });
        } else {
            event.replyError(FormatUtils.invalidArgs(this.getName()));
        }
    }
    
    private class BotsCmd extends NightFuryCommand {

        public BotsCmd(NightFuryManager manager) {
            super(manager);
            this.name = "bots";
            this.arguments = "[number of messages]";
            this.help = "gets a number of messages from the channel and deletes any from bots";
            this.guildOnly = true;
            this.category = manager.MODERATOR;
            this.botPermissions = new Permission[]{Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY};
        }

        @Override
        protected void execute(CommandEvent event) {
            String args = event.getArgs();
            if(args.matches(ARGS_REGEX))
            {
                int numberToDelete = Integer.parseInt(args.replaceAll(ARGS_REGEX, "$1"));
                String reason = args.replaceAll(ARGS_REGEX, "$4");
                if(numberToDelete<2) {
                    event.replyError(FormatUtils.errorWithHelp("Too few messages specified", this.getName()));
                    return;
                }
                if(numberToDelete>100) {
                    event.replyError(FormatUtils.errorWithHelp("Too many messages specified", this.getName()));
                    return;
                }
                TextChannel target = event.getTextChannel();
                OffsetDateTime maxTime = OffsetDateTime.now().minusWeeks(2L);
                target.getHistory().retrievePast(numberToDelete).queue(history -> {
                    List<Message> toDelete = new ArrayList<>();
                    history.stream()
                    .filter(message -> message.getAuthor().isBot())
                    .filter(message -> message.getCreationTime().minusWeeks(2L).isBefore(maxTime))
                    .forEach(message -> toDelete.add(message));
                    int number = toDelete.size();
                    Settings settings = manager.getSettingsForGuild(event.getGuild());
                    if(numberToDelete<2) {
                        event.replyError(FormatUtils.errorWithHelp("Too few messages found", this.getName()));
                    } else {
                        target.deleteMessages(toDelete).reason((reason.isEmpty()? "No reason specified" : reason)).queue(v -> {
                            event.replySuccess("Deleted "+number+" messages!");
                            if(settings.hasModLogId()) {
                                TextChannel log = event.getGuild().getTextChannelById(settings.getModLogId());
                                if(!event.getSelfMember().hasPermission(log, Permission.MESSAGE_WRITE))
                                    return;
                                else if(!reason.isEmpty())
                                    LogUtils.cleanWithReason(settings, log, event.getAuthor(), target, number, reason);
                                else
                                    LogUtils.cleanWithNoReason(settings, log, event.getAuthor(), target, number);
                            }
                        });
                    }
                });
            } else {
                event.replyError(FormatUtils.invalidArgs(this.getName()));
            }
        }
        
    }

}
