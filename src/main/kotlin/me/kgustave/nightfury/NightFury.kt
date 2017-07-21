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

import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.api.GoogleAPI
import me.kgustave.nightfury.commands.*
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.listeners.command.DebugListener
import me.kgustave.nightfury.listeners.command.IdleListener
import me.kgustave.nightfury.listeners.command.StandardListener
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.utils.SimpleLog
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author Kaidan Gustave
 */
fun main(args: Array<String>)
{
    NightFury.LOG.info("Starting NightFury...")
    NightFury()
}

internal val executor : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

class NightFury
{
    companion object {
        val LOG: SimpleLog = SimpleLog.getLog("NightFury")
        fun shutdown(exit: Int)
        {
            executor.shutdown()
            LOG.info("Shutdown Complete! "+if(exit == 0)"Restarting..." else "Exiting...")
            System.exit(exit)
        }
    }

    init {
        val jda : JDABuilder = JDABuilder(AccountType.BOT)

        val config = Config(Paths.get(System.getProperty("user.dir"), "config.txt").toFile())

        Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").level = Level.OFF

        val manager = DatabaseManager(config.dbURL, config.dbUser, config.dbPass)

        val waiter = EventWaiter()
        val client = Client(config.prefix, config.ownerId, manager, config.success,
                config.warning, config.error, config.server, config.dbotskey, waiter)

        val google = GoogleAPI()

        jda.addEventListener(client)
        jda.setToken(config.token).setStatus(OnlineStatus.DO_NOT_DISTURB).setGame(Game.of("Starting Up..."))

        jda.buildAsync()

        client.addCommands(
                ColorMeCmd(),
                GoogleCmd(google),
                HelpCmd(),
                InfoCmd(),
                InviteCmd(*config.permissions),
                PingCmd(),
                PrefixCmd(),
                RoleMeCmd(),
                ServerCmd(waiter),

                BanCmd(),
                CleanCmd(),
                KickCmd(),
                MuteCmd(),
                SettingsCmd(),
                UnmuteCmd(),

                ModeratorCmd(),

                EvalCmd(),
                RestartCmd(),
                ShutdownCmd()
        )

        client.addListener(StandardListener.name, StandardListener())
        client.addListener(IdleListener.name, IdleListener())
        client.addListener(DebugListener.name, DebugListener())

        client.targetListener(StandardListener.name)

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
    internal val prefix: String = "||"
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