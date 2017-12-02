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

import com.jagrosh.jagtag.JagTag
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.requests.SessionReconnectQueue
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.nightfury.api.E621API
import xyz.nightfury.api.GoogleAPI
import xyz.nightfury.api.GoogleImageAPI
import xyz.nightfury.api.YouTubeAPI
import xyz.nightfury.commands.admin.*
import xyz.nightfury.commands.dev.*
import xyz.nightfury.commands.moderator.*
import xyz.nightfury.commands.music.*
import xyz.nightfury.commands.other.E621Cmd
import xyz.nightfury.commands.standard.*
import xyz.nightfury.db.Database
import xyz.nightfury.entities.menus.EventWaiter
import xyz.nightfury.extensions.*
import xyz.nightfury.jagtag.tagMethods
import xyz.nightfury.listeners.AutoLoggingListener
import xyz.nightfury.listeners.DatabaseListener
import xyz.nightfury.listeners.InvisibleTracker
import xyz.nightfury.listeners.StarboardListener
import xyz.nightfury.music.MusicManager
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.logging.Level

fun main(args: Array<String>?) {
    NightFury.LOG.info("Starting NightFury...")

    java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").level = Level.OFF

    try {
        NightFury()
    } catch(e: IOException) {
        NightFury.LOG.error("Failed to get configurations!",e)
    } catch(e: Exception) {
        NightFury.LOG.error("An error occurred!",e)
    }
}

/**
 * @author Kaidan Gustave
 */
class NightFury {
    companion object {
        val VERSION: String = this::class.java.`package`.implementationVersion ?: "BETA"
        val GITHUB: String = "https://github.com/NightFuryBot/NightFury/"
        val LOG: Logger = LoggerFactory.getLogger("NightFury")

        fun shutdown(exit: Int) {
            LOG.info("Shutdown Complete! "+if(exit == 0)"Restarting..." else "Exiting...")
            System.exit(exit)
        }
    }

    init {
        val config = Config()

        // Make initial connection
        Database.connect(config.dbURL, config.dbUser, config.dbPass)

        val e621 = E621API()
        val google = GoogleAPI()
        val image = GoogleImageAPI()
        val yt = YouTubeAPI(config.ytApiKey)

        val parser = JagTag.newDefaultBuilder().addMethods(tagMethods).build()

        val waiter = EventWaiter()
        val invisTracker = InvisibleTracker()
        val musicManager = MusicManager()

        val client = Client(
            config.prefix, config.devId,
            config.success, config.warning, config.error,
            config.server, config.dbotsKey, config.dborgKey,
            waiter, parser,

            AboutCmd(*config.permissions),
            AvatarCmd(),
            ColorMeCmd(),
            EmoteCmd(),
            GoogleCmd(google),
            HelpCmd(),
            ImageCmd(image),
            InfoCmd(invisTracker),
            InviteCmd(*config.permissions),
            PingCmd(),
            ProfileCmd(),
            QuoteCmd(),
            RoleMeCmd(waiter),
            ServerCmd(waiter, invisTracker),
            TagCommand(waiter),
            YouTubeCmd(yt),

            PlayCmd(musicManager),
            QueueCmd(waiter, musicManager),
            RemoveCmd(musicManager),
            SkipCmd(musicManager),
            StopCmd(musicManager),

            E621Cmd(e621, waiter),

            BanCmd(),
            CleanCmd(),
            KickCmd(),
            MuteCmd(),
            ReasonCmd(),
            SettingsCmd(),
            UnbanCmd(),
            UnmuteCmd(),

            AnnounceCmd(),
            CustomCommandCmd(waiter),
            ModeratorCmd(),
            ModLogCmd(),
            PrefixCmd(),
            StarboardCmd(),
            WelcomeCmd(),
            LevelCmd(),
            ToggleCmd(),

            BashCmd(),
            EvalCmd(musicManager),
            GuildlistCmd(waiter),
            MemoryCmd(),
            ModeCmd(),
            RestartCmd(),
            ShutdownCmd(),
            WhitelistCmd(musicManager))

        JDABuilder(AccountType.BOT) buildAsync {
            manager  { AsyncEventManager() }
            listener { client }
            listener { invisTracker }
            listener { musicManager }
            listener { StarboardListener() }
            listener { waiter }
            listener { AutoLoggingListener() }
            listener { DatabaseListener() }
            token    { config.token }
            status   { OnlineStatus.DO_NOT_DISTURB }
            game     { "Starting Up..." }
        }
    }

    private infix inline fun <reified T: JDABuilder> T.buildAsync(lazy: JDABuilder.() -> Unit) {
        lazy()
        buildAsync()
    }

    @Suppress("UNUSED")
    private inline fun <reified T: JDABuilder> T.buildAsync(shards: Int, lazy: JDABuilder.() -> Unit) {
        lazy()
        setShardedRateLimiter(ShardedRateLimiter())
        setReconnectQueue(SessionReconnectQueue())
        for(i in 0 until shards) {
            useSharding(i, shards)
            buildAsync()
            LOG.info("Shard [$i / ${shards - 1}] now building...")
            Thread.sleep(5000) // Five second backoff
        }
    }
}

class Config {
    companion object {
        @JvmStatic var testing: Boolean = false
    }

    private val file: File = Paths.get(System.getProperty("user.dir"), "config.json").toFile()
    private val json: JSONObject = JSONObject(file.readText(Charsets.UTF_8))

    val token: String
        get() = (if(testing) json.getString("test_bot_token") else json.getString("bot_token")) ?: nulled()

    val devId: Long = 211393686628597761L

    val dbotsKey: String = json.getString("dbots_key") ?: nulled()

    val dborgKey: String = json.getString("discord_bots_list_key") ?: nulled()

    val dbURL: String
        get() = buildString {
            val db = json.getJSONObject("db") ?: nulled()
            append(db.getString("prefix") ?: nulled())

            if(testing)
                append(db.getString("test_url") ?: nulled())
            else
                append(db.getString("url") ?: nulled())

            for((key, value) in db.getJSONObject("configurations")?.toMap() ?: nulled()) {
                append(";")
                append(key.toUpperCase())
                append("=")
                append(value.toString().toUpperCase())
            }
        }

    val dbUser: String = json.getJSONObject("db")?.getString("user") ?: nulled()

    val dbPass: String = json.getJSONObject("db")?.getString("pass") ?: nulled()

    val ytApiKey: String = json.getString("yt_api_key") ?: nulled()

    val prefix: String
        get() = if(testing) "||" else "|"

    val success: String = "\uD83D\uDC32"
    val warning: String = "\uD83D\uDC22"
    val error: String = "\uD83D\uDD25"

    val server: String = "https://discord.gg/xkkw54u"

    val permissions: Array<Permission> = arrayOf(

        Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ATTACH_FILES,
        Permission.MESSAGE_ADD_REACTION,

        Permission.MANAGE_PERMISSIONS,
        Permission.MANAGE_ROLES,
        Permission.MANAGE_CHANNEL,
        Permission.NICKNAME_MANAGE,
        Permission.MESSAGE_MANAGE,

        Permission.KICK_MEMBERS,
        Permission.BAN_MEMBERS,

        Permission.VIEW_AUDIT_LOGS

    )

    // This prevents npe with native java code
    private inline fun <reified T> nulled(): T {
        throw IllegalStateException("${file.name} did not have a specified token!")
    }
}