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
import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.api.E621API
import me.kgustave.nightfury.api.GoogleAPI
import me.kgustave.nightfury.api.YouTubeAPI
import me.kgustave.nightfury.commands.admin.*
import me.kgustave.nightfury.commands.moderator.*
import me.kgustave.nightfury.commands.dev.*
import me.kgustave.nightfury.commands.other.*
import me.kgustave.nightfury.commands.standard.*
import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.jagtag.getMethods
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.utils.SimpleLog
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.sql.SQLException
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
        val LOG: SimpleLog = SimpleLog.getLog("NightFury")
        fun shutdown(exit: Int)
        {
            LOG.info("Shutdown Complete! "+if(exit == 0)"Restarting..." else "Exiting...")
            System.exit(exit)
        }

        @JvmStatic val version : String = "0.5.0"
        @JvmStatic val github : String = "https://github.com/TheMonitorLizard/NightFury/"
    }

    init {
        val jda : JDABuilder = JDABuilder(AccountType.BOT).setEventManager(AsyncEventManager())

        val config = Config(Paths.get(System.getProperty("user.dir"), "config.txt").toFile())
        val manager = DatabaseManager(config.dbURL, config.dbUser, config.dbPass)
        if(args.isNotEmpty())
        {
            args.forEach {
                if(it == "-setupDB"       && !manager.startup())             throw SQLException("Failed to setup database!")
                if(it == "-setupCases"    && !manager.createCasesTable())    throw SQLException("Failed to setup cases table!")
                if(it == "-setupChannels" && !manager.createChannelsTable()) throw SQLException("Failed to setup channels table!")
                if(it == "-setupPrefixes" && !manager.createPrefixesTable()) throw SQLException("Failed to setup prefixes table!")
                if(it == "-setupRoles"    && !manager.createRolesTable())    throw SQLException("Failed to setup roles table!")
                if(it == "-setupTags"     && !manager.createTagsTables())    throw SQLException("Failed to setup tags table!")
                if(it == "-setupCCs"      && !manager.createCommandsTable()) throw SQLException("Failed to setup custom commands table!")
            }
        }

        val waiter = EventWaiter()

        val google = GoogleAPI()
        val e621 = E621API()
        val yt = YouTubeAPI(config.ytApiKey)

        val parser : Parser = JagTag.newDefaultBuilder().addMethods(getMethods()).build()

        val client = Client(
                config.prefix, config.devId, manager,
                config.success, config.warning, config.error,
                config.server, config.dbotskey, waiter, parser,

                AboutCmd(*config.permissions),
                ColorMeCmd(),
                GoogleCmd(google),
                HelpCmd(),
                InfoCmd(),
                InviteCmd(*config.permissions),
                PingCmd(),
                RoleMeCmd(waiter),
                ServerCmd(waiter),
                TagCommand(waiter),
                YouTubeCmd(yt),

                E621Cmd(e621, waiter),

                BanCmd(),
                KickCmd(),
                MuteCmd(),
                ReasonCmd(),
                SettingsCmd(),
                UnmuteCmd(),

                CustomCommandCmd(waiter),
                ModeratorCmd(),
                ModLogCmd(),
                PrefixCmd(),

                EvalCmd(),
                MemoryCmd(),
                ModeCmd(),
                RestartCmd(),
                ShutdownCmd()
        )

        jda.addEventListener(client)
                .setToken(config.token)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.of("Starting Up..."))
                .buildAsync()
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
    internal val devId: Long = tokens[1].toLong()
    internal val dbotskey: String = tokens[2]
    internal val dbURL: String = tokens[3]
    internal val dbUser: String = tokens[4]
    internal val dbPass: String = tokens[5]
    internal val ytApiKey: String = tokens[6]
    internal val prefix: String = "|"
    internal val success: String = "\uD83D\uDC32"
    internal val warning: String = "\uD83D\uDC22"
    internal val error: String = "\uD83D\uDD25"
    internal val server: String = "https://discord.gg/xkkw54u"
    internal val permissions: Array<Permission> = arrayOf(
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
}

internal class ConfigException(ioe: IOException) : Exception(ioe)