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
@file:Suppress("Unused")
package xyz.nightfury.db

import xyz.nightfury.entities.Case
import net.dv8tion.jda.core.entities.*
import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
object Database : Closeable {
    var init: Boolean = false

    @Suppress("ObjectPropertyName")
    lateinit private var _connection: Connection

    val connection: Connection
        get() {
            if(!init)
                throw UninitializedPropertyAccessException("Connection has not been opened yet!")
            return _connection
        }

    fun connect(url: String, user: String, pass: String) {
        Class.forName("org.h2.Driver").newInstance()

        _connection = DriverManager.getConnection(url, user, pass)
        init = true

        for(data in TableData.values()) {
            if(!(connection createTable data)) {
                connection.close()
                throw SQLException("Failed to set up vital DB Data!")
            }
        }
    }

    override fun close() {
        SQL.LOG.info("Closing JDBC Connection...")
        connection.close()
    }

    private infix fun Connection.createTable(data: TableData): Boolean {
        try {
            return if(this hasTableNamed data.name) { true } else {
                val params = data.parameters
                // Automatically builds a statement out of parameter data
                val statement = buildString {
                    append("CREATE TABLE ${data.name} (")
                    for(i in 0 until (params.size-1)) append(params[i]).append(", ")
                    append("${params[params.size-1]})")
                }
                using(this.prepareStatement(statement)) { execute() }
                SQL.LOG.info("Created ${data.name} Table!")
                true
            }
        } catch (e : SQLException) {
            SQL.LOG.warn("SQLException",e)
            return false
        }
    }

    private infix fun Connection.hasTableNamed(name: String) =
        metaData.getTables(null, null, name, null).use { it.next() }

    infix fun isRoleMe(role: Role) = SQLRoleMe.isRole(role)
    infix fun getRoleMes(guild: Guild) = SQLRoleMe.getRoles(guild)
    infix fun addRoleMe(role: Role) = SQLRoleMe.addRole(role)
    infix fun removeRoleMe(role: Role) = SQLRoleMe.deleteRole(role)

    infix fun isColorMe(role: Role) = SQLColorMe.isRole(role)
    infix fun getColorMes(guild: Guild) = SQLColorMe.getRoles(guild)
    infix fun addColorMe(role: Role) = SQLColorMe.addRole(role)
    infix fun removeColorMe(role: Role) = SQLColorMe.deleteRole(role)

    infix fun hasModRole(guild: Guild) = SQLModeratorRole.hasRole(guild)
    infix fun getModRole(guild: Guild) = SQLModeratorRole.getRole(guild)
    infix fun setModRole(role: Role) = SQLModeratorRole.setRole(role)
    infix fun resetModRole(guild: Guild) = SQLModeratorRole.deleteRole(guild)

    infix fun hasMutedRole(guild: Guild) = SQLMutedRole.hasRole(guild)
    infix fun getMutedRole(guild: Guild) = SQLMutedRole.getRole(guild)
    infix fun setMutedRole(role: Role) = SQLMutedRole.setRole(role)
    infix fun resetMutedRole(guild: Guild) = SQLMutedRole.deleteRole(guild)

    infix fun hasModLog(guild: Guild) = SQLModeratorLog.hasChannel(guild)
    infix fun getModLog(guild: Guild) = SQLModeratorLog.getChannel(guild)
    infix fun setModLog(channel: TextChannel) = SQLModeratorLog.setChannel(channel)
    infix fun resetModLog(guild: Guild) = SQLModeratorLog.deleteChannel(guild)

    infix fun isIgnoredChannel(channel: TextChannel) = SQLIgnoredChannels.isChannel(channel)
    infix fun getIgnoredChannels(guild: Guild) = SQLIgnoredChannels.getChannels(guild)
    infix fun addIgnoredChannel(channel: TextChannel) = SQLIgnoredChannels.addChannel(channel)
    infix fun removeIgnoredChannel(channel: TextChannel) = SQLIgnoredChannels.deleteChannel(channel)

    fun getCaseNumber(guild: Guild, number: Int) = SQLCases.getCaseNumber(guild, number)
    fun getCasesByUser(member: Member) = getCasesByUser(member.guild, member.user)
    fun getCasesByUser(guild: Guild, user: User) = SQLCases.getCasesByUser(guild, user)
    fun getCases(guild: Guild) = SQLCases.getCases(guild)
    fun addCase(case: Case) = SQLCases.addCase(case)
    fun updateCase(case: Case) = SQLCases.updateCase(case)

    fun isPrefixFor(guild: Guild, prefix: String) = SQLPrefixes.isPrefix(guild, prefix)
    fun getPrefixes(guild: Guild) = SQLPrefixes.getPrefixes(guild)
    fun addPrefix(guild: Guild, prefix: String) = SQLPrefixes.addPrefix(guild, prefix)
    fun removePrefix(guild: Guild, prefix: String) = SQLPrefixes.removePrefix(guild, prefix)

    fun hasWelcome(guild: Guild) = SQLWelcomes.hasWelcome(guild)
    fun resetWelcome(guild: Guild) = SQLWelcomes.removeWelcome(guild)
    fun getWelcomeChannel(guild: Guild) = SQLWelcomes.getChannel(guild)
    fun setWelcome(channel: TextChannel, message: String) = SQLWelcomes.setWelcome(channel, message)
    fun getWelcomeMessage(guild: Guild) = SQLWelcomes.getMessage(guild)

    fun hasLimit(guild: Guild, command: String) = SQLLimits.hasLimit(guild, command.toLowerCase())
    fun getLimit(guild: Guild, command: String) = SQLLimits.getLimit(guild, command.toLowerCase())
    fun setLimit(guild: Guild, command: String, limit: Int) = SQLLimits.setLimit(guild, command, limit)
    fun removeLimit(guild: Guild, command: String) = SQLLimits.removeLimit(guild, command.toLowerCase())

    fun isRolePersist(guild: Guild) = SQLEnables.getStatusFor(guild, SQLEnables.Type.ROLE_PERSIST)
    fun setIsRolePersist(guild: Guild, boolean: Boolean) = SQLEnables.setStatusFor(guild, SQLEnables.Type.ROLE_PERSIST, boolean)
    fun getRolePersistence(member: Member) = SQLRolePersist.getRolePersist(member)
    fun addRolePersist(member: Member) = SQLRolePersist.setRolePersist(member)
    fun removeRolePersist(member: Member) = SQLRolePersist.removeRolePersist(member)
    fun removeAllRolePersist(guild: Guild) = SQLRolePersist.removeAllRolePersist(guild)

    @[Suppress("Unused") Throws(SQLException::class)]
    fun evaluate(string: String) = using(connection.prepareStatement(string)) { execute() }

    enum class TableData(vararg val parameters: String) {
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

        COMMAND_LEVELS("GUILD_ID BIGINT", "COMMAND VARCHAR(200)", "LEVEL VARCHAR(50)"),

        PROFILES("USER_ID BIGINT",
                 "TIME_ZONE_ID VARCHAR(70) DEFAULT NULL",
                 "GITHUB VARCHAR(100) DEFAULT NULL",
                 "WEBSITE VARCHAR(100) DEFAULT NULL",
                 "TWITCH VARCHAR(100) DEFAULT NULL",
                 "TITLE VARCHAR(50) DEFAULT NULL",
                 "ABOUT VARCHAR(1800) DEFAULT NULL",
                 "BIRTHDAY DATE DEFAULT NULL"),

        STARBOARD_ENTRIES("STARRED_ID BIGINT", "ENTRY_ID BIGINT DEFAULT NULL", "STARBOARD_ID BIGINT", "GUILD_ID BIGINT", "USER_ID BIGINT"),

        STARBOARD_SETTINGS("THRESHOLD INT DEFAULT 5", "MAX_AGE INT DEFAULT 72", "GUILD_ID BIGINT");
    }
}