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
package xyz.nightfury

import com.jagrosh.jagtag.Parser
import com.jagrosh.jdautilities.waiter.EventWaiter
import xyz.nightfury.annotations.APICache
import xyz.nightfury.entities.ModLogger
import xyz.nightfury.entities.logging.NormalFilter
import xyz.nightfury.entities.logging.logLevel
import xyz.nightfury.resources.Arguments
import xyz.nightfury.listeners.*
import xyz.nightfury.db.Database
import xyz.nightfury.db.SQLCustomCommands
import xyz.nightfury.resources.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.impl.JDAImpl
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.requests.Requester
import okhttp3.*
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class Client internal constructor
(val prefix: String, val devId: Long, val manager: Database,
 val success: String, val warning: String, val error: String,
 val server: String, val dBotsKey: String, val dBorgKey: String,
 val waiter: EventWaiter, val parser: Parser, vararg commands: Command): EventListener
{
    //////////////////////
    // PRE-INIT MEMBERS //
    //////////////////////

    var totalGuilds : Int = 0
        private set

    var mode : CommandListener.Mode = CommandListener.Mode.STANDARD
        set(value)
        {
            NormalFilter.level = value.level.logLevel
            listener = value.listener
            field = value
        }

    val commands         : CommandMap               = CommandMap(*commands)
    val startTime        : OffsetDateTime           = OffsetDateTime.now()
    val logger           : ModLogger                = ModLogger(manager)
    val messageCacheSize : Int
        get() = callCache.size

    internal var listener : CommandListener = CommandListener.Mode.STANDARD.listener

    private val executor  : ScheduledExecutorService                  = Executors.newSingleThreadScheduledExecutor()
    private val cooldowns : MutableMap<String, OffsetDateTime>        = HashMap()
    private val uses      : MutableMap<String, Int>                   = HashMap()
    private val callCache : FixedSizeCache<Long, MutableSet<Message>> = FixedSizeCache(300)

    companion object
    {
        private val log: Logger = LoggerFactory.getLogger("Client")
    }

    //////////////////////
    // MEMBER FUNCTIONS //
    //////////////////////

    fun getRemainingCooldown(name: String) : Int
    {
        return if(cooldowns.containsKey(name)) {
            val time = OffsetDateTime.now().until(cooldowns[name], ChronoUnit.SECONDS).toInt()
            if(time <= 0) {
                cooldowns.remove(name); 0
            } else time
        } else 0
    }

    fun applyCooldown(name: String, seconds: Int)
    {
        cooldowns.put(name, OffsetDateTime.now().plusSeconds(seconds.toLong()))
    }

    fun cleanCooldowns()
    {
        val now = OffsetDateTime.now()
        cooldowns.keys.stream().filter { cooldowns[it]!!.isBefore(now) }.toList().forEach { cooldowns.remove(it) }
    }

    @Suppress("unused")
    fun getUsesFor(command: Command) : Int
    {
        synchronized(uses) { return uses.getOrDefault(command.name, 0) }
    }

    fun incrementUses(command: Command)
    {
        synchronized(uses) { uses.put(command.name, uses.getOrDefault(command.name, 0)+1) }
    }

    fun searchCommand(query: String): Command?
    {
        val splitQuery = query.split(Arguments.commandArgs, 2)
        return commands.firstOrNull { it.isForCommand(splitQuery[0]) }
                ?.findChild(if(splitQuery.size > 1) splitQuery[1] else "")
    }

    //////////////////////
    //      EVENTS      //
    //////////////////////

    override fun onEvent(event: Event?)
    {
        when(event)
        {
            is MessageReceivedEvent -> onMessageReceived(event)
            is MessageDeleteEvent   -> onMessageDelete(event)
            is GuildMemberJoinEvent -> onGuildMemberJoin(event)
            is ReadyEvent           -> onReady(event)
            is GuildJoinEvent       -> {
                if(event.guild.selfMember.joinDate.plusMinutes(5).isAfter(OffsetDateTime.now()))
                    if(event.guild.isGood) updateStats(event.jda) else event.guild.leave().queue()
            }
            is GuildLeaveEvent      -> updateStats(event.jda)
            is ShutdownEvent        -> onShutdown(event)

            else -> Unit
        }
    }

    private fun onReady(event: ReadyEvent)
    {
        event.jda.addEventListener(waiter, DatabaseListener(manager), AutoLoggingListener(manager, logger))
        event.jda.presence.status = OnlineStatus.ONLINE
        event.jda.presence.game = Game.of("Type ${prefix}help")

        val si = event.jda.shardInfo
        log.info("${if(si == null) "NightFury" else "[${si.shardId} / ${si.shardTotal - 1}]"} is Online!")

        val toLeave = event.jda.guilds.stream().filter { !it.isGood }.toList()
        if(toLeave.isNotEmpty())
        {
            toLeave.forEach { it.leave().queue() }
            log.info("Left ${toLeave.size} bad guilds!")
        }

        // Clear Caches every hour
        if(si == null || si.shardId == 0)
        {
            executor.scheduleAtFixedRate({
                try {
                    clearAPICaches()
                    cleanCooldowns()
                } catch(e: Exception) {
                    log.error("Failed to clear caches!", e)
                }
            }, 0, 1, TimeUnit.HOURS)
        }

        updateStats(event.jda)
    }

    private fun onMessageReceived(event: MessageReceivedEvent)
    {
        if(event.author.isBot)
            return
        val rawContent = event.message.rawContent.trim()
        val parts: List<String>

        when
        {
            rawContent.startsWith(prefix, true) -> // From Anywhere with default prefix
            {
                parts = rawContent.substring(prefix.length).trim().split(Arguments.commandArgs, 2)
            }

            event.guild != null -> // From Guild without default prefix
            {
                val prefixes = manager.getPrefixes(event.guild)
                if(prefixes.isNotEmpty())
                {
                    val prefix = prefixes.find { rawContent.startsWith(it, true) }
                    if(prefix!=null)
                    {
                        parts = rawContent.substring(prefix.length).trim().split(Arguments.commandArgs, 2)
                    }
                    else return
                }
                else return
            }

            else -> return // No match, not a command call
        }

        val name = parts[0]
        val args = if(parts.size == 2) parts[1] else ""
        if(listener.checkCall(event, this, name, args))
        {
            val command = commands[name]
            val commandEvent = CommandEvent(event, args.trim(), this)
            if(command != null)
            {
                listener.onCommandCall(commandEvent, command)
                return command.run(commandEvent)
            }
            if(event.isFromType(ChannelType.TEXT))
            {
                val customCommandContent = SQLCustomCommands.getContentFor(name, event.guild)
                if(customCommandContent.isNotEmpty())
                    return commandEvent.reply(parser.clear()
                            .put("user", event.author)
                            .put("guild", event.guild)
                            .put("channel", event.textChannel)
                            .put("args", args)
                            .parse(customCommandContent))
            }
        }
    }

    private fun onMessageDelete(event: MessageDeleteEvent)
    {
        if(!event.isFromType(ChannelType.TEXT))
            return
        synchronized(callCache)
        {
            val messages = callCache[event.messageIdLong] ?: return
            if(messages.size > 1 && event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_MANAGE))
                event.textChannel.deleteMessages(messages).queue({},{})
            else if(messages.size == 1)
                messages.forEach { it.delete().queue({},{}) }
        }
    }

    private fun onShutdown(event: ShutdownEvent)
    {
        val si = event.jda.shardInfo
        val identifier = if(si != null) "Shard [${si.shardId} / ${si.shardTotal - 1}]" else "JDA"
        val cc = event.closeCode
        log.info("$identifier has shutdown.")
        log.debug("Shutdown Info:\n" +
                                                 "- Shard ID: ${si?.shardId ?: "0 (No Shard)"}\n" +
                                                 "- Close Code: ${if(cc != null) "${cc.code} - ${cc.meaning}" else "${event.code} - Unknown Code!"}\n" +
                                                 "- Time: ${event.shutdownTime}")
        executor.shutdownNow()
        manager.close()
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent)
    {
        // If there's no welcome channel then we just return.
        val welcomeChannel = manager.getWelcomeChannel(event.guild)?:return

        // We can't even send messages to the channel so we return
        if(!welcomeChannel.canTalk()) return

        // We prevent possible spam by creating a cooldown key 'welcomes|U:<User ID>|G:<Guild ID>'
        val cooldownKey = "welcomes|U:${event.user.idLong}|G:${event.guild.idLong}"
        val remaining = getRemainingCooldown(cooldownKey)

        // Still on cooldown - we're done here
        if(remaining>0) return

        val message = parser.clear()
                .put("guild", event.guild)
                .put("channel", welcomeChannel)
                .put("user", event.user)
                .parse(manager.getWelcomeMessage(event.guild))

        // Too long or empty means we can't send, so we just return because it'll break otherwise
        if(message.isEmpty() || message.length>2000) return

        // Send Message
        welcomeChannel.sendMessage(message).queue()

        // Apply cooldown
        applyCooldown(cooldownKey, 100)
    }

    //////////////////////
    // INTERNAL MEMBERS //
    //////////////////////

    internal fun linkIds(id: Long, message: Message)
    {
        synchronized(callCache)
        {
            val stored = callCache[id]
            if(stored != null)
                stored.add(message)
            else
            {
                val toStore = HashSet<Message>()
                toStore.add(message)
                callCache[id] = toStore
            }
        }
    }

    /////////////////////
    // PRIVATE MEMBERS //
    /////////////////////

    private inline val Guild.isGood : Boolean
        inline get() = members.stream().filter { it.user.isBot }.count()<=30 || getMemberById(devId)!=null

    private fun clearAPICaches()
    {
        commands.stream().filter {
            it::class.findAnnotation<APICache>() != null
        }.forEach { cmd -> cmd::class.functions.stream()
                .filter { it.findAnnotation<APICache>() != null }
                .findFirst().ifPresent { it.call(cmd) }
        }
    }

    private fun updateStats(jda: JDA)
    {
        val client = (jda as JDAImpl).httpClientBuilder.build()
        val body = JSONObject().put("server_count", jda.guilds.size)

        if(jda.shardInfo != null)
            body.put("shard_id", jda.shardInfo.shardId).put("shard_count", jda.shardInfo.shardTotal)

        // Create POST request to bots.discord.pw
        client.newRequest({
            post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
            url("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
            header("Authorization", dBotsKey)
            header("Content-Type", "application/json")
        }).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = response.close()

            override fun onFailure(call: Call, e: IOException)
            {
                log.error("Failed to send information to bots.discord.pw", e)
            }
        })

        // Send POST request to discordbots.org
        client.newRequest({
            post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
            url("https://discordbots.org/api/bots/${jda.selfUser.id}/stats")
            header("Authorization", dBorgKey)
            header("Content-Type", "application/json")
        }).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = response.close()

            override fun onFailure(call: Call, e: IOException) {
                log.error("Failed to send information to discordbots.org", e)
            }
        })

        // If we're not sharded there's no reason to send a GET request
        if(jda.shardInfo == null)
        {
            totalGuilds = jda.guilds.size
            return
        }

        // Send GET request to bots.discord.pw
        try {
            client.newRequest({
                get().url("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
                header("Authorization", dBotsKey)
                header("Content-Type", "application/json")
            }).execute().body()!!.charStream().use {
                val json = JSONObject(JSONTokener(it))

                var total = 0

                log.debug("Received JSON from bots.discord.pw:\n${json.toString(2)}")

                json.getJSONArray("stats").mapNotNull {
                    (it as? JSONObject).takeIf { it?.has("server_count") == true }
                }.forEach { total += it["server_count"] as Int }

                totalGuilds = total
            }
        } catch (e: Exception) {
            log.error("Failed to retrieve bot shard information from bots.discord.pw", e)
        }
    }

    private inline fun OkHttpClient.newRequest(lazy: Request.Builder.() -> Unit) : Call
    {
        val builder = Request.Builder()
        builder.lazy()
        return newCall(builder.build())
    }
}