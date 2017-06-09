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
package me.kgustave.nightfury.listeners;

import me.kgustave.nightfury.manager.LogAction;
import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.LogUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 *
 * @author Kaidan Gustave
 */
public class LogsListener extends ListenerAdapter {
    
    private final NightFuryManager manager;
    
    public LogsListener(NightFuryManager manager) {
        this.manager = manager;
    }
    
    // Bans
    @Override
    public void onGuildBan(GuildBanEvent event) {
        if(event.getUser().equals(event.getJDA().getSelfUser()))
            return;
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasModLogId() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            event.getGuild().getAuditLogs().limit(10).queue(audits -> {
                audits.stream().findAny()
                .filter(audit -> audit.getType().equals(ActionType.BAN))
                .filter(audit -> event.getUser().getIdLong()==audit.getTargetIdLong())
                .ifPresent(audit -> audit(settings, event.getGuild().getTextChannelById(settings.getModLogId()), audit, LogAction.BAN));
            });
        }
    }
    
    // Unbans
    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        if(event.getUser().equals(event.getJDA().getSelfUser()))
            return;
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasModLogId() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            event.getGuild().getAuditLogs().limit(10).queue(audits -> {
                audits.stream().findAny()
                .filter(audit -> audit.getType().equals(ActionType.UNBAN))
                .filter(audit -> event.getUser().getIdLong()==audit.getTargetIdLong())
                .ifPresent(audit -> audit(settings, event.getGuild().getTextChannelById(settings.getModLogId()), audit, LogAction.UNBAN));
            });
        }
    }
    
    // Kicks
    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if(event.getMember().getUser().equals(event.getJDA().getSelfUser()))
            return;
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasModLogId() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            event.getGuild().getAuditLogs().limit(10).queue(audits -> {
                audits.stream().findAny()
                .filter(audit -> audit.getType().equals(ActionType.KICK))
                .filter(audit -> event.getMember().getUser().getIdLong()==audit.getTargetIdLong())
                .ifPresent(audit -> audit(settings, event.getGuild().getTextChannelById(settings.getModLogId()), audit, LogAction.KICK));
            });
        }
    }
    
    // Mutes
    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if(event.getMember().getUser().equals(event.getJDA().getSelfUser()))
            return;
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasMutedId() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            if(event.getRoles().contains(event.getGuild().getRoleById(settings.getMutedId()))) {
                event.getGuild().getAuditLogs().limit(10).queue(audits -> {
                    audits.stream().findAny()
                    .filter(audit -> audit.getType().equals(ActionType.MEMBER_ROLE_UPDATE))
                    .filter(audit -> event.getMember().getUser().getIdLong()==audit.getTargetIdLong())
                    .ifPresent(audit -> audit(settings, event.getGuild().getTextChannelById(settings.getModLogId()), audit, LogAction.MUTE));
                });
            }
        }
    }
    
    // Unmutes
    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if(event.getMember().getUser().equals(event.getJDA().getSelfUser()))
            return;
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasMutedId() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            if(event.getRoles().contains(event.getGuild().getRoleById(settings.getMutedId()))) {
                event.getGuild().getAuditLogs().limit(10).queue(audits -> {
                    audits.stream().findAny()
                    .filter(audit -> audit.getType().equals(ActionType.MEMBER_ROLE_UPDATE))
                    .filter(audit -> event.getMember().getUser().getIdLong()==audit.getTargetIdLong())
                    .ifPresent(audit -> audit(settings, event.getGuild().getTextChannelById(settings.getModLogId()), audit, LogAction.UNMUTE));
                });
            }
        }
    }
    
    // Remutes
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if(event.getMember().getUser().equals(event.getJDA().getSelfUser()))
            return;
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasMutedId() && event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            if(settings.isMuted(event.getMember().getUser().getIdLong())) {
                event.getGuild().getController().addRolesToMember(event.getMember(), event.getGuild().getRoleById(settings.getMutedId())).queue();
            }
        }
    }
    
    private void audit(Settings settings, TextChannel log, AuditLogEntry audit, LogAction action) {
        if(audit.getUser().equals(log.getJDA().getSelfUser()))
            return;
        LogUtils.audit(settings, log, audit, action);
    }
    
}
