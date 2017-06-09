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

import me.kgustave.nightfury.manager.LogAction;
import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.LogUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author Kaidan Gustave
 */
public class MuteCmd extends NightFuryCommand {
    
    public MuteCmd(NightFuryManager manager) {
        super(manager);
        this.name = "mute";
        this.arguments = "[@user or ID] <reason>";
        this.help = "mutes a user";
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.category = manager.MODERATOR;
    }

    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs();
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(!settings.hasMutedId()) {
            event.replyError(FormatUtils.errorWithHelp("Muted role has not been setup", this.getName()));
        } else if(args.matches(FormatUtils.TARGET_ID_REASON_REGEX)) {
            String id = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_REGEX, "$2").trim();
            String reason = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_REGEX, "$5").trim();
            Member target = event.getGuild().getMemberById(id);
            Role role = event.getGuild().getRoleById(settings.getMutedId());
            if(target==null) {
                event.replyError("User could not be found on this server!");
            } else {
                executeMute(event,settings,target,role,reason);
            }
        } else if(args.matches(FormatUtils.TARGET_ID_REASON_FAIL_REGEX)) {
            String name = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$2").trim();
            String descriminator = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$3").trim();
            String reason = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$6").trim();
            List<Member> members = event.getGuild().getMembersByName(name, false)
                    .stream().filter(member -> member.getUser().getDiscriminator().equals(descriminator)).collect(Collectors.toList());
            if(members.size()<1)
                event.replyError("No users detected going by **"+name+"**#"+descriminator+"!");
            else if(members.size()>1)
                event.replyError("Multiple users detected going by **"+name+"**#"+descriminator+"!");
            else {
                Member target = members.get(0);
                Role role = event.getGuild().getRoleById(settings.getMutedId());
                executeMute(event,settings,target,role,reason);
            }
        } else {
            event.replyError(FormatUtils.invalidArgs(this.getName()));
        }
    }
    
    private void executeMute(CommandEvent event, Settings settings, Member target, Role role, String reason) {
        String user = FormatUtils.formattedUserName(target.getUser(), true);
        if(!event.getSelfMember().canInteract(target)) {
            event.replyError("I cannot mute "+user+" because they have a higher role than me!");
        } else if(!event.getSelfMember().canInteract(role)) {
            event.replyError("I cannot mute "+user+" because the muted role is a higher role than me!");
        } else if(target.getRoles().contains(role)) {
            event.replyError("I cannot mute "+user+" because they are already muted!");
        } else {
            event.getGuild().getController().addRolesToMember(target, role)
            .reason(reason==null? "No reason specified" : reason).queue(v -> {
                event.replySuccess(user+" was muted.");
                if(settings.hasModLogId()) {
                    TextChannel log = event.getGuild().getTextChannelById(settings.getModLogId());
                    if(!event.getSelfMember().hasPermission(log, Permission.MESSAGE_WRITE))
                        return;
                    else if(!reason.isEmpty())
                        LogUtils.userWithReason(settings, log, event.getAuthor(), target.getUser(), LogAction.MUTE, reason);
                    else
                        LogUtils.userWithNoReason(settings, log, event.getAuthor(), target.getUser(), LogAction.MUTE);
                }
            });
        }
    }
}
