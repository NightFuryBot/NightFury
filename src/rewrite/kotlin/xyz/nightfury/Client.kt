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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.nightfury

import com.jagrosh.jagtag.Parser
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.requests.Requester
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.owner.OwnerGroup
import xyz.nightfury.command.standard.StandardGroup
import xyz.nightfury.ndb.Database
import xyz.nightfury.listeners.SuspendedListener
import xyz.nightfury.util.collections.FixedSizeCache
import xyz.nightfury.util.*
import xyz.nightfury.util.collections.CaseInsensitiveHashMap
import xyz.nightfury.util.collections.CommandMap
import xyz.nightfury.util.db.*
import xyz.nightfury.util.ext.await
import xyz.nightfury.util.ext.newRequest
import java.io.IOException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet

/**
 * @author Kaidan Gustave
 */
class Client(
    val prefix: String,
    private val dBotsKey: String?,
    private val dBorgKey: String?,
    val parser: Parser
): SuspendedListener, EventListener {
    companion object {
        val LOG = createLogger(Client::class)
    }

    private val cooldowns = HashMap<String, OffsetDateTime>()
    private val uses = CaseInsensitiveHashMap<Int>()
    private val cycleContext = newSingleThreadContext("CycleContext")
    private val callCache = FixedSizeCache<Long, HashSet<Message>>(300)

    val httpClient: OkHttpClient = NightFury.HTTP_CLIENT_BUILDER.build()
    val groups = arrayOf(StandardGroup, OwnerGroup)
    val commands: Map<String, Command> = CommandMap(*groups)
    val startTime: OffsetDateTime = OffsetDateTime.now()

    val messageCacheSize: Int get() = callCache.size

    var mode = ClientMode.SERVICE
    var totalGuilds: Int = 0
        private set

    fun getRemainingCooldown(name: String): Int {
        return if(name in cooldowns) {
            val time = OffsetDateTime.now().until(cooldowns[name]!!, ChronoUnit.SECONDS).toInt()
            if(time <= 0) {
                cooldowns.remove(name)
                return 0 // Return zero because the cooldown is expired
            } else time
        } else 0
    }

    fun applyCooldown(name: String, seconds: Int) {
        cooldowns[name] = OffsetDateTime.now().plusSeconds(seconds.toLong())
    }

    fun cleanCooldowns() {
        val now = OffsetDateTime.now()
        cooldowns.entries.filter { it.value.isBefore(now) }.forEach { cooldowns -= it.key }
    }

    fun incrementUses(command: Command) {
        uses[command.name] = (uses[command.name] ?: 0) + 1
    }

    fun searchCommand(query: String): Command? {
        val splitQuery = query.split(commandArgs, 2)
        if(splitQuery.isEmpty())
            return null
        return commands[splitQuery[0]]?.findChild(if(splitQuery.size > 1) splitQuery[1] else "")
    }

    override suspend fun CoroutineScope.onEvent(event: Event) {
        when(event) {
            is MessageReceivedEvent -> onMessageReceived(event)
            is ReadyEvent -> onReady(event)
        }
    }

    override fun onEvent(event: Event) {
        when(event) {
            is ShutdownEvent -> onShutdown(event)
            is MessageDeleteEvent -> onMessageDelete(event)
            is GuildMemberJoinEvent -> onGuildMemberJoin(event)
        }
    }

    private suspend fun CoroutineScope.onReady(event: ReadyEvent) {
        with(event.jda.presence) {
            status = OnlineStatus.ONLINE
            game = listeningTo("type ${prefix}help")
        }

        val si = event.jda.shardInfo
        LOG.info("${si?.let { "[${it.shardId} / ${it.shardTotal - 1}]" } ?: "NightFury"} is Online!")

        val toLeave = event.jda.guilds.filter { !it.isGood }
        if(toLeave.isNotEmpty()) {
            toLeave.forEach { it.leave().queue() }
            LOG.info("Left ${toLeave.size} bad guilds!")
        }

        // Clear Caches every hour
        if(si === null || si.shardId == 0) {
            launch(cycleContext) {
                while(isActive) {
                    commands.mapNotNull { it as? Cleanable }.forEach { it.clean() }
                    cleanCooldowns()
                    delay(1, TimeUnit.HOURS)
                }
            }
        }

        updateStats(event.jda)
    }

    private suspend fun CoroutineScope.onMessageReceived(event: MessageReceivedEvent) {
        // Do not allow bots to trigger any sort of command
        if(event.author.isBot)
            return

        val raw = event.message.contentRaw
        val guild = event.guild

        val parts = when {
            raw.startsWith(prefix, true) -> {
                raw.substring(prefix.length).trim().split(commandArgs, 2)
            }

            guild !== null -> {
                val prefixes = event.guild.prefixes

                if(prefixes.isEmpty())
                    return

                val prefix = prefixes.find { raw.startsWith(it, true) } ?: return
                raw.substring(prefix.length).trim().split(commandArgs, 2)
            }

            else -> return
        }

        val name = parts[0].toLowerCase()
        val args = if(parts.size == 2) parts[1] else ""
        if(mode.checkCall(event, this@Client, name, args)) {
            val ctx = CommandContext(event, this@Client, args, coroutineContext)
            commands[name]?.let { command ->
                mode.onCommandCall(ctx, command)
                return command.run(ctx)
            }
        }
    }

    private fun onMessageDelete(event: MessageDeleteEvent) {
        if(!event.isFromType(ChannelType.TEXT))
            return

        synchronized(callCache) {
            callCache[event.messageIdLong]?.let {
                val channel = event.textChannel
                if(it.size > 1 && event.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE)) {
                    channel.deleteMessages(it).queue()
                } else {
                    it.forEach { it.delete().queue() }
                }
            }
        }
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val guild = event.guild
        // If there's no welcome channel then we just return.
        val welcomeChannel = guild.welcomeChannel ?: return

        // Past here we should have a non-null message, so we log it if by some chance it is null
        val welcomeMessage = guild.welcomeMessage ?: return LOG.warn(
            "Got a null welcome message for a registered guild (ID: ${guild.idLong})"
        )

        // We can't even send messages to the channel so we return
        if(!guild.selfMember.hasPermission(welcomeChannel, Permission.MESSAGE_WRITE)) return

        // We prevent possible spam by creating a cooldown key 'welcomes|U:<User ID>|G:<Guild ID>'
        val cooldownKey = "welcomes|U:${event.user.idLong}|G:${guild.idLong}"
        val remaining = getRemainingCooldown(cooldownKey)

        // Still on cooldown - we're done here
        if(remaining > 0) return

        val message = parser.clear()
            .put("guild", guild)
            .put("channel", welcomeChannel)
            .put("user", event.user)
            .parse(welcomeMessage)

        // Too long or empty means we can't send, so we just return because it'll break otherwise
        if(message.isEmpty() || message.length > 2000) return

        // Send Message
        welcomeChannel.sendMessage(message).queue()

        // Apply cooldown
        applyCooldown(cooldownKey, 100)
    }

    private fun onShutdown(event: ShutdownEvent) {
        val identifier = event.jda.shardInfo?.let { "Shard [${it.shardId} / ${it.shardTotal - 1}]" } ?: "JDA"
        LOG.info("$identifier has shutdown.")
        cycleContext.close()
        Database.close()
    }

    private inline val Guild.isGood : Boolean inline get() {
        if(isBlacklisted)
            return false
        if(isJoinWhitelisted)
            return true
        return members.count { it.user.isBot } <= 30 || getMemberById(NightFury.DEV_ID) !== null
    }

    private suspend fun CoroutineScope.updateStats(jda: JDA) {
        val body = JSONObject().put("server_count", jda.guilds.size)

        jda.shardInfo?.let { body.put("shard_id", it.shardId).put("shard_count", it.shardTotal) }

        dBotsKey?.let {
            // Run this as a child job
            launch(coroutineContext) {
                try {
                    // Send POST request to bots.discord.pw
                    httpClient.newRequest({
                        post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
                        url("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
                        header("Authorization", dBotsKey)
                        header("Content-Type", "application/json")
                    }).await().close()
                } catch(e: IOException) {
                    LOG.error("Failed to send information to bots.discord.pw", e)
                }
            }
        }

        dBorgKey?.let {
            // Run this as a child job
            launch(coroutineContext) {
                try {
                    // Send POST request to discordbots.org
                    httpClient.newRequest({
                        post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
                        url("https://discordbots.org/api/bots/${jda.selfUser.id}/stats")
                        header("Authorization", dBorgKey)
                        header("Content-Type", "application/json")
                    }).await().close()
                } catch(e: IOException) {
                    LOG.error("Failed to send information to discordbots.org", e)
                }
            }
        }

        // If we're not sharded there's no reason to send a GET request
        if(jda.shardInfo === null || dBotsKey === null) {
            totalGuilds = jda.guilds.size
            return
        }

        try {
            // Send GET request to bots.discord.pw
            httpClient.newRequest {
                get().url("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
                header("Authorization", dBotsKey)
                header("Content-Type", "application/json")
            }.await().body()?.charStream()?.use {
                val json = JSONObject(JSONTokener(it))
                LOG.debug("Received JSON from bots.discord.pw:\n${json.toString(2)}")
                totalGuilds = json.getJSONArray("stats").mapNotNull {
                    val obj = it as? JSONObject
                    obj?.takeIf { obj.has("server_count") && !obj.isNull("server_count") }
                }.sumBy { it["server_count"] as Int }
            }
        } catch (e: Exception) {
            LOG.error("Failed to retrieve bot shard information from bots.discord.pw", e)
        }
    }

    internal fun linkCall(id: Long, message: Message) {
        if(!message.channelType.isGuild) return
        synchronized(callCache) {
            callCache.computeIfAbsent(id) { HashSet() } += message
        }
    }
}
