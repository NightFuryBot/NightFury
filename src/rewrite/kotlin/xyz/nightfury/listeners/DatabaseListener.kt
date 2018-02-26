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

import xyz.nightfury.util.ext.muteRole
import xyz.nightfury.util.ext.refreshMutedRole
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.nightfury.util.db.*

/**
 * @author Kaidan Gustave
 */
class DatabaseListener : EventListener {
    override fun onEvent(event: Event) {
        when(event) {
            is ReadyEvent              -> onReady(event)
            is RoleDeleteEvent         -> onRoleDelete(event)
            is TextChannelCreateEvent  -> onTextChannelCreate(event)
            is TextChannelDeleteEvent  -> onTextChannelDelete(event)
            is VoiceChannelCreateEvent -> onVoiceChannelCreate(event)
            is CategoryCreateEvent     -> onCategoryCreate(event)
        }
    }

    private fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach { guild ->
            guild.mutedRole?.let { guild.refreshMutedRole(it) }
        }
    }

    private fun onRoleDelete(event: RoleDeleteEvent) {
        val role = event.role
        val guild = event.guild

        // RoleMe Deleted
        if(role.isRoleMe) {
            role.isRoleMe = false
        }

        // ColorMe Deleted
        if(role.isColorMe) {
            role.isColorMe = false
        }

        // Announcement role
        if(role.isAnnouncements) {
            role.isAnnouncements = false
        }

        // Mod Role Deleted
        val modRole = guild.modRole
        if(modRole !== null) {
            if(modRole == role) {
                guild.modRole = null
            }
        } else {
            if(guild.hasModRole) {
                guild.modRole = null
            }
        }

        // Muted Role Deleted
        val mutedRole = guild.mutedRole
        if(mutedRole !== null) {
            if(mutedRole == role) {
                guild.mutedRole = null
            }
        } else {
            if(guild.hasMutedRole) {
                guild.mutedRole = null
            }
        }
    }

    private fun onTextChannelCreate(event: TextChannelCreateEvent) {
        event.guild.mutedRole?.let { event.channel.muteRole(it) }
    }

    private fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        event.guild.mutedRole?.let { event.channel.muteRole(it) }
    }

    private fun onCategoryCreate(event: CategoryCreateEvent) {
        event.guild.mutedRole?.let { event.category.muteRole(it) }
    }

    // If the guild has a type of channel and it equals the deleted channel, then it's removed
    // if the type of channel is null, but the database contains info regarding that type, it
    // is also removed
    private fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        val channel = event.channel
        val guild = event.guild

        // Ignored Channel Deleted
        if(channel.isIgnored) {
            channel.isIgnored = false
        }

        // ModLog Deleted
        val modLog = guild.modLog
        if(modLog !== null) {
            if(modLog == channel) {
                guild.modLog = null
            }
        } else {
            if(guild.hasModLog) {
                guild.modLog = null
            }
        }

        // Welcome Channel Deleted
        val welcomeChan = guild.welcomeChannel
        if(welcomeChan !== null) {
            if(welcomeChan == channel) {
                guild.removeWelcome()
            }
        } else {
            if(guild.hasWelcome) {
                guild.removeWelcome()
            }
        }

        // Announcements Channel Deleted
        val announceChan = guild.announcementChannel
        if(announceChan !== null) {
            if(announceChan == channel) {
                guild.announcementChannel = null
            }
        } else {
            if(guild.hasAnnouncementsChannel) {
                guild.announcementChannel = null
            }
        }

        val starboard = guild.starboardSettings
        if(starboard !== null) {
            if(starboard.channelId == channel.idLong) {
                guild.removeStarboard()
            }
        } else {
            if(guild.hasStarboard) {
                guild.removeStarboard()
            }
        }
    }
}
