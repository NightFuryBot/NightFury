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

import club.minnced.kjda.entities.isSelf
import club.minnced.kjda.then
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.entities.ModLogger
import me.kgustave.nightfury.extensions.action
import me.kgustave.nightfury.extensions.limit
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.hooks.EventListener

/**
 * @author Kaidan Gustave
 */
class AutoLoggingListener(private val manager: DatabaseManager, private val logger: ModLogger) : EventListener
{
    override fun onEvent(event: Event?) = when(event)
    {
        is GuildBanEvent -> onGuildBan(event)
        is GuildUnbanEvent -> onGuildUnban(event)
        is GuildMemberLeaveEvent -> onGuildMemberLeave(event)
        is GuildMemberRoleAddEvent -> onGuildMemberRoleAdd(event)
        is GuildMemberRoleRemoveEvent -> onGuildMemberRoleRemove(event)

        else -> Unit
    }

    // Handle Bans
    fun onGuildBan(event: GuildBanEvent)
    {
        if(!event.shouldLog()) return

        event.guild.auditLogs limit { 10 } action { ActionType.BAN } then {
            if(this==null) return@then

            val entry = with(this.stream().filter { it.targetIdLong == event.user.idLong && !it.user.isSelf }) {
                try { this.findFirst() } catch (e: NullPointerException) { null }
            }?:return@then

            entry.ifPresent {
                logger.newBan(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Unbans
    fun onGuildUnban(event: GuildUnbanEvent)
    {
        if(!event.shouldLog()) return

        event.guild.auditLogs limit { 10 } action { ActionType.UNBAN } then {
            if(this==null) return@then

            val entry = with(this.stream().filter { it.targetIdLong == event.user.idLong && !it.user.isSelf }) {
                try { this.findFirst() } catch (e: NullPointerException) { null }
            }?:return@then

            entry.ifPresent {
                logger.newUnban(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Kicks
    fun onGuildMemberLeave(event: GuildMemberLeaveEvent)
    {
        if(!event.shouldLog()) return

        event.guild.auditLogs limit { 10 } action { ActionType.KICK } then {
            if(this==null) return@then

            val entry = with(this.stream().filter { it.targetIdLong == event.user.idLong && !it.user.isSelf }) {
                try { this.findFirst() } catch (e: NullPointerException) { null }
            }?:return@then

            entry.ifPresent {
                logger.newKick(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Mutes
    fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent)
    {
        if(!event.shouldLog()) return
        val mutedRole = manager.getMutedRole(event.guild) ?: return

        if(!event.roles.contains(mutedRole)) return

        event.guild.auditLogs limit { 10 } action { ActionType.MEMBER_ROLE_UPDATE } then {
            if(this==null) return@then

            val entry = with(this.stream().filter { it.targetIdLong == event.user.idLong && !it.user.isSelf }) {
                try { this.findFirst() } catch (e: NullPointerException) { null }
            }?:return@then

            entry.ifPresent {
                logger.newMute(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Unmutes
    fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent)
    {
        if(!event.shouldLog()) return
        val mutedRole = manager.getMutedRole(event.guild) ?: return

        if(!event.roles.contains(mutedRole)) return

        event.guild.auditLogs limit { 10 } action { ActionType.MEMBER_ROLE_UPDATE } then {
            if(this==null) return@then

            val entry = with(this.stream().filter { it.targetIdLong == event.user.idLong && !it.user.isSelf }) {
                try { this.findFirst() } catch (e: NullPointerException) { null }
            }?:return@then

            entry.ifPresent {
                logger.newUnmute(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    private inline fun <reified T : GenericGuildEvent> T.shouldLog() =
            manager.getModLog(guild)!=null && guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)
}
