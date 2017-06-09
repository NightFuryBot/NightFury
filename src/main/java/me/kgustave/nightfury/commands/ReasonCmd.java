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

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.LogUtils;

/**
 *
 * @author Kaidan Gustave
 */
public class ReasonCmd extends NightFuryCommand {
    
    // $1 - first digit
    // $3 - second digit
    // $4 - reason
    private static final String ARGS_REGEX = "(\\d+)?(\\s*-\\s*(\\d+))?([\\s|\\S]+)";
    
    public ReasonCmd(NightFuryManager manager) {
        super(manager);
        this.name = "reason";
        this.aliases = new String[]{"r"};
        this.arguments = "<case number> [reason]";
        this.help = "logs a reason to the servers mod-log";
        this.category = manager.MODERATOR;
        this.guildOnly = true;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs();
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(!settings.hasModLogId()) {
            event.replyError(FormatUtils.errorWithHelp("Mod Log has not been set up", this.getName()));
        } else if(args.isEmpty()) {
            event.replyError(FormatUtils.tooFewArgs(this.getName()));
        } else if(args.matches(ARGS_REGEX)) {
            String firstCase = args.replaceAll(ARGS_REGEX, "$1").trim();
            String secondCase = args.replaceAll(ARGS_REGEX, "$3").trim();
            String reason = args.replaceAll(ARGS_REGEX, "$4").trim();
            if(reason.isEmpty()) {
                event.replyError(FormatUtils.tooFewArgs(this.getName()));
            } else if(reason.length()>200) {
                event.replyError("Reasons for cases can only be 200 characters or less!");
            } else {
                if(firstCase.isEmpty()) {
                    executeReason(event, settings, reason);
                } else if(!firstCase.isEmpty() && secondCase.isEmpty()) {
                    try {
                        int caseNumber = Integer.parseInt(firstCase);
                        executeReason(event, settings, caseNumber, reason);
                    } catch(NumberFormatException e) {
                        event.replyError(FormatUtils.errorWithHelp("The command provided could not be parsed for an unknown reason!", this.getName()));
                    }
                } else if(!secondCase.isEmpty()) {
                    try {
                        int begin = Integer.parseInt(firstCase);
                        int end = Integer.parseInt(secondCase);
                        executeReason(event, settings, begin, end, reason);
                    } catch(NumberFormatException e) {
                        event.replyError(FormatUtils.errorWithHelp("The command provided could not be parsed for an unknown reason!", this.getName()));
                    }
                }
            }
        } else {
            event.replyError(FormatUtils.invalidArgs(this.getName()));
        }
    }
    
    private void executeReason(CommandEvent event, Settings settings, String reason) {
        int caseNumber = settings.getNewestCaseForId(event.getAuthor().getIdLong());
        if(caseNumber==0) {
            event.replyError("You have no cases to give reasons for!");
        } else if(settings.getNextCaseNumber()>caseNumber) {
            if(event.getAuthor().getIdLong()==settings.getModIdForCase(caseNumber)) {
                long messageId = settings.getMessageIdForCase(caseNumber);
                event.getGuild().getTextChannelById(settings.getModLogId()).getMessageById(messageId).queue(m -> {
                    LogUtils.updateReason(settings, m, caseNumber, reason);
                    event.reactSuccess();
                });
            } else
                event.replyError(FormatUtils.errorWithHelp("You provided a case number you are not responsible for!", this.getName()));
        } else
            event.replyError(FormatUtils.errorWithHelp("Invalid case number!", this.getName()));
    }
    
    //Case Number Specified
    private void executeReason(CommandEvent event, Settings settings, int caseNumber, String reason) {
        if(caseNumber>0 && settings.getNextCaseNumber()>caseNumber) {
            if(event.getAuthor().getIdLong()==settings.getModIdForCase(caseNumber)) {
                long messageId = settings.getMessageIdForCase(caseNumber);
                event.getGuild().getTextChannelById(settings.getModLogId()).getMessageById(messageId).queue(m -> {
                    LogUtils.updateReason(settings, m, caseNumber, reason);
                    event.reactSuccess();
                });
            } else
                event.replyError(FormatUtils.errorWithHelp("You provided a case number you are not responsible for!", this.getName()));
        } else
            event.replyError(FormatUtils.errorWithHelp("Invalid case number!", this.getName()));
    }
    
    //Case Range Specified
    private void executeReason(CommandEvent event, Settings settings, int begin, int end, String reason) {
        if(end>1 && begin>0 && end>begin || 5<(1+end)-(begin-1)) {
            if(settings.getNextCaseNumber()>end) {
                for(int i=begin; i<=end; i++) {
                    int caseNumber = i;
                    if(event.getAuthor().getIdLong()==settings.getModIdForCase(caseNumber)) {
                        long messageId = settings.getMessageIdForCase(caseNumber);
                        event.getGuild().getTextChannelById(settings.getModLogId()).getMessageById(messageId).queue(m -> {
                            LogUtils.updateReason(settings, m, caseNumber, reason);
                            event.reactSuccess();
                        });
                    }
                }
            } else
                event.replyError(FormatUtils.errorWithHelp("Invalid case number", this.getName()));
        } else
            event.replyError(FormatUtils.errorWithHelp("Invalid case range", this.getName()));
    }
}
