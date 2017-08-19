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
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
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
 val parser: Parser, vararg commands: Command) : ListenerAdapter()
{
    val commands: CommandMap = CommandMap(*commands)
    val startTime : OffsetDateTime = OffsetDateTime.now()
    val logger : ModLogger = ModLogger(manager)
    val messageCacheSize : Int
        get() = linkedCache.size

    var totalGuilds : Int = 0
        private set

    private val executor : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val cooldowns : HashMap<String, OffsetDateTime> = HashMap()
    private val scheduled : HashMap<String, ScheduledFuture<*>> = HashMap()
    private val uses : HashMap<String, Int> = HashMap()
    private val linkedCache : FixedSizeCache<Long, HashSet<Message>> = FixedSizeCache(300)
    private val listeners : Array<CommandListener> = arrayOf(
            StandardListener(),
            IdleListener(),
            DebugListener()
    )

    internal var listener : CommandListener

    init {
        listener = listeners[0]
    }

    companion object
    {
        private val LOG = SimpleLog.getLog("Client")
    }

    fun targetListener(name: String)
    {
        val l = listeners.toList().filter { it.name == name.toLowerCase() }
        if(l.isNotEmpty()) listener = l[0]
    }

    fun getRemainingCooldown(name: String): Int
    {
        if(cooldowns.containsKey(name)) {
            val time = OffsetDateTime.now().until(cooldowns[name], ChronoUnit.SECONDS).toInt()
            if(time <= 0) {
                cooldowns.remove(name)
                return 0
            }
            return time
        }
        return 0
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
    fun hasFuture(key: String) = synchronized(scheduled) { scheduled.containsKey(key.toLowerCase()) }

    @Suppress("unused")
    fun saveFuture(key: String, future: ScheduledFuture<*>)
    {
        synchronized(scheduled) { scheduled.put(key.toLowerCase(),future) }
    }

    @Suppress("unused")
    fun cancelFuture(key: String)
    {
        synchronized(scheduled) { scheduled[key.toLowerCase()]?.cancel(false) }
        removeFuture(key)
    }

    @Suppress("unused")
    fun removeFuture(key: String)
    {
        synchronized(scheduled) { scheduled.remove(key.toLowerCase()) }
    }

    private fun cleanSchedule()
    {
        synchronized(scheduled)
        {
            scheduled.keys.stream().filter { scheduled[it]!!.isDone || scheduled[it]!!.isCancelled }
                    .toList().forEach { scheduled.remove(it) }
        }
    }

    @Suppress("unused")
    fun getUsesFor(command: Command) = synchronized(uses) { uses.getOrDefault(command.name, 0) }

    fun incrementUses(command: Command) = synchronized(uses) { uses.put(command.name, uses.getOrDefault(command.name, 0)+1) }

    private fun clearAPICaches()
    {
        commands.stream().filter {
            it::class.annotations.filterIsInstance<APICache>().isNotEmpty()
        }.forEach { cmd ->
            cmd::class.functions.filter {
                it.annotations.filterIsInstance<APICache>().isNotEmpty()
            }.forEach { it.call(cmd) }
        }
    }

    internal fun linkIds(id: Long, message: Message)
    {
        synchronized(linkedCache)
        {
            val stored = linkedCache[id]
            if(stored != null)
                stored.add(message)
            else {
                val toStore = HashSet<Message>()
                toStore.add(message)
                linkedCache.add(id, toStore)
            }
        }
    }

    override fun onReady(event: ReadyEvent)
    {
        event.jda.addEventListener(waiter, DatabaseListener(manager, executor), AutoLoggingListener(manager, logger))
        event.jda.presence.status = OnlineStatus.ONLINE
        event.jda.presence.game = Game.of("Type ${prefix}help")

        LOG.info("NightFury is Online!")

        val toLeave = event.jda.guilds.stream().filter { !it.isGood }.toList()
        if(toLeave.isNotEmpty())
        {
            toLeave.forEach { it.leave().queue() }

            LOG.info("Left ${toLeave.size} bad guilds!")
        }

        // Clear API Caches every hour
        executor.scheduleAtFixedRate({
            clearAPICaches()
            cleanCooldowns()
            cleanSchedule()
        }, 0, 1, TimeUnit.HOURS)

        updateStats(event.jda)
    }

    override fun onGuildJoin(event: GuildJoinEvent)
    {
        if(event.guild.selfMember.joinDate.plusMinutes(5).isAfter(OffsetDateTime.now()))
        {
            if(event.guild.isGood) updateStats(event.jda)
            else                   event.guild.leave().queue()
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent)
    {
        updateStats(event.jda)
    }

    override fun onMessageReceived(event: MessageReceivedEvent)
    {
        if(event.author.isBot)
            return
        val rawContent = event.message.rawContent
        val parts: List<String>
        val prefixUsed: String

        when
        {
            rawContent.startsWith(prefix, true) -> // From Anywhere with default prefix
            {
                prefixUsed = prefix
                parts = rawContent.substring(prefix.length).trim().split(Regex("\\s+"), 2)
            }

            event.guild != null -> // From Guild without default prefix
            {
                val prefixes = manager.getPrefixes(event.guild)
                if(!prefixes.isEmpty()) {
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
                commandEvent.reply(parser.put("user", event.author)
                        .put("guild", event.guild)
                        .put("channel", event.textChannel)
                        .put("args", args)
                        .parse(customCommandContent))
                parser.clear()
            }
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent)
    {
        if(!event.isFromType(ChannelType.TEXT))
            return
        synchronized(linkedCache)
        {
            if(linkedCache.contains(event.messageIdLong))
            {
                val messages = linkedCache[event.messageIdLong]?:return
                if(messages.size > 1 && event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_MANAGE))
                    event.textChannel.deleteMessages(messages).queue({},{})
                else if(messages.size == 1)
                    messages.forEach { it.delete().queue({},{}) }
            }
        }
    }

    // Shutdown
    override fun onShutdown(event: ShutdownEvent)
    {
        executor.shutdown()
        manager.shutdown()
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent)
    {
        val welcomeChannel = manager.getWelcomeChannel(event.guild)?:return
        val message = parser.clear()
                .put("guild", event.guild)
                .put("channel", welcomeChannel)
                .put("user", event.user)
                .parse(manager.getWelcomeMessage(event.guild))
        welcomeChannel.sendMessage(message).queue()
    }

    private val Guild.isGood : Boolean
        get() {
            val bots = this.members.stream().filter { it.user.isBot }.count()
            return bots<=30 || this.getMemberById(devId)!=null
        }

    private fun updateStats(jda: JDA)
    {
        val log = SimpleLog.getLog("BotList")
        val client = (jda as JDAImpl).httpClientBuilder.build()
        val body = JSONObject().put("server_count", jda.getGuilds().size)

        if (jda.getShardInfo() != null) body.put("shard_id", jda.getShardInfo().shardId).put("shard_count", jda.getShardInfo().shardTotal)

        val builder = Request.Builder().post(RequestBody.create(Requester.MEDIA_TYPE_JSON, body.toString()))
                .url("https://bots.discord.pw/api/bots/" + jda.getSelfUser().id + "/stats")
                .header("Authorization", dBotsKey)
                .header("Content-Type", "application/json")

        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = response.close()

            override fun onFailure(call: Call, e: IOException) {
                log.fatal("Failed to send information to bots.discord.pw")
                log.log(e)
            }
        })

        try {
            client.newCall(Request.Builder().get()
                    .url("https://bots.discord.pw/api/bots/" + jda.getSelfUser().id + "/stats")
                    .header("Authorization", dBotsKey).header("Content-Type", "application/json").build())
                    .execute().body()!!.charStream().use {
                val array = JSONObject(JSONTokener(it)).getJSONArray("stats")
                var total = 0
                array.forEach { total += (it as JSONObject).getInt("server_count") }
                this.totalGuilds = total
            }
        } catch (e: Exception) {
            log.fatal("Failed to retrieve bot shard information from bots.discord.pw")
            // Dbots is broken when I'm pushing this, hopefully next push it'll be fixed
            // log.log(e)
        }
    }
}