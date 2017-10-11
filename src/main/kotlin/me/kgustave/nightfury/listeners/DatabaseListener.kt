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
import me.kgustave.nightfury.extensions.refreshMutedRole
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

/**
 * @author Kaidan Gustave
 */
class DatabaseListener(private val manager: DatabaseManager) : EventListener
{
    override fun onEvent(event: Event?) = when(event)
    {
        is ReadyEvent              -> onReady(event)
        is RoleDeleteEvent         -> onRoleDelete(event)
        is TextChannelCreateEvent  -> onTextChannelCreate(event)
        is TextChannelDeleteEvent  -> onTextChannelDelete(event)
        is VoiceChannelCreateEvent -> onVoiceChannelCreate(event)
        is CategoryCreateEvent     -> onCategoryCreate(event)
        is GuildMemberJoinEvent    -> onGuildMemberJoin(event)
        is GuildMemberLeaveEvent   -> onGuildMemberLeave(event)

        else -> Unit
    }

    fun onReady(event: ReadyEvent)
    {
        event.jda.guilds.forEach {
            val muted = manager getMutedRole it
            if(muted!=null)
                it refreshMutedRole muted
        }
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

    // If the guild has a type of channel and it equals the deleted channel, then it's removed
    // if the type of channel is null, but the database contains info regarding that type, it
    // is also removed
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

    fun onCategoryCreate(event: CategoryCreateEvent)
    {
        val muted = manager.getMutedRole(event.guild)
        if(muted!=null)
            event.category muteRole muted
    }

    fun onGuildMemberLeave(event: GuildMemberLeaveEvent)
    {
        if(manager.isRolePersist(event.guild))
            manager.addRolePersist(event.member)
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent)
    {
        if(manager.isRolePersist(event.guild))
        {
            val roles = manager.getRolePersistence(event.member)
            if(roles.isNotEmpty())
            {
                event.guild.controller.addRolesToMember(event.member, roles).queue()
                manager.removeRolePersist(event.member)
            }
        }
    }

}