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

import club.minnced.kjda.events.AsyncEventManager
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.api.GoogleAPI
import me.kgustave.nightfury.commands.admin.*
import me.kgustave.nightfury.commands.moderator.*
import me.kgustave.nightfury.commands.dev.*
import me.kgustave.nightfury.commands.other.*
import me.kgustave.nightfury.commands.standard.*
import me.kgustave.nightfury.db.DatabaseManager
import me.monitor.je621.JE621Builder
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.utils.SimpleLog
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.sql.SQLException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

fun main(args: Array<String>?)
{
    NightFury.LOG.info("Starting NightFury...")
    NightFury(if(args==null || args.isEmpty()) emptyArray<String>() else args)
    Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").level = Level.OFF
}

/**
 * @author Kaidan Gustave
 */
class NightFury(args: Array<String>)
{
    companion object {
        private val executor : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        val LOG: SimpleLog = SimpleLog.getLog("NightFury")
        fun shutdown(exit: Int)
        {
            executor.shutdown()
            LOG.info("Shutdown Complete! "+if(exit == 0)"Restarting..." else "Exiting...")
            System.exit(exit)
        }
    }

    init {
        val jda : JDABuilder = JDABuilder(AccountType.BOT).setEventManager(AsyncEventManager())

        val config = Config(Paths.get(System.getProperty("user.dir"), "config.txt").toFile())

        val manager = DatabaseManager(config.dbURL, config.dbUser, config.dbPass)
        if(args.isNotEmpty())
        {
            args.forEach {
                if(it == "-setupdb")
                    if(!manager.startup())
                        throw SQLException("Failed to setup database!")
            }
        }
        val waiter = EventWaiter()

        val google = GoogleAPI()
        val e621 = JE621Builder("NightFury").build()

        val client = Client(
                config.prefix, config.ownerId, manager,
                config.success, config.warning, config.error,
                config.server, config.dbotskey, waiter,
                ColorMeCmd(),
                GoogleCmd(google),
                HelpCmd(),
                InfoCmd(),
                InviteCmd(*config.permissions),
                PingCmd(),
                PrefixCmd(),
                RoleMeCmd(),
                ServerCmd(waiter),
                TagCommand(),

                E621Cmd(e621, waiter),

                BanCmd(),
                KickCmd(),
                MuteCmd(),
                ReasonCmd(),
                SettingsCmd(),
                UnmuteCmd(),

                ModeratorCmd(),
                ModLogCmd(),

                EvalCmd(),
                ModeCmd(),
                RestartCmd(),
                ShutdownCmd()
        )

        jda.addEventListener(client)
                .setToken(config.token)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.of("Starting Up..."))
                .buildAsync()

        executor.scheduleAtFixedRate({
            client.cleanCooldowns()
            client.cleanSchedule()
            google.clearCache()
        }, 10, 10, TimeUnit.MINUTES)
    }
}

internal class Config(key: File)
{
    private val tokens : List<String> = try {
        key.readLines()
    } catch (e: IOException) {
        throw ConfigException(e)
    }

    internal val token : String = tokens[0]
    internal val ownerId : Long = tokens[1].toLong()
    internal val dbotskey : String = tokens[2]
    internal val dbURL: String = tokens[3]
    internal val dbUser: String = tokens[4]
    internal val dbPass: String = tokens[5]
    internal val prefix: String = "|"
    internal val success: String = "\uD83D\uDC32"
    internal val warning: String = "\uD83D\uDC22"
    internal val error: String = "\uD83D\uDD25"
    internal val server: String = "https://discord.gg/xkkw54u"
    internal val permissions: Array<Permission> = arrayOf(
            Permission.BAN_MEMBERS,
            Permission.KICK_MEMBERS,
            Permission.MESSAGE_MANAGE,
            Permission.MANAGE_ROLES,
            Permission.MESSAGE_ADD_REACTION,
            Permission.VOICE_MOVE_OTHERS,
            Permission.VOICE_MUTE_OTHERS,
            Permission.MESSAGE_EMBED_LINKS
    )
    //internal val description: String = "multipurpose discord bot"
    /*internal val features: Array<String> = arrayOf(
            "Moderation",
            "Logs",
            "Utility"
    )*/
    //internal val color: Color = Color.BLACK
}

internal class ConfigException(ioe: IOException) : Exception(ioe)