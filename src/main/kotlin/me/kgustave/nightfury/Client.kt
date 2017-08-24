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
@file:Suppress("unused")
package me.kgustave.nightfury

import com.jagrosh.jagtag.Parser
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.annotations.APICache
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.entities.ModLogger
import me.kgustave.nightfury.listeners.*
import me.kgustave.nightfury.listeners.command.*
import me.kgustave.nightfury.resources.*
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
import net.dv8tion.jda.core.utils.SimpleLog
import okhttp3.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.full.functions
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class Client internal constructor
(val prefix: String, val devId: Long, val manager: DatabaseManager,
 val success: String, val warning: String, val error: String,
 val server: String, val dBotsKey : String, val waiter: EventWaiter,
 val parser: Parser, vararg commands: Command) : EventListener
{
    //////////////////////
    // PRE-INIT MEMBERS //
    //////////////////////

    var totalGuilds : Int = 0
        private set

    val commands         : CommandMap = CommandMap(*commands)
    val startTime        : OffsetDateTime = OffsetDateTime.now()
    val logger           : ModLogger = ModLogger(manager)
    val messageCacheSize : Int
        get() = callCache.size

    internal var listener : CommandListener

    private val executor  : ScheduledExecutorService               = Executors.newSingleThreadScheduledExecutor()
    private val cooldowns : HashMap<String, OffsetDateTime>        = HashMap()
    private val scheduled : HashMap<String, ScheduledFuture<*>>    = HashMap()
    private val uses      : HashMap<String, Int>                   = HashMap()
    private val callCache : FixedSizeCache<Long, HashSet<Message>> = FixedSizeCache(300)

    private val listeners : Array<CommandListener> = arrayOf(StandardListener(), IdleListener(), DebugListener())

    init
    {
        listener = listeners[0]
    }

    companion object
    {
        private val log = SimpleLog.getLog("Client")
        private infix fun log(e : Exception) = log.log(e)
    }

    fun targetListener(name: String)
    {
        val l = listeners.filter { it.name == name.toLowerCase() }
        if(l.isNotEmpty()) listener = l[0]
    }

    fun getRemainingCooldown(name: String): Int
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

    fun hasFuture(key: String) : Boolean
    {
        synchronized(scheduled) { return scheduled.containsKey(key.toLowerCase()) }
    }

    fun saveFuture(key: String, future: ScheduledFuture<*>)
    {
        synchronized(scheduled) { scheduled.put(key.toLowerCase(),future) }
    }

    fun cancelFuture(key: String)
    {
        synchronized(scheduled) { scheduled[key.toLowerCase()]?.cancel(false) }
        removeFuture(key)
    }

    fun removeFuture(key: String)
    {
        synchronized(scheduled) { scheduled.remove(key.toLowerCase()) }
    }

    fun getUsesFor(command: Command) : Int
    {
        synchronized(uses) { return uses.getOrDefault(command.name, 0) }
    }

    fun incrementUses(command: Command)
    {
        synchronized(uses) { uses.put(command.name, uses.getOrDefault(command.name, 0)+1) }
    }

    //////////////////////
    //      EVENTS      //
    //////////////////////

    override fun onEvent(event: Event?) = when(event)
    {
        is ReadyEvent           -> onReady(event)
        is GuildJoinEvent       -> onGuildJoin(event)
        is GuildLeaveEvent      -> onGuildLeave(event)
        is MessageReceivedEvent -> onMessageReceived(event)
        is MessageDeleteEvent   -> onMessageDelete(event)
        is ShutdownEvent        -> onShutdown(event)
        is GuildMemberJoinEvent -> onGuildMemberJoin(event)

        else -> Unit
    }

    fun onReady(event: ReadyEvent)
    {
        event.jda.addEventListener(waiter, DatabaseListener(manager, executor), AutoLoggingListener(manager, logger))
        event.jda.presence.status = OnlineStatus.ONLINE
        event.jda.presence.game = Game.of("Type ${prefix}help")

        Client.log.info("NightFury is Online!")

        val toLeave = event.jda.guilds.stream().filter { !it.isGood }.toList()
        if(toLeave.isNotEmpty())
        {
            toLeave.forEach { it.leave().queue() }

            Client.log.info("Left ${toLeave.size} bad guilds!")
        }

        // Clear Caches every hour
        executor.scheduleAtFixedRate({
            try {
                clearAPICaches()
                cleanCooldowns()
                cleanSchedule()
            } catch(e : Exception) {
                Client.log.info("Failed to clear caches!")
                Client log e
            }
        }, 0, 1, TimeUnit.HOURS)

        updateStats(event.jda)
    }

    fun onGuildJoin(event: GuildJoinEvent)
    {
        if(event.guild.selfMember.joinDate.plusMinutes(5).isAfter(OffsetDateTime.now()))
        {
            if(event.guild.isGood) updateStats(event.jda)
            else                   event.guild.leave().queue()
        }
    }

    fun onGuildLeave(event: GuildLeaveEvent)
    {
        updateStats(event.jda)
    }

    fun onMessageReceived(event: MessageReceivedEvent)
    {
        if(event.author.isBot)
            return
        val rawContent = event.message.rawContent.trim()
        val parts: List<String>
        val prefixUsed: String

        when
        {
            rawContent.startsWith(prefix, true) -> // From Anywhere with default prefix
            {
                prefixUsed = prefix
                parts = rawContent.substring(prefixUsed.length).trim().split(Regex("\\s+"), 2)
            }

            event.guild != null -> // From Guild without default prefix
            {
                val prefixes = manager.getPrefixes(event.guild)
                if(prefixes.isNotEmpty()) {
                    val prefix = prefixes.stream().filter { rawContent.startsWith(it, true) }?.findFirst()
                    if(prefix!=null && prefix.isPresent) {
                        prefixUsed = prefix.get()
                        parts = rawContent.substring(prefixUsed.length).trim().split(Regex("\\s+"), 2)
                    } else return
                } else return
            }

            else -> return // No match, not a command call
        }

        val name : String = parts[0]
        val args : String = if(parts.size == 2) parts[1] else ""
        if(listener.checkCall(event, this, name, args))
        {
            val command = commands[name]
            val commandEvent = CommandEvent(event,args.trim(),this,prefixUsed)
            if(command != null)
            {
                listener.onCommandCall(commandEvent, command)
                return command.run(commandEvent)
            }
            val customCommandContent = manager.customCommands.getContentFor(name,event.guild)
            if(event.isFromType(ChannelType.TEXT) && customCommandContent.isNotEmpty())
            {
                return commandEvent.reply(parser.clear().put("user", event.author)
                        .put("guild", event.guild)
                        .put("channel", event.textChannel)
                        .put("args", args)
                        .parse(customCommandContent))
            }
        }
    }

    fun onMessageDelete(event: MessageDeleteEvent)
    {
        if(!event.isFromType(ChannelType.TEXT))
            return
        synchronized(callCache)
        {
            if(callCache.contains(event.messageIdLong))
            {
                val messages = callCache[event.messageIdLong]?:return
                if(messages.size > 1 && event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_MANAGE))
                    event.textChannel.deleteMessages(messages).queue({},{})
                else if(messages.size == 1)
                    messages.forEach { it.delete().queue({},{}) }
            }
        }
    }

    fun onShutdown(event: ShutdownEvent)
    {
        val identifier = if(event.jda.shardInfo != null) "Shard: ${event.jda.shardInfo.shardString}" else "JDA"
        log.info("$identifier has shutdown.")
        executor.shutdown()
        manager.shutdown()
    }

    fun onGuildMemberJoin(event: GuildMemberJoinEvent)
    {
        // If there's no welcome channel then we just return.
        val welcomeChannel = manager getWelcomeChannel event.guild ?:return

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
                .parse(manager getWelcomeMessage event.guild)

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
            else {
                val toStore = HashSet<Message>()
                toStore.add(message)
                callCache.add(id, toStore)
            }
        }
    }

    /////////////////////
    // PRIVATE MEMBERS //
    /////////////////////

    private inline val Guild.isGood : Boolean
        inline get() = members.stream().filter { it.user.isBot }.count()<=30 || getMemberById(devId)!=null

    private inline fun <reified I> Iterable<*>.containsInstance() : Boolean
    {
        this.forEach { if (it is I) return true }
        return false
    }

    private fun cleanSchedule()
    {
        synchronized(scheduled)
        {
            scheduled.keys.stream().filter { scheduled[it]!!.isDone || scheduled[it]!!.isCancelled }
                    .toList().forEach { scheduled.remove(it) }
        }
    }

    private fun clearAPICaches()
    {
        commands.stream().filter {
            it::class.annotations.containsInstance<APICache>()
        }.forEach { cmd ->
            cmd::class.functions.stream().filter { it.annotations.containsInstance<APICache>() }.forEach { it.call(cmd) }
        }
    }

    private fun updateStats(jda: JDA)
    {
        val client = (jda as JDAImpl).httpClientBuilder.build()
        val body = JSONObject().put("server_count", jda.guilds.size)

        if(jda.shardInfo != null)
            body.put("shard_id", jda.shardInfo.shardId).put("shard_count", jda.shardInfo.shardTotal)

        val builder = Request.Builder().post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
                .url("https://bots.discord.pw/api/bots/" + jda.selfUser.id + "/stats")
                .header("Authorization", dBotsKey)
                .header("Content-Type", "application/json")

        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = response.close()

            override fun onFailure(call: Call, e: IOException) {
                Client.log.fatal("Failed to send information to bots.discord.pw")
                Client log e
            }
        })

        if(jda.shardInfo == null)
        {
            totalGuilds = jda.guilds.size
            return
        }

        try {
            client.newCall(Request.Builder().get()
                    .url("https://bots.discord.pw/api/bots/" + jda.selfUser.id + "/stats")
                    .header("Authorization", dBotsKey).header("Content-Type", "application/json").build())
                    .execute().body()!!.charStream().use {
                val array = JSONObject(JSONTokener(it)).getJSONArray("stats")
                var total = 0
                array.forEach { total += (it as JSONObject).getInt("server_count") }
                this.totalGuilds = total
            }
        } catch (e: Exception) {
            Client.log.fatal("Failed to retrieve bot shard information from bots.discord.pw")
            Client log e
        }
    }
}