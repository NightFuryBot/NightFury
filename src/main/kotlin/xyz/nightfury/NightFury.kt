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
@file:Suppress("MemberVisibilityCanBePrivate", "Unused")
package xyz.nightfury

import com.jagrosh.jagtag.JagTag
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.utils.SessionControllerAdapter
import okhttp3.OkHttpClient
import xyz.nightfury.command.administrator.*
import xyz.nightfury.command.moderator.*
import xyz.nightfury.command.music.*
import xyz.nightfury.command.owner.*
import xyz.nightfury.command.standard.*
import xyz.nightfury.entities.WebhookAppender
import xyz.nightfury.entities.tagMethods
import xyz.nightfury.listeners.*
import xyz.nightfury.music.MusicManager
import xyz.nightfury.ndb.Database
import xyz.nightfury.requests.GoogleAPI
import xyz.nightfury.requests.GoogleImageAPI
import xyz.nightfury.requests.YouTubeAPI
import xyz.nightfury.util.*
import xyz.nightfury.util.jda.*
import xyz.nightfury.util.reflect.packageOf

/**
 * @author Kaidan Gustave
 */
object NightFury {
    const val DEV_ID = 211393686628597761L
    const val SUCCESS = "\uD83D\uDC32"
    const val WARNING = "\uD83D\uDC22"
    const val ERROR = "\uD83D\uDD25"
    const val PREFIX = "|"
    const val TEST_PREFIX = "||"
    const val GITHUB = "https://github.com/NightFuryBot/NightFury/"
    const val SERVER_INVITE = "https://discord.gg/xkkw54u"

    val LOG = createLogger(NightFury::class)

    val packageInfo = packageOf(this::class)
    val version = packageInfo.version.implementation ?: "BETA"
    val httpClientBuilder = OkHttpClient.Builder()
    val permissions = arrayOf(
        MESSAGE_HISTORY,
        MESSAGE_EMBED_LINKS,
        MESSAGE_ATTACH_FILES,
        MESSAGE_ADD_REACTION,

        MANAGE_PERMISSIONS,
        MANAGE_ROLES,
        MANAGE_CHANNEL,
        NICKNAME_MANAGE,
        MESSAGE_MANAGE,

        KICK_MEMBERS,
        BAN_MEMBERS,

        VIEW_AUDIT_LOGS
    )

    @JvmStatic fun main(args: Array<String>) {
        start()
    }

    fun start(): Client {
        LOG.info("Starting...")

        val config = NightFury.Config()

        Database.connect(config.databaseURL, config.databaseUser, config.databasePass)

        if(!WebhookAppender.isInitialized) {
            LOG.debug("Webhook appender is not initialized.")
        }

        val google = GoogleAPI()
        val image = GoogleImageAPI()
        val yt = YouTubeAPI(config.ytApiKey)

        val parser = JagTag.newDefaultBuilder().addMethods(tagMethods).build()

        val waiter = EventWaiter()
        val musicManager = MusicManager()

        // Standard Commands
        AboutCommand()
        AvatarCommand()
        ColorMeCommand(waiter)
        EmoteCommand()
        GoogleCommand(google)
        HelpCommand()
        ImageCommand(image)
        InfoCommand()
        InviteCommand()
        PingCommand()
        RoleMeCommand(waiter)
        ServerCommand(waiter)
        TagCommand(waiter)
        YouTubeCommand(yt)

        // Music Commands
        PlayCommand(musicManager)
        RemoveCommand(musicManager)
        SkipCommand(musicManager)
        StopCommand(musicManager)
        VolumeCommand(musicManager)

        // Moderator Commands
        BanCommand()
        CleanCommand()
        KickCommand()
        MuteCommand(waiter)
        ReasonCommand()
        UnbanCommand()
        UnmuteCommand()

        // Administrator Commands
        CustomCmdCommand(waiter)
        PrefixCommand(waiter)
        LogCommand(waiter)
        LevelCommand()
        WelcomeCommand()

        // Owner Commands
        EvalCommand()
        GuildListCommand(waiter)
        MemoryCommand()
        RestartCommand()
        ShutdownCommand()

        val client = Client(if(config.test) TEST_PREFIX else PREFIX, config.dBotsKey, config.dBotsListKey, parser)

        JDABuilder(AccountType.BOT).buildAsync {
            manager    { ContextEventManager() }

            listener   { client }
            listener   { musicManager }
            listener   { waiter }
            listener   { StarboardListener() }
            listener   { ModLog }
            listener   { DatabaseListener() }

            contextMap { null }
            token      { config.token }
            status     { OnlineStatus.DO_NOT_DISTURB }
            watching   { "Everything Start Up..." }
        }

        return client
    }

    private inline fun <reified T: JDABuilder> T.buildAsync(lazy: JDABuilder.() -> Unit) {
        lazy()
        buildAsync()
    }

    @Suppress("UNUSED")
    private inline fun <reified T: JDABuilder> T.buildAsync(shards: Int, lazy: JDABuilder.() -> Unit) {
        lazy()
        sessionController { SessionControllerAdapter() }
        for(i in 0 until shards) {
            useSharding(i, shards)
            buildAsync()
            LOG.info("Shard [$i / ${shards - 1}] now building...")
            Thread.sleep(5000) // Five second backoff
        }
    }

    class Config {
        private val conf = hocon {
            setSource { this::class.resourceStreamOf("/bot.conf")?.bufferedReader(Charsets.UTF_8) }
            parseOptions.allowMissing = false
            renderOptions.comments = true
        }

        val bot = requireNotNull(conf.node("bot"))

        val token = requireNotNull(bot.getList("token"){it}?.joinToString(".")) { "Missing Token Node" }
        val test = bot["test"] ?: false

        val keys = requireNotNull(bot.node("keys"))

        val dBotsKey = keys.getList("dbots"){it}?.joinToString(".")
        val dBotsListKey = keys.getList("dbotslist"){it}?.joinToString(".")
        val ytApiKey: String? = keys["youtube"]

        val database = requireNotNull(conf.node("database")) { "Missing Database Node" }
        val login = requireNotNull(database.node("login")) { "Missing Database-Login Node" }

        val databaseUser: String = requireNotNull(login["user"]) { "Missing Database User" }
        val databasePass: String = requireNotNull(login["pass"]) { "Missing Database Pass" }
        val databaseURL: String = run {
            val url = requireNotNull(database.node("url")) { "Missing Database-URL Node" }
            val prefix = requireNotNull(url.get<String>("prefix")) { "Missing Database-URL-Prefix Node" }
            val path = requireNotNull(url.get<String>("path")) { "Missing Database-URL-Path Node" }
            val options = database.node("options")?.childrenMap ?: emptyMap()
            buildString {
                append("$prefix$path")
                options.forEach { key, value ->
                    append(";")
                    append(key)
                    append("=")
                    append(value.value.toString())
                }
            }
        }
    }
}
