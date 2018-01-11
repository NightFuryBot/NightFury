/*
 * Copyright 2017-2018 Kaidan Gustave
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
package xyz.nightfury.listeners

import xyz.nightfury.extensions.muteRole
import xyz.nightfury.extensions.refreshMutedRole
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.nightfury.db.*

/**
 * @author Kaidan Gustave
 */
class DatabaseListener : EventListener {
    override fun onEvent(event: Event?) {
        when(event) {
            is ReadyEvent              -> onReady(event)
            is RoleDeleteEvent         -> onRoleDelete(event)
            is TextChannelCreateEvent  -> onTextChannelCreate(event)
            is TextChannelDeleteEvent  -> onTextChannelDelete(event)
            is VoiceChannelCreateEvent -> onVoiceChannelCreate(event)
            is CategoryCreateEvent     -> onCategoryCreate(event)
            is GuildMemberJoinEvent    -> onGuildMemberJoin(event)
            is GuildMemberLeaveEvent   -> onGuildMemberLeave(event)
        }
    }

    fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach {
            it.refreshMutedRole(SQLMutedRole.getRole(it) ?: return@forEach)
        }
    }

    fun onRoleDelete(event: RoleDeleteEvent) {
        // RoleMe Deleted
        if(SQLRoleMe.isRole(event.role))
            SQLRoleMe.deleteRole(event.role)

        // ColorMe Deleted
        if(SQLColorMe.isRole(event.role))
            SQLColorMe.deleteRole(event.role)

        // Mod Role Deleted
        val modRole = SQLModeratorRole.getRole(event.guild)
        if(modRole != null) {
            if(modRole == event.role)
                SQLModeratorRole.deleteRole(event.guild)
        } else {
            if(SQLModeratorRole.hasRole(event.guild))
                SQLModeratorRole.deleteRole(event.guild)
        }

        // Muted Role Deleted
        val mutedRole = SQLMutedRole.getRole(event.guild)
        if(mutedRole != null) {
            if(mutedRole == event.role)
                SQLMutedRole.deleteRole(event.guild)
        } else {
            if(SQLMutedRole.hasRole(event.guild))
                SQLMutedRole.deleteRole(event.guild)
        }

        // Announcement role
        if(SQLAnnouncementRoles.isRole(event.role))
            SQLAnnouncementRoles.deleteRole(event.role)
    }

    fun onTextChannelCreate(event: TextChannelCreateEvent) {
        event.channel.muteRole(SQLMutedRole.getRole(event.guild) ?: return)
    }

    fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        event.channel.muteRole(SQLMutedRole.getRole(event.guild) ?: return)
    }

    // If the guild has a type of channel and it equals the deleted channel, then it's removed
    // if the type of channel is null, but the database contains info regarding that type, it
    // is also removed
    fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        // ModLog Deleted
        val modLog = SQLModeratorLog.getChannel(event.guild)
        if(modLog != null) {
            if(event.channel == modLog)
                SQLModeratorLog.deleteChannel(event.guild)
        } else {
            if(SQLModeratorLog.hasChannel(event.guild))
                SQLModeratorLog.deleteChannel(event.guild)
        }

        // Ignored Channel Deleted
        if(SQLIgnoredChannels.isChannel(event.channel))
            SQLIgnoredChannels.deleteChannel(event.channel)

        // Welcome Channel Deleted
        val welcomeChan = SQLWelcomes.getChannel(event.guild)
        if(welcomeChan != null) {
            if(event.channel == welcomeChan)
                SQLWelcomes.removeWelcome(event.guild)
        } else {
            if(SQLWelcomes.hasWelcome(event.guild))
                SQLWelcomes.removeWelcome(event.guild)
        }

        val announceChan = SQLAnnouncementChannel.getChannel(event.guild)
        if(announceChan != null) {
            if(event.channel == announceChan)
                SQLAnnouncementChannel.deleteChannel(event.guild)
        } else {
            if(SQLAnnouncementChannel.hasChannel(event.guild))
                SQLAnnouncementChannel.deleteChannel(event.guild)
        }

        val starboard = SQLStarboardSettings.getChannel(event.guild)
        if(starboard != null) {
            if(starboard == event.channel)
                SQLStarboardSettings.deleteSettingsFor(event.guild)
        } else {
            if(SQLStarboardSettings.hasChannel(event.guild))
                SQLStarboardSettings.deleteSettingsFor(event.guild)
        }
    }

    fun onCategoryCreate(event: CategoryCreateEvent) {
        event.category.muteRole(SQLMutedRole.getRole(event.guild) ?: return)
    }

    fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if(SQLRolePersist.isRolePersist(event.guild))
            SQLRolePersist.setRolePersist(event.member)
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if(SQLRolePersist.isRolePersist(event.guild)) {
            val roles = SQLRolePersist.getRolePersist(event.member)
            if(roles.isNotEmpty()) {
                event.guild.controller.addRolesToMember(event.member, roles).queue()
                SQLRolePersist.removeRolePersist(event.member)
            }
        }
    }
}
