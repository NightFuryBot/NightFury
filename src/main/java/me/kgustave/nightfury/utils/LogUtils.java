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
package me.kgustave.nightfury.utils;

import me.kgustave.nightfury.manager.LogAction;
import me.kgustave.nightfury.manager.Settings;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author Kaidan Gustave
 */
public class LogUtils {
    
    public static void userWithReason(Settings settings, TextChannel log, User mod, User target, LogAction action, String reason)
    {
        log.sendMessageFormat(
                
                FormatUtils.CASE_FORMAT,// Format
                settings.getNextCaseNumber(), // Case Number
                action.emote, // Action Emote
                FormatUtils.formattedUserName(mod, true), // Formatted Username of Mod
                action.act, // Action
                target.getName(), // Target Name
                target.getIdLong(), // Target ID
                (reason==null || reason.isEmpty()? mod.getAsMention()+" type `"+settings.getPrefixes().getString(0)+"reason`" : reason) // Reason
                
                ).queue(m -> settings.addCase(mod.getIdLong(), m.getIdLong(), reason));
    }
    
    public static void userWithNoReason(Settings settings, TextChannel log, User mod, User target, LogAction action)
    {
        userWithReason(settings, log, mod, target, action, null);
    }

    public static void channelWithReason(Settings settings, TextChannel log, User mod, TextChannel target, LogAction action, String reason)
    {
        log.sendMessageFormat(
                
                FormatUtils.CASE_FORMAT, // Format
                settings.getNextCaseNumber(), // Case Number
                action.emote, // Action Emote
                FormatUtils.formattedUserName(mod, true), // Formatted Username of Mod
                action.act, // Action
                target.getAsMention(), // TextChannel Mention
                target.getIdLong(), // TextChannel ID
                (reason==null || reason.isEmpty()? mod.getAsMention()+" type `"+settings.getPrefixes().getString(0)+"reason`" : reason) // Reason
                
                ).queue(m -> settings.addCase(mod.getIdLong(), m.getIdLong(), reason));
    }
    
    public static void channelWithNoReason(Settings settings, TextChannel log, User mod, TextChannel target, LogAction action)
    {
        channelWithReason(settings, log, mod, target, action, null);
    }
    
    public static void cleanWithReason(Settings settings, TextChannel log, User mod, TextChannel target, int numberDeleted, String reason)
    {
        log.sendMessageFormat(
                
                FormatUtils.CASE_FORMAT, // Format
                settings.getNextCaseNumber(), // Case Number
                LogAction.CLEAN.emote, // Action Emote
                FormatUtils.formattedUserName(mod, true), // Formatted Username of Mod
                String.format(LogAction.CLEAN.act, numberDeleted), // Action
                target.getAsMention(), // TextChannel Mention
                target.getIdLong(), // TextChannel ID
                (reason==null || reason.isEmpty()? mod.getAsMention()+" type `"+settings.getPrefixes().getString(0)+"reason`" : reason) // Reason
                
                ).queue(m -> settings.addCase(mod.getIdLong(), m.getIdLong(), reason));
    }
    
    public static void cleanWithNoReason(Settings settings, TextChannel log, User mod, TextChannel target, int numberDeleted)
    {
        cleanWithReason(settings, log, mod, target, numberDeleted, null);
    }
    
    public static void audit(Settings settings, TextChannel log, AuditLogEntry audit, LogAction action)
    {
        User mod = audit.getUser();
        audit.getJDA().retrieveUserById(audit.getTargetIdLong()).queue(target -> {
            userWithReason(settings, log, mod, target, action, audit.getReason());
        });
    }
    
    public static void updateReason(Settings settings, Message message, int caseNumber, String reason)
    {
        if(!message.getAuthor().equals(message.getJDA().getSelfUser()))
            throw new IllegalArgumentException("Error! Attempted to edit log for message that was not sent by this bot!");
        String top = message.getContent().split("\n")[0];
        message.editMessage(top+"\n`[ REASON ]` "+reason).queue(r -> {
            settings.addReasonToCase(reason, caseNumber);
        });
    }
}
