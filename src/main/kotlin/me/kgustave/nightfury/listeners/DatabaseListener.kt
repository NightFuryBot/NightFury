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
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


/**
 * @author Kaidan Gustave
 */
class DatabaseListener(val manager: DatabaseManager, val executor: ScheduledExecutorService) : ListenerAdapter()
{
    private val leaving = HashMap<Long, ScheduledFuture<*>>()

    override fun onRoleDelete(event: RoleDeleteEvent)
    {
        if(manager.isRoleMe(event.role))                  manager.removeRoleMe(event.role)
        if(manager.isColorMe(event.role))                 manager.removeColorMe(event.role)
        if(manager.getModRole(event.guild)==event.role)   manager.resetModRole(event.guild)
        if(manager.getMutedRole(event.guild)==event.role) manager.resetMutedRole(event.guild)
    }

    override fun onTextChannelCreate(event: TextChannelCreateEvent)
    {
        val muted = manager.getMutedRole(event.guild)
        if(muted!=null)
            event.channel.muteRole(muted)
    }

    override fun onTextChannelDelete(event: TextChannelDeleteEvent)
    {
        val modLog = manager.getModLog(event.guild)
        if(modLog != null && event.channel == modLog)
            manager.resetModLog(event.guild)
        if(manager.isIgnoredChannel(event.channel))
            manager.removeIgnoredChannel(event.channel)
    }

    override fun onVoiceChannelCreate(event: VoiceChannelCreateEvent)
    {
        val muted = manager.getMutedRole(event.guild)
        if(muted!=null)
            event.channel.muteRole(muted)
    }

    override fun onGuildJoin(event: GuildJoinEvent)
    {
        synchronized(leaving)
        {
            if(leaving.contains(event.guild.idLong))
                leaving.remove(event.guild.idLong)?.cancel(false)
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent)
    {
        // Soft 5 Minute
        synchronized(leaving) {
            leaving.put(event.guild.idLong, executor.schedule({
                manager.leaveGuild(event.guild)
            }, 5, TimeUnit.MINUTES))
        }
    }

}