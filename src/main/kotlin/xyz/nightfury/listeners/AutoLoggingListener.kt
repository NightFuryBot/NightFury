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
package xyz.nightfury.listeners

import xyz.nightfury.entities.ModLogger
import xyz.nightfury.entities.then
import xyz.nightfury.extensions.action
import xyz.nightfury.extensions.isSelf
import xyz.nightfury.extensions.limit
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.audit.AuditLogEntry
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.nightfury.db.SQLModeratorLog
import xyz.nightfury.db.SQLMutedRole
import java.time.OffsetDateTime

/**
 * @author Kaidan Gustave
 */
class AutoLoggingListener : EventListener {

    private val previouslyKicked: MutableMap<Long, Long> = HashMap()

    override fun onEvent(event: Event?) {
        when(event) {
            is GuildBanEvent -> onGuildBan(event)
            is GuildUnbanEvent -> onGuildUnban(event)
            is GuildMemberLeaveEvent -> onGuildMemberLeave(event)
            is GuildMemberRoleAddEvent -> onGuildMemberRoleAdd(event)
            is GuildMemberRoleRemoveEvent -> onGuildMemberRoleRemove(event)
        }
    }

    // Handle Bans
    fun onGuildBan(event: GuildBanEvent) {
        if(!event.shouldLog()) return

        event.guild.auditLogs limit { 10 } action { ActionType.BAN } then {
            if(this==null) return@then

            val entry = stream().filter {
                it.targetIdLong == event.user.idLong &&
                !it.user.isSelf &&
                it.creationTime.plusMinutes(2).isBefore(OffsetDateTime.now())
            }.run { try { findFirst() } catch(e: NumberFormatException) { null } } ?: return@then

            entry.ifPresent {
                ModLogger.newBan(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Unbans
    fun onGuildUnban(event: GuildUnbanEvent) {
        if(!event.shouldLog()) return

        event.guild.auditLogs limit { 10 } action { ActionType.UNBAN } then {
            if(this==null) return@then

            val entry = stream().filter {
                it.targetIdLong == event.user.idLong &&
                !it.user.isSelf &&
                it.creationTime.plusMinutes(2).isBefore(OffsetDateTime.now())
            }.run { try { findFirst() } catch(e: NumberFormatException) { null } } ?: return@then

            entry.ifPresent {
                ModLogger.newUnban(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Kicks
    fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if(!event.shouldLog()) return

        event.guild.auditLogs limit { 10 } action { ActionType.KICK } then {
            if(this == null) return@then

            val entry = stream().filter { it.test(event) }
                            .run { try { findFirst() } catch(e: NumberFormatException) { null } } ?: return@then

            entry.ifPresent {
                // As I learned after creating this system, kicks are one case where you
                // cannot feasibly determine correctness within reasonable measure with
                // just info given by the audit-logs.
                // To solve this, we cache the last kick ID, and pair it with the guild ID
                // as a key for it.
                // Now if we come to the point where yet again the same person is leaving
                // we just return, otherwise we cache the new value.
                // It doesn't really work, but nothing else does anyways, so why the hell not.
                synchronized(previouslyKicked) {
                    if(previouslyKicked.containsKey(event.guild.idLong)) {
                        val id = previouslyKicked[event.guild.idLong] ?: run {
                            previouslyKicked[event.guild.idLong] = it.targetIdLong
                            return@synchronized
                        }

                        if(id == it.targetIdLong) {
                            // Go to very top, do not log anything
                            return@ifPresent
                        } else {
                            // Cache new latest kick
                            previouslyKicked[event.guild.idLong] = it.targetIdLong
                        }
                    }
                }
                ModLogger.newKick(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Mutes
    fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if(!event.shouldLog()) return
        val mutedRole = SQLMutedRole.getRole(event.guild) ?: return

        if(!event.roles.contains(mutedRole)) return

        event.guild.auditLogs limit { 10 } action { ActionType.MEMBER_ROLE_UPDATE } then {
            if(this==null) return@then

            val entry = stream().filter { it.test(event) }
                            .run { try { findFirst() } catch(e: NumberFormatException) { null } } ?: return@then

            entry.ifPresent {
                ModLogger.newMute(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    // Handle Unmutes
    fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if(!event.shouldLog()) return
        val mutedRole = SQLMutedRole.getRole(event.guild) ?: return

        if(!event.roles.contains(mutedRole)) return

        event.guild.auditLogs limit { 10 } action { ActionType.MEMBER_ROLE_UPDATE } then {
            if(this==null) return@then

            val entry = stream().filter { it.test(event) }
                            .run { try { findFirst() } catch(e: NumberFormatException) { null } } ?: return@then

            entry.ifPresent {
                ModLogger.newUnmute(event.guild, it.user, event.user, it.reason)
            }
        }
    }

    private fun AuditLogEntry.test(event: GenericGuildMemberEvent) = targetIdLong == event.user.idLong &&
                                                                     !user.isSelf &&
                                                                     creationTime.plusMinutes(2).isBefore(OffsetDateTime.now())

    private inline fun <reified T : GenericGuildEvent> T.shouldLog() =
        SQLModeratorLog.hasChannel(guild) && guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)
}
