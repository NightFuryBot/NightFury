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
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author Kaidan Gustave
 */
public class BanCmd extends NightFuryCommand {
    
    public BanCmd(NightFuryManager manager) {
        super(manager);
        this.name = "ban";
        this.arguments = "[@user or ID] <reason>";
        this.help = "bans a user from the server";
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.category = manager.MODERATOR;
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(event.getArgs().matches(FormatUtils.TARGET_ID_REASON_REGEX)) {
            String id = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_REGEX, "$2").trim();
            String reason = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_REGEX, "$5").trim();
            event.getJDA().retrieveUserById(id).queue(target -> {
                executeBan(event,settings,target,reason);
            }, err -> {
                event.replyError("User could not be found!");
            });
        } else if(event.getArgs().matches(FormatUtils.TARGET_ID_REASON_FAIL_REGEX)) {
            String name = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$2").trim();
            String descriminator = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$3").trim();
            String reason = event.getArgs().replaceAll(FormatUtils.TARGET_ID_REASON_FAIL_REGEX, "$6").trim();
            List<Member> members = event.getGuild().getMembersByName(name, false)
                    .stream().filter(member -> member.getUser().getDiscriminator().equals(descriminator)).collect(Collectors.toList());
            if(members.size()<1)
                event.replyError("No users detected going by **"+name+"**#"+descriminator+"!");
            else if(members.size()>1)
                event.replyError("Multiple users detected going by **"+name+"**#"+descriminator+"!");
            else
                executeBan(event,settings,members.get(0).getUser(),reason);
        } else {
            event.replyError(FormatUtils.invalidArgs(this.getName()));
        }
    }
    
    private void executeBan(CommandEvent event, Settings settings, User target, String reason) {
        String user = FormatUtils.formattedUserName(target, true);
        if(event.getSelfUser().equals(target))
            event.replyError("I cannot ban myself from the server!");
        else if(event.getAuthor().equals(target))
            event.replyError("I cannot ban you from the server!");
        else if(event.getGuild().getMember(target)!=null && !event.getSelfMember().canInteract(event.getGuild().getMember(target)))
            event.replyError("I cannot ban "+user+" because they have a higher role than me!");
        else {
            event.getGuild().getController().ban(target, 1).reason((reason.isEmpty()? "No reason specified" : reason)).queue(v -> {
                event.replySuccess(user+" was banned from the server.");
                if(settings.hasModLogId()) {
                    TextChannel log = event.getGuild().getTextChannelById(settings.getModLogId());
                    if(!event.getSelfMember().hasPermission(log, Permission.MESSAGE_WRITE))
                        return;
                    else if(!reason.isEmpty())
                        LogUtils.userWithReason(settings, log, event.getAuthor(), target, LogAction.BAN, reason);
                    else
                        LogUtils.userWithNoReason(settings, log, event.getAuthor(), target, LogAction.BAN);
                }
            }, err -> {
                event.replyError("An unexpected error occurred while banning "+user+"!");
            });
        }
    }
}
