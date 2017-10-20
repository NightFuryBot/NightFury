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
package me.kgustave.nightfury.db

import me.kgustave.nightfury.db.sql.*
import me.kgustave.nightfury.entities.Case
import net.dv8tion.jda.core.entities.*
import java.sql.*

/**
 * @author Kaidan Gustave
 */
class DatabaseManager @Throws(Exception::class) constructor(url: String, user: String, pass: String)
{
    val connection : Connection

    init {
        Class.forName("org.h2.Driver").newInstance()

        connection = DriverManager.getConnection(url, user, pass) // Create the connection

        for(data in TableData.values())
        {
            if(!(connection createTable data))
            {
                connection.close()
                throw SQLException("Failed to set up vital DB Data!")
            }
        }
    }

    private val roleMe = RoleMe(connection)
    private val colorMe = ColorMe(connection)
    private val modRole = ModeratorRole(connection)
    private val mutedRole = MutedRole(connection)

    private val rolePersist = SQLRolePersist(connection)

    private val modLog = ModeratorLog(connection)
    private val ignoredChannels = IgnoredChannels(connection)

    private val cases = SQLCases(connection)

    private val prefixes = SQLPrefixes(connection)
    private val welcomes = SQLWelcomes(connection)

    private val enables = SQLEnables(connection)

    val localTags = SQLLocalTags(connection)
    val globalTags = SQLGlobalTags(connection)

    val customCommands = SQLCustomCommands(connection)

    val commandLimits = SQLLimits(connection)

    val musicWhitelist = SQLMusicWhitelist(connection)

    val levels = SQLLevel(connection)

    private infix fun Connection.createTable(data: TableData): Boolean = top@ try {
        if(this hasTableNamed data.name)
            return@top true
        else {
            val params = data.parameters
            // Automatically builds a statement out of parameter data
            val statement = buildString {
                append("CREATE TABLE ${data.name} (")
                for(i in 0 until (params.size-1))
                    append(params[i]).append(", ")
                append("${params[params.size-1]})")
            }
            using(this.prepareStatement(statement)) { execute() }
            SQL.LOG.info("Created ${data.name} Table!")
            return@top true
        }
    } catch (e : SQLException) {
        SQL.LOG.warn("SQLException",e)
        return@top false
    }

    private infix fun Connection.hasTableNamed(name: String) =
            metaData.getTables(null, null, name, null).use { it.next() }

    infix fun isRoleMe(role: Role) = roleMe.isRole(role)
    infix fun getRoleMes(guild: Guild) = roleMe.getRoles(guild)
    infix fun addRoleMe(role: Role) = roleMe.addRole(role)
    infix fun removeRoleMe(role: Role) = roleMe.deleteRole(role)

    infix fun isColorMe(role: Role) = colorMe.isRole(role)
    infix fun getColorMes(guild: Guild) = colorMe.getRoles(guild)
    infix fun addColorMe(role: Role) = colorMe.addRole(role)
    infix fun removeColorMe(role: Role) = colorMe.deleteRole(role)

    infix fun hasModRole(guild: Guild) = modRole.hasRole(guild)
    infix fun getModRole(guild: Guild) = modRole.getRole(guild)
    infix fun setModRole(role: Role) = modRole.setRole(role)
    infix fun resetModRole(guild: Guild) = modRole.deleteRole(guild)

    infix fun hasMutedRole(guild: Guild) = mutedRole.hasRole(guild)
    infix fun getMutedRole(guild: Guild) = mutedRole.getRole(guild)
    infix fun setMutedRole(role: Role) = mutedRole.setRole(role)
    infix fun resetMutedRole(guild: Guild) = mutedRole.deleteRole(guild)

    infix fun hasModLog(guild: Guild) = modLog.hasChannel(guild)
    infix fun getModLog(guild: Guild) = modLog.getChannel(guild)
    infix fun setModLog(channel: TextChannel) = modLog.setChannel(channel)
    infix fun resetModLog(guild: Guild) = modLog.deleteChannel(guild)

    infix fun isIgnoredChannel(channel: TextChannel) = ignoredChannels.isChannel(channel)
    infix fun getIgnoredChannels(guild: Guild) = ignoredChannels.getChannels(guild)
    infix fun addIgnoredChannel(channel: TextChannel) = ignoredChannels.addChannel(channel)
    infix fun removeIgnoredChannel(channel: TextChannel) = ignoredChannels.deleteChannel(channel)

    fun getCaseNumber(guild: Guild, number: Int) = cases.getCaseNumber(guild, number)
    fun getCasesByUser(member: Member) = getCasesByUser(member.guild, member.user)
    fun getCasesByUser(guild: Guild, user: User) = cases.getCasesByUser(guild, user)
    fun getCases(guild: Guild) = cases.getCases(guild)
    fun addCase(case: Case) = cases.addCase(case)
    fun updateCase(case: Case) = cases.updateCase(case)

    fun isPrefixFor(guild: Guild, prefix: String) = prefixes.isPrefix(guild, prefix)
    fun getPrefixes(guild: Guild) = prefixes.getPrefixes(guild)
    fun addPrefix(guild: Guild, prefix: String) = prefixes.addPrefix(guild, prefix)
    fun removePrefix(guild: Guild, prefix: String) = prefixes.removePrefix(guild, prefix)

    fun hasWelcome(guild: Guild) = welcomes.hasWelcome(guild)
    fun resetWelcome(guild: Guild) = welcomes.removeWelcome(guild)
    fun getWelcomeChannel(guild: Guild) = welcomes.getChannel(guild)
    fun setWelcome(channel: TextChannel, message: String) = welcomes.setWelcome(channel, message)
    fun getWelcomeMessage(guild: Guild) = welcomes.getMessage(guild)

    fun hasLimit(guild: Guild, command: String) = commandLimits.hasLimit(guild, command.toLowerCase())
    fun getLimit(guild: Guild, command: String) = commandLimits.getLimit(guild, command.toLowerCase())
    fun setLimit(guild: Guild, command: String, limit: Int) = commandLimits.setLimit(guild, command, limit)
    fun removeLimit(guild: Guild, command: String) = commandLimits.removeLimit(guild, command.toLowerCase())

    fun isRolePersist(guild: Guild) = enables.getStatusFor(guild, SQLEnables.Type.ROLE_PERSIST)
    fun setIsRolePersist(guild: Guild, boolean: Boolean) = enables.setStatusFor(guild, SQLEnables.Type.ROLE_PERSIST, boolean)
    fun getRolePersistence(member: Member) = rolePersist.getRolePersist(member)
    fun addRolePersist(member: Member) = rolePersist.setRolePersist(member)
    fun removeRolePersist(member: Member) = rolePersist.removeRolePersist(member)
    fun removeAllRolePersist(guild: Guild) = rolePersist.removeAllRolePersist(guild)

    @[Suppress("Unused") Throws(SQLException::class)]
    fun evaluate(string: String) = using(connection.prepareStatement(string)) { execute() }

    fun shutdown() = try { connection.close() } catch (e: SQLException) { SQL.LOG.warn("SQLException", e) }

    enum class TableData(vararg val parameters: String)
    {
        CASES("NUMBER INT",     "GUILD_ID BIGINT",      "MESSAGE_ID BIGINT",    "MOD_ID BIGINT",
              "TARGET_ID BIGINT", "IS_ON_USER BOOLEAN", "ACTION VARCHAR(200)", "REASON VARCHAR(200)"),

        CHANNELS("GUILD_ID BIGINT", "CHANNEL_ID BIGINT", "TYPE VARCHAR(200)"),

        PREFIXES("GUILD_ID BIGINT", "PREFIX VARCHAR(200)"),

        ROLES("GUILD_ID BIGINT", "ROLE_ID BIGINT", "TYPE VARCHAR(20)"),

        ROLE_PERSIST("GUILD_ID BIGINT", "USER_ID BIGINT", "ROLE_IDS VARCHAR(2000)"),

        GLOBAL_TAGS("NAME VARCHAR(50)", "OWNER_ID BIGINT", "CONTENT VARCHAR(1900)"),

        LOCAL_TAGS("NAME VARCHAR(50)", "GUILD_ID BIGINT", "OWNER_ID BIGINT", "CONTENT VARCHAR(1900)"),

        CUSTOM_COMMANDS("NAME VARCHAR(50)", "CONTENT VARCHAR(1900)", "GUILD_ID BIGINT"),

        WELCOMES("GUILD_ID BIGINT", "CHANNEL_ID BIGINT", "MESSAGE VARCHAR(1900)"),

        COMMAND_LIMITS("GUILD_ID BIGINT", "COMMAND_NAME VARCHAR(100)", "LIMIT_NUMBER INT"),

        ENABLES("GUILD_ID BIGINT", "ENABLE_TYPE VARCHAR(40)", "STATUS BOOLEAN"),

        GUILDS("GUILD_ID BIGINT", "TYPE VARCHAR(20)"),

        MUSIC_SETTINGS("GUILD_ID BIGINT", "VOICE_CHANNEL_ID BIGINT", "TEXT_CHANNEL_ID BIGINT", "NP_IN_TOPIC BOOLEAN"),

        COMMAND_LEVELS("GUILD_ID BIGINT", "COMMAND VARCHAR(200)", "LEVEL VARCHAR(50)");
    }
}