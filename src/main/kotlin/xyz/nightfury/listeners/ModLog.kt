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

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.audit.AuditLogEntry
import net.dv8tion.jda.core.audit.AuditLogKey
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import xyz.nightfury.ndb.entities.Case
import xyz.nightfury.util.createLogger
import xyz.nightfury.util.db.*
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.formattedName
import xyz.nightfury.util.jda.isSelf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
object ModLog : SuspendedListener {
    @Volatile private var running = false

    private const val REASON_LINE_FORMAT = "`[ REASON ]` %s"
    private const val FORMAT = "`[  CASE  ]` `[%d]` %s %s %s **%s** (ID: %d)\n$REASON_LINE_FORMAT"
    private const val DEFAULT_REASON = "none"
    private const val RETRIEVE_AMOUNT = 20

    private val context by lazy { newSingleThreadContext("ModLog-Context") }
    private val eventCache = ConcurrentLinkedQueue<Event>()
    private val auditCache = ConcurrentHashMap<Long, List<AuditLogEntry>>()

    val LOG = createLogger(ModLog::class)

    override suspend fun CoroutineScope.onEvent(event: Event) {
        val guild = when(event) {
            is GuildBanEvent -> event.guild
            is GuildUnbanEvent -> event.guild
            is GuildMemberLeaveEvent -> event.guild
            is GuildMemberRoleAddEvent -> {
                event.guild.takeIf { it.mutedRole?.let { it in event.roles } == true }
            }
            is GuildMemberRoleRemoveEvent -> {
                event.guild.takeIf { it.mutedRole?.let { it in event.roles } == true }
            }

            is ShutdownEvent -> {
                // Time to shut down
                eventCache.clear()
                auditCache.clear()
                running = false
                this@ModLog.context.cancel(CancellationException("Shutdown Event was fired"))
                this@ModLog.context.close()
                return
            }

            else -> null
        }

        if(guild === null || !guild.hasModLog)
            return

        if(guild.modLog?.canTalk().let { it == null || it == false })
            return

        launch(coroutineContext) {
            // We need to wait a second in case the audit
            // log is slow to update.
            delay(1, TimeUnit.SECONDS)
            LOG.debug("Got moderator event to log...")
            eventCache += event
            start()
        }
    }

    suspend fun newBan(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, Case.Action.BAN, reason)
    }

    suspend fun newUnban(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, Case.Action.UNBAN, reason)
    }

    suspend fun newKick(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, Case.Action.KICK, reason)
    }

    suspend fun newMute(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, Case.Action.MUTE, reason)
    }

    suspend fun newUnmute(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, Case.Action.UNMUTE, reason)
    }

    suspend fun newClean(moderator: Member, target: TextChannel, msgs: Int, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, Case.Action.CLEAN, reason, msgs)
    }

    suspend fun editReason(logMessage: Message, reason: String) {
        require(logMessage.author.isSelf) { "ModLog message provided was from a different user!" }

        val parts = logMessage.contentRaw.split('\n', limit = 2)

        require(parts.isNotEmpty()) { "ModLog message when split was an empty collection!" }

        val newContent = "${parts[0]}\n${REASON_LINE_FORMAT.format(reason)}"

        logMessage.editMessage(newContent).await()
    }

    private fun start() {
        if(running) return
        running = true
        launch(context) {
            LOG.debug("Starting new cache clear (Playing ${eventCache.size} events)...")
            try {
                while(running) {
                    // Wait here and check every two seconds until the eventCache is not empty
                    while(eventCache.isEmpty()) {
                        delay(2, TimeUnit.SECONDS)
                    }

                    while(eventCache.isNotEmpty()) try {
                        val event = eventCache.poll()
                        when(event) {
                            is GuildBanEvent -> onBan(event, pollLogOrGetFromCache(event.guild))
                            is GuildUnbanEvent -> onUnban(event, pollLogOrGetFromCache(event.guild))
                            is GuildMemberLeaveEvent -> onLeave(event, pollLogOrGetFromCache(event.guild))
                            is GuildMemberRoleAddEvent -> onRoleAdd(event, pollLogOrGetFromCache(event.guild))
                            is GuildMemberRoleRemoveEvent -> onRoleRemove(event, pollLogOrGetFromCache(event.guild))
                        }
                    } catch(t: Throwable) {
                        if(t is CancellationException) throw t
                    } finally {
                        // Always clear auditCache after a pass
                        LOG.debug("Clearing audit cache...")
                        auditCache.clear()
                    }
                }
            } catch(cancellation: CancellationException) {
                LOG.debug("Encountered cancellation exception while running ModLog process: ${cancellation.message}")
            }
        }.invokeOnCompletion {
            LOG.debug("ModLog process ended.")
            it?.let { LOG.warn("ModLog process ended due to an exception", it) }

            auditCache.clear()
            running = false
        }
    }

    private suspend fun onBan(event: GuildBanEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val target = event.user
        val entry = entries.firstOrNull { it.type == ActionType.BAN && it.targetIdLong == target.idLong }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return LOG.warn("Could not find ban for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return
        val reason = entry.reason ?: DEFAULT_REASON

        log(modLog, target, moderator, Case.Action.BAN, reason)
    }

    private suspend fun onUnban(event: GuildUnbanEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val target = event.user
        val entry = entries.firstOrNull { it.type == ActionType.UNBAN && it.targetIdLong == target.idLong }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return LOG.warn("Could not find unban for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return
        val reason = entry.reason ?: DEFAULT_REASON

        log(modLog, target, moderator, Case.Action.UNBAN, reason)
    }

    private suspend fun onLeave(event: GuildMemberLeaveEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val target = event.user

        // We are not going to log multiple manual kicks by the same user
        // my reasoning for this is because I'd rather lean towards when
        // deciding when not to log than when to log
        val entry = entries.singleOrNull { it.type == ActionType.KICK && it.targetIdLong == target.idLong }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            // This will happen more often than not, and thus
            // we don't log it at warn level
            return LOG.debug("Could not find kick for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return
        val reason = entry.reason ?: DEFAULT_REASON

        log(modLog, target, moderator, Case.Action.KICK, reason)
    }

    private suspend fun onRoleAdd(event: GuildMemberRoleAddEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val mutedRole = guild.mutedRole ?: return
        val target = event.user
        val entry = entries.firstOrNull entry@ {
            if(it.type == ActionType.MEMBER_ROLE_UPDATE && it.targetIdLong == target.idLong) {
                val change = it.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD)
                if(change === null)
                    return@entry false

                return@entry change.getNewValue<List<Map<String, String>>>().any { it["name"] == mutedRole.name }
            }

            return@entry false
        }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return LOG.warn("Could not find role update for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return

        log(modLog, target, moderator, Case.Action.MUTE, null)
    }

    private suspend fun onRoleRemove(event: GuildMemberRoleRemoveEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val mutedRole = guild.mutedRole ?: return
        val target = event.user
        val entry = entries.firstOrNull entry@ {
            if(it.type == ActionType.MEMBER_ROLE_UPDATE && it.targetIdLong == target.idLong) {
                val change = it.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE)
                if(change === null)
                    return@entry false

                return@entry change.getNewValue<List<Map<String, String>>>().any { it["name"] == mutedRole.name }
            }

            return@entry false
        }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return LOG.warn("Could not find role update for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return

        log(modLog, target, moderator, Case.Action.UNMUTE, null)
    }

    private suspend fun log(modLog: TextChannel, target: ISnowflake, moderator: User,
                            action: Case.Action, reason: String?, argument: Any? = null) {
        val guild = modLog.guild
        val number = guild.lastCaseNumber + 1

        val text = FORMAT.format(
            number,
            action.emote(),
            moderator.formattedName(true),
            action.keyword(argument),
            (target as? User)?.name ?: (target as? TextChannel)?.asMention ?: "UNKNOWN",
            target.idLong,
            reason ?: DEFAULT_REASON
        )

        LOG.debug("Sending case to Moderator Log (ID: ${modLog.idLong}, Guild ID: ${guild.idLong})")
        val message = try {
            modLog.sendMessage(text).await()
        } catch(t: Throwable) {
            return LOG.error("An error occurred while sending a case to a ModLog!", t)
        }

        val case = Case(number, guild.idLong, message.idLong, moderator.idLong,
            target.idLong, target is User, action, reason)

        guild.addCase(case)
    }

    private suspend fun pollLogOrGetFromCache(guild: Guild): List<AuditLogEntry> {
        return auditCache[guild.idLong] ?: run {
            LOG.debug("Polling AuditLog for Guild ID: ${guild.idLong} (max $RETRIEVE_AMOUNT entries)")
            guild.auditLogs.limit(RETRIEVE_AMOUNT).await().also {
                auditCache[guild.idLong] = it
            }
        }
    }

    private fun Case.Action.keyword(argument: Any? = null): String {
        return when(this) {
            Case.Action.BAN -> "banned"
            Case.Action.KICK -> "kicked"
            Case.Action.UNBAN -> "unbanned"
            Case.Action.MUTE -> "muted"
            Case.Action.UNMUTE -> "unmuted"
            Case.Action.CLEAN -> "cleaned %d messages in".format(argument)
            Case.Action.OTHER -> ""
        }
    }

    private fun Case.Action.emote(): String {
        return when(this) {
            Case.Action.BAN -> "\uD83D\uDD28"
            Case.Action.KICK -> "\uD83D\uDC62"
            Case.Action.MUTE -> "\uD83D\uDD07"
            Case.Action.UNMUTE -> "\uD83D\uDD08"
            else -> ""
        }
    }
}
