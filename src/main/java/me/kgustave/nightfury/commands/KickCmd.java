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

import java.util.List;
import java.util.stream.Collectors;

import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.utils.SafeIdUtil;

import me.kgustave.nightfury.manager.LogAction;
import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.LogUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author Kaidan Gustave
 */
public class KickCmd extends NightFuryCommand {
    
    public KickCmd(NightFuryManager manager) {
        super(manager);
        this.name = "kick";
        this.arguments = "[@user or ID] <reason>";
        this.help = "kicks a user from the server";
        this.botPermissions = new Permission[]{Permission.KICK_MEMBERS};
        this.category = manager.MODERATOR;
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(event.getArgs().matches(FormatUtils.TARGET_ID_REASON_REGEX)) {
            String id = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_REGEX, "$2").trim();
            String reason = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_REGEX, "$5").trim();
            Member target = event.getGuild().getMemberById(SafeIdUtil.safeConvert(id));
            if(target==null)
                event.replyError("User could not be found on this server!");
            else
                executeKick(event,settings,target,reason);
        } else if(event.getArgs().matches(FormatUtils.TARGET_ID_REASON_FAIL_REGEX)) {
            String name = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$2");
            String descriminator = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$3");
            String reason = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$6").trim();
            List<Member> members = event.getGuild().getMembersByName(name, false)
                    .stream().filter(member -> member.getUser().getDiscriminator().equals(descriminator)).collect(Collectors.toList());
            if(members.size()<1)
                event.replyError("No users detected going by **"+name+"**#"+descriminator+"!");
            else if(members.size()>1)
                event.replyError("Multiple users detected going by **"+name+"**#"+descriminator+"!");
            else
                executeKick(event,settings,members.get(0), reason);
        } else {
            event.replyError(FormatUtils.invalidArgs(this.getName()));
        }
    }
    
    private void executeKick(CommandEvent event, Settings settings, Member target, String reason) {
        String user = FormatUtils.formattedUserName(target.getUser(), true);
        if(event.getSelfMember().equals(target))
            event.replyError("I cannot kick myself from the server!");
        else if(event.getAuthor().equals(target))
            event.replyError("I cannot kick you from the server!");
        else if(!event.getSelfMember().canInteract(target))
            event.replyError("I cannot kick "+user+" because they have a higher role than me!");
        else {
            event.getGuild().getController().kick(target).reason((reason.isEmpty()? "No reason specified" : reason)).queue(v -> {
                event.replySuccess(user+" was kicked from the server.");
                if(settings.hasModLogId()) {
                    TextChannel log = event.getGuild().getTextChannelById(settings.getModLogId());
                    if(!event.getSelfMember().hasPermission(log, Permission.MESSAGE_WRITE))
                        return;
                    else if(!reason.isEmpty())
                        LogUtils.userWithReason(settings, log, event.getAuthor(), target.getUser(), LogAction.KICK, reason);
                    else
                        LogUtils.userWithNoReason(settings, log, event.getAuthor(), target.getUser(), LogAction.KICK);
                }
            }, err -> {
                event.replyError("An unexpected error occurred while kicking "+user+"!");
            });
        }
    }
    
}
