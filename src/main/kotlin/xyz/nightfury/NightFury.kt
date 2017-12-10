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
import java.io.IOException

fun main(args: Array<String>?) {
    NightFury.LOG.info("Starting NightFury...")

    try {
        NightFury()
    } catch(e: IOException) {
        NightFury.LOG.error("Failed to get configurations!",e)
    } catch(e: Exception) {
        NightFury.LOG.error("An error occurred!",e)
    } catch(e: ConfigException) {
        NightFury.LOG.error("The bot.conf file was missing a value, or an error occur while reading!", e)
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
            LOG.info("Shutdown Complete! ${if(exit == 0) "Restarting..." else "Exiting..."}")
            System.exit(exit)
        }
    }

    init {
        val config = BotConfig()

        // Make initial connection
        Database.connect(config.databaseURL, config.databaseUser, config.databasePass)

        val e621 = E621API()
        val google = GoogleAPI()
        val image = GoogleImageAPI()
        val yt = YouTubeAPI(config.ytKey)

        val parser = JagTag.newDefaultBuilder().addMethods(tagMethods).build()

        val waiter = EventWaiter()
        val invisTracker = InvisibleTracker()
        val musicManager = MusicManager()

        val client = Client(
            config.prefix, config.devId,
            config.success, config.warning, config.error,
            config.server, config.dbotsKey, config.dbotslistKey,
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