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
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import me.kgustave.nightfury.annotations.APICache
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.entities.ModLogger
import me.kgustave.nightfury.listeners.AutoLoggingListener
import me.kgustave.nightfury.listeners.DatabaseListener
import me.kgustave.nightfury.listeners.command.*
import me.kgustave.nightfury.resources.FixedSizeCache
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.SimpleLog
import org.json.JSONException
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.full.functions
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class Client internal constructor
(val prefix: String, val ownerID: Long, val manager: DatabaseManager,
 val success: String, val warning: String, val error: String,
 val server: String, val dBotsKey : String, val waiter: EventWaiter,
val parser: Parser,
 vararg commands: Command) : ListenerAdapter()
{
    // TODO Remove all occurrences of @Suppress("unused")
    private val executor : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val commandIndex : HashMap<String, Int> = HashMap()
    private val cooldowns : HashMap<String, OffsetDateTime> = HashMap()
    private val scheduled : HashMap<String, ScheduledFuture<*>> = HashMap()
    private val uses : HashMap<String, Int> = HashMap()
    private val linkedCache : FixedSizeCache<Long, HashSet<Message>> = FixedSizeCache(300)
    private val listeners : HashMap<String, CommandListener> = HashMap()
    internal var listener : CommandListener

    val commands : ArrayList<Command> = ArrayList()
    val startTime : OffsetDateTime = OffsetDateTime.now()
    val logger : ModLogger = ModLogger(manager)
    val messageCacheSize : Int
        get() {return linkedCache.size}

    var totalGuilds : Int = 0
        private set

    init {
        listeners.put(StandardListener.name, StandardListener())
        listeners.put(IdleListener.name, IdleListener())
        listeners.put(DebugListener.name, DebugListener())
        listener = listeners[StandardListener.name]!!
        addCommands(*commands)
    }

    companion object
    {
        private val LOG = SimpleLog.getLog("Client")
    }

    fun addCommand(command: Command)
    {
        addCommand(command, commands.size)
    }

    fun addCommand(command: Command, index: Int)
    {
        if(index > commands.size || index < 0)
            throw ArrayIndexOutOfBoundsException("Index specified is invalid: [" + index + "/" + commands.size + "]")
        val name = command.name.toLowerCase()
        if(commandIndex.containsKey(name))
            throw IllegalArgumentException("Command added has a name or alias that has already been indexed: \"$name\"!")
        for(alias in command.aliases)
        {
            if(commandIndex.containsKey(alias.toLowerCase()))
                throw IllegalArgumentException("Command added has a name or alias that has already been indexed: \"$alias\"!")
            commandIndex.put(alias.toLowerCase(), index)
        }
        commandIndex.put(name, index)
        if(index < commands.size)
        {
            commandIndex.keys.stream()
                    .filter({ key -> commandIndex[key]!! > index })
                    .collect(Collectors.toList<String>())
                    .forEach { key -> commandIndex.put(key, commandIndex[key]!! + 1) }
        }
        commands.add(command)
    }

    fun addCommands(vararg commands: Command)
    {
        commands.forEach { addCommand(it) }
    }

    @Suppress("unused")
    fun removeCommand(cmdRef: String)
    {
        val name = cmdRef.toLowerCase()
        if(!commandIndex.containsKey(name))
            throw IllegalArgumentException("Name provided is not indexed: \"$name\"!")
        val targetIndex = commandIndex.remove(name)!!
        if(commandIndex.containsValue(targetIndex))
        {
            commandIndex.keys.stream()
                    .filter { key -> commandIndex[key] == targetIndex }
                    .collect(Collectors.toList<String>())
                    .forEach { key -> commandIndex.remove(key) }
        }
        commandIndex.keys.stream()
                .filter { key -> commandIndex[key]!! > targetIndex }
                .collect(Collectors.toList<String>())
                .forEach { key -> commandIndex.put(key, commandIndex[key]!! - 1) }
        commands.removeAt(targetIndex)
    }

    fun getCommandByName(name: String) : Command?
    {
        val index = synchronized(commandIndex) { commandIndex.getOrDefault(name.toLowerCase(), -1) }
        if(index!=-1)
            return synchronized(commands) { commands[index] }
        else return null
    }

    fun targetListener(name: String)
    {
        if(!listeners.containsKey(name.toLowerCase()))
            throw IllegalArgumentException("Name provided has not been registered!")
        else {
            listener = listeners[name.toLowerCase()]!!
        }
    }

    @Suppress("unused")
    fun getCooldown(name: String): OffsetDateTime?
    {
        return cooldowns[name]
    }

    fun getRemainingCooldown(name: String): Int
    {
        if(cooldowns.containsKey(name)) {
            val time = OffsetDateTime.now().until(cooldowns[name], ChronoUnit.SECONDS).toInt()
            if (time <= 0) {
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
        cooldowns.keys.stream()
                .filter { str -> cooldowns[str]!!.isBefore(now) }
                .collect(Collectors.toList<String>()).stream().forEach { str -> cooldowns.remove(str) }
    }

    @Suppress("unused")
    fun hasFuture(key: String) : Boolean
    {
        synchronized(scheduled) { return scheduled.containsKey(key.toLowerCase()) }
    }

    @Suppress("unused")
    fun saveFuture(key: String, future: ScheduledFuture<*>)
    {
        synchronized(scheduled) { scheduled.put(key.toLowerCase(),future) }
    }

    @Suppress("unused")
    fun cancelFuture(key: String)
    {
        synchronized(scheduled) { scheduled[key.toLowerCase()]!!.cancel(false) }
        removeFuture(key)
    }

    fun removeFuture(key: String)
    {
        synchronized(scheduled) { scheduled.remove(key.toLowerCase()) }
    }

    fun cleanSchedule()
    {
        synchronized(scheduled)
        {
            scheduled.keys.stream()
                    .filter { key -> scheduled[key]!!.isDone || scheduled[key]!!.isCancelled }
                    .collect(Collectors.toSet())
                    .forEach { key -> scheduled.remove(key) }
        }
    }

    @Suppress("unused")
    fun getUsesFor(command: Command) = synchronized(uses) { uses.getOrDefault(command.name, 0) }

    fun incrementUses(command: Command) = synchronized(uses) { uses.put(command.name, uses.getOrDefault(command.name, 0)+1) }

    fun clearAPICaches()
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
            val stored = linkedCache.get(id)
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
        event.jda.addEventListener(
                waiter,
                DatabaseListener(manager, executor),
                AutoLoggingListener(manager, logger)
        )
        event.jda.presence.status = OnlineStatus.ONLINE
        event.jda.presence.game = Game.of("Type ${prefix}help")
        LOG.info("NightFury is Online!")
        val toLeave = event.jda.guilds.stream().filter { !testGuild(it) }.toList()
        if(toLeave.isNotEmpty()) {
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
            if(testGuild(event.guild)) updateStats(event.jda)
            else                       event.guild.leave().queue()
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
        var parts: Array<String?>? = null
        var prefixUsed: String = prefix
        when {
            rawContent.startsWith(prefix, true) ->
                parts = Arrays.copyOf(rawContent.substring(prefixUsed.length).trim()
                        .split(Regex("\\s+"), 2).toTypedArray(),2)
            else -> {
                val prefixes = manager.getPrefixes(event.guild)
                if(!prefixes.isEmpty())
                {
                    val prefix = prefixes.stream().filter { rawContent.startsWith(it, true) }?.findFirst()
                    if(prefix!=null && prefix.isPresent) {
                        prefixUsed = prefix.get()
                        parts = Arrays.copyOf(rawContent.substring(prefixUsed.length).trim()
                                .split(Regex("\\s+"), 2).toTypedArray(), 2)
                    }
                }
            }
        }
        if(parts != null)
        {
            val name : String = parts[0] as String
            val args : String = parts[1]?:""
            if(listener.checkCall(event, this, name, args))
            {
                val command = getCommandByName(name)
                if(command != null)
                {
                    val commandEvent = CommandEvent(event.jda,event.responseNumber,event.message,args.trim(),this,prefixUsed)
                    listener.onCommandCall(commandEvent, command)
                    return command.run(commandEvent)
                }
                val customCommandContent = manager.customCommands.getContentFor(name,event.guild)
                if(event.isFromType(ChannelType.TEXT) && customCommandContent.isNotEmpty())
                {
                    val messages = CommandEvent.processMessage(parser.put("user", event.author)
                            .put("guild", event.guild)
                            .put("channel", event.textChannel)
                            .put("args", args)
                            .parse(customCommandContent)).toTypedArray()
                    messages.forEachIndexed { i, msg ->
                        if(i<2) event.textChannel.sendMessage(msg).queue { linkIds(event.messageIdLong, it) }
                    }
                    parser.clear()
                }
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
                val messages = linkedCache.get(event.messageIdLong)!!
                if(messages.size > 1 && event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_MANAGE))
                    event.textChannel.deleteMessages(messages).queue({},{})
                else if(messages.size == 1)
                    messages.forEach { m -> m.delete().queue({},{}) }
            }
        }
    }

    override fun onShutdown(event: ShutdownEvent)
    {
        executor.shutdown()
        manager.shutdown()
    }

    private fun testGuild(guild: Guild) : Boolean
    {
        val bots = guild.members.stream().filter { it.user.isBot }.count()
        return bots<=30 || guild.getMemberById(ownerID)!=null
    }

    private fun updateStats(jda: JDA)
    {
        if(jda.shardInfo == null)
        {
            Unirest.post("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
                    .header("Authorization", dBotsKey)
                    .header("Content-Type","application/json")
                    .body(JSONObject().put("server_count", jda.guilds.size).toString())
                    .asJsonAsync()
        }
        else
        {
            Unirest.post("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
                    .header("Authorization", dBotsKey)
                    .header("Content-Type","application/json")
                    .body(JSONObject()
                            .put("shard_id", jda.shardInfo.shardId)
                            .put("shard_count", jda.shardInfo.shardTotal)
                            .put("server_count", jda.guilds.size)
                            .toString())
                    .asJsonAsync()
            try {
                val array = Unirest.get("https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats")
                        .header("Authorization", dBotsKey)
                        .header("Content-Type", "application/json")
                        .asJson().body.`object`.getJSONArray("stats")
                var total = 0
                array.forEach { obj : Any -> total += (obj as JSONObject).getInt("server_count") }
                totalGuilds = total
            } catch (ex: UnirestException) {
                LOG.warn("Failed to retrieve bot shard information from bots.discord.pw")
            } catch (ex: JSONException) {
                LOG.warn("Failed to retrieve bot shard information from bots.discord.pw")
            }

        }
    }
}