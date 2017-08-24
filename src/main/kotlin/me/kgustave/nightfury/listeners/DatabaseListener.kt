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
package me.kgustave.nightfury.listeners

import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.extensions.muteRole
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
class DatabaseListener(private val manager: DatabaseManager, private val executor: ScheduledExecutorService) : EventListener
{
    private val leaving = HashMap<Long, ScheduledFuture<*>>()

    override fun onEvent(event: Event?) = when(event)
    {
        is RoleDeleteEvent -> onRoleDelete(event)
        is TextChannelCreateEvent -> onTextChannelCreate(event)
        is TextChannelDeleteEvent -> onTextChannelDelete(event)
        is VoiceChannelCreateEvent -> onVoiceChannelCreate(event)
        is GuildJoinEvent -> onGuildJoin(event)
        is GuildLeaveEvent -> onGuildLeave(event)

        else -> Unit
    }

    fun onRoleDelete(event: RoleDeleteEvent)
    {
        // RoleMe Deleted
        if(manager.isRoleMe(event.role))
            manager.removeRoleMe(event.role)

        // ColorMe Deleted
        if(manager.isColorMe(event.role))
            manager.removeColorMe(event.role)

        // Mod Role Deleted
        val modRole = manager.getModRole(event.guild)
        if(modRole!=null) {
            if(modRole==event.role)
                manager.resetModRole(event.guild)
        } else {
            if(manager.hasModRole(event.guild))
                manager.resetModRole(event.guild)
        }

        // Muted Role Deleted
        val mutedRole = manager.getMutedRole(event.guild)
        if(mutedRole!=null) {
            if(mutedRole==event.role)
                manager.resetMutedRole(event.guild)
        } else {
            if(manager.hasMutedRole(event.guild))
                manager.resetMutedRole(event.guild)
        }
    }

    fun onTextChannelCreate(event: TextChannelCreateEvent)
    {
        val muted = manager.getMutedRole(event.guild)
        if(muted!=null)
            event.channel muteRole muted
    }

    fun onTextChannelDelete(event: TextChannelDeleteEvent)
    {
        // ModLog Deleted
        val modLog = manager.getModLog(event.guild)
        if(modLog != null) {
            if(event.channel == modLog)
                manager.resetModLog(event.guild)
        } else {
            if(manager.hasModLog(event.guild))
                manager.resetModLog(event.guild)
        }

        // Ignored Channel Deleted
        if(manager.isIgnoredChannel(event.channel))
            manager.removeIgnoredChannel(event.channel)

        // Welcome Channel Deleted
        val welcomeChan = manager.getWelcomeChannel(event.guild)
        if(welcomeChan != null) {
            if(event.channel == welcomeChan) {
                manager.resetWelcome(event.guild)
            }
        } else {
            if(manager.hasWelcome(event.guild)) {
                manager.resetWelcome(event.guild)
            }
        }
    }

    fun onVoiceChannelCreate(event: VoiceChannelCreateEvent)
    {
        val muted = manager.getMutedRole(event.guild)
        if(muted!=null)
            event.channel muteRole muted
    }

    fun onGuildJoin(event: GuildJoinEvent)
    {
        synchronized(leaving)
        {
            if(leaving.contains(event.guild.idLong))
                leaving.remove(event.guild.idLong)?.cancel(false)
        }
    }

    fun onGuildLeave(event: GuildLeaveEvent)
    {
        // Soft 5 Minute
        synchronized(leaving) {
            leaving.put(event.guild.idLong, executor.schedule({
                manager.leaveGuild(event.guild)
            }, 5, TimeUnit.MINUTES))
        }
    }
}