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
import club.minnced.kjda.promise
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.entities.ModLogger
import me.kgustave.nightfury.extensions.limit
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

/**
 * @author Kaidan Gustave
 */
class AutoLoggingListener(val manager: DatabaseManager, val logger: ModLogger) : ListenerAdapter()
{
    // Handle Bans
    override fun onGuildBan(event: GuildBanEvent)
    {
        if(!event.shouldLog()) return

        event.guild.auditLogs.limit { 10 }.promise() then {
            if(it==null) return@then

            val entry = with(it.stream().filter {
                it.type == ActionType.BAN && it.targetIdLong == event.user.idLong && !it.user.isSelf
            }) { try { this.findFirst() } catch (e: NullPointerException) { null } }?:return@then

            entry.ifPresent {
                logger.newBan(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Unbans
    override fun onGuildUnban(event: GuildUnbanEvent)
    {
        if(!event.shouldLog()) return

        event.guild.auditLogs.limit { 10 }.promise() then {
            if(it==null) return@then

            val entry = with(it.stream().filter {
                it.type == ActionType.UNBAN && it.targetIdLong == event.user.idLong && !it.user.isSelf
            }) { try { this.findFirst() } catch (e: NullPointerException) { null } }?:return@then

            entry.ifPresent {
                logger.newUnban(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Kicks
    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent)
    {
        if(!event.shouldLog()) return

        event.guild.auditLogs.limit { 10 }.promise() then {
            if(it==null) return@then

            val entry = with(it.stream().filter {
                it.type == ActionType.KICK && it.targetIdLong == event.user.idLong && !it.user.isSelf
            }) { try { this.findFirst() } catch (e: NullPointerException) { null } }?:return@then

            entry.ifPresent {
                logger.newKick(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Mutes
    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent)
    {
        if(!event.shouldLog()) return
        val mutedRole = manager.getMutedRole(event.guild) ?: return

        if(!event.roles.contains(mutedRole)) return

        event.guild.auditLogs.limit { 10 }.promise() then {
            if(it==null) return@then

            val entry = with(it.stream().filter {
                it.type == ActionType.MEMBER_ROLE_UPDATE && it.targetIdLong == event.member.user.idLong && !it.user.isSelf
            }) { try { this.findFirst() } catch (e: NullPointerException) { null } }?:return@then

            entry.ifPresent {
                logger.newMute(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Unmutes
    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent)
    {
        if(!event.shouldLog()) return
        val mutedRole = manager.getMutedRole(event.guild) ?: return

        if(!event.roles.contains(mutedRole)) return

        event.guild.auditLogs.limit { 10 }.promise() then {
            if(it==null) return@then

            val entry = with(it.stream().filter {
                it.type == ActionType.MEMBER_ROLE_UPDATE && it.targetIdLong == event.member.user.idLong && !it.user.isSelf
            }) { try { this.findFirst() } catch (e: NullPointerException) { null } }?:return@then

            entry.ifPresent {
                logger.newUnmute(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    private fun GenericGuildEvent.shouldLog() : Boolean {
        return manager.getModLog(guild)!=null && guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)
    }
}
