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
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
class DatabaseManager @Throws(Exception::class) constructor(url: String, user: String, pass: String)
{
    val connection : Connection

    init {
        try {
            Class.forName("org.h2.Driver").newInstance()
        } catch (e: Exception) {
            throw e
        }
        // Create the connection
        connection = DriverManager.getConnection(url, user, pass)

        for (data in TableData.values())
            if(!(connection createTable data)) {
                connection.close()
                throw SQLException("Failed to set up vital DB Data!")
            }
    }

    private val roleMe : SQLRoleMe = SQLRoleMe(connection)
    private val colorMe : SQLColorMe = SQLColorMe(connection)
    private val modRole : SQLModeratorRole = SQLModeratorRole(connection)
    private val mutedRole : SQLMutedRole = SQLMutedRole(connection)

    private val modLog : SQLModeratorLog = SQLModeratorLog(connection)
    private val ignoredChannels : SQLIgnoredChannels = SQLIgnoredChannels(connection)
    private val welcomeChannels : SQLWelcomeChannel = SQLWelcomeChannel(connection)

    private val cases : SQLCases = SQLCases(connection)

    private val prefixes : SQLPrefixes = SQLPrefixes(connection)
    private val welcomesMessages : SQLWelcomeMessage = SQLWelcomeMessage(connection)

    val localTags : SQLLocalTags = SQLLocalTags(connection)
    val globalTags : SQLGlobalTags = SQLGlobalTags(connection)

    val customCommands : SQLCustomCommands = SQLCustomCommands(connection)

    val commandLimits : SQLLimits = SQLLimits(connection)

    infix fun Connection.createTable(data: TableData) : Boolean= top@ try {
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
            this prepare statement closeAfter { execute() }
            SQL.LOG.info("Created ${data.name} Table!")
            return@top true
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        return@top false
    }

    infix fun Connection.hasTableNamed(name: String) = this.metaData.getTables(null, null, name, null).use { it.next() }

    infix fun isRoleMe(role: Role) : Boolean {
        val rolemes = getRoleMes(role.guild)
        return rolemes.isNotEmpty() && rolemes.contains(role)
    }
    infix fun getRoleMes(guild: Guild) = roleMe.get(guild, guild.idLong)
    infix fun addRoleMe(role: Role) {
        roleMe.add(role.guild.idLong, role.idLong)
    }
    infix fun removeRoleMe(role: Role) = roleMe.remove(role.guild.idLong, role.idLong)

    infix fun isColorMe(role: Role) : Boolean {
        val colormes = getColorMes(role.guild)
        return colormes.isNotEmpty() && colormes.contains(role)
    }
    infix fun getColorMes(guild: Guild) = colorMe.get(guild, guild.idLong)
    infix fun addColorMe(role: Role) = colorMe.add(role.guild.idLong, role.idLong)
    infix fun removeColorMe(role: Role) = colorMe.remove(role.guild.idLong, role.idLong)

    infix fun hasModRole(guild: Guild) = modRole.has(guild.idLong)
    infix fun getModRole(guild: Guild) = modRole.get(guild, guild.idLong)
    infix fun setModRole(role: Role) = if(getModRole(role.guild)!=null)
        modRole.update(role.idLong, role.guild.idLong)
    else
        modRole.set(role.guild.idLong, role.idLong)
    infix fun resetModRole(guild: Guild) = modRole.reset(guild.idLong)

    infix fun hasMutedRole(guild: Guild) = mutedRole.has(guild.idLong)
    infix fun getMutedRole(guild: Guild) = mutedRole.get(guild, guild.idLong)
    infix fun setMutedRole(role: Role) = if(getMutedRole(role.guild)!=null)
        mutedRole.update(role.idLong, role.guild.idLong)
    else
        mutedRole.set(role.guild.idLong, role.idLong)
    infix fun resetMutedRole(guild: Guild) = mutedRole.reset(guild.idLong)

    infix fun hasModLog(guild: Guild) = modLog.has(guild.idLong)
    infix fun getModLog(guild: Guild) = modLog.get(guild, guild.idLong)
    infix fun setModLog(channel: TextChannel) = if(getModLog(channel.guild)!=null)
        modLog.update(channel.idLong, channel.guild.idLong)
    else
        modLog.set(channel.guild.idLong, channel.idLong)
    infix fun resetModLog(guild: Guild) = modLog.reset(guild.idLong)

    infix fun isIgnoredChannel(channel: TextChannel) : Boolean {
        val ignored = getIgnoredChannels(channel.guild)
        return ignored.isNotEmpty() && ignored.contains(channel)
    }
    infix fun getIgnoredChannels(guild: Guild) = ignoredChannels.get(guild, guild.idLong)
    infix fun addIgnoredChannel(channel: TextChannel) = ignoredChannels.add(channel.guild.idLong, channel.idLong)
    infix fun removeIgnoredChannel(channel: TextChannel) = ignoredChannels.remove(channel.guild.idLong, channel.idLong)

    fun getCaseMatching(guild: Guild, toMatch: (Case) -> Boolean) : Case {
        val cases = getCases(guild)
        if(cases.isEmpty()) return Case()
        return cases.stream().filter(toMatch).findFirst().takeIf { it.isPresent }?.get()?:Case()
    }
    fun getFirstCaseMatching(guild: Guild, toMatch: (Case) -> Boolean) : Case {
        val cases = getCases(guild)
        if(cases.isEmpty()) return Case()
        return cases.stream().filter(toMatch).sorted(Comparator.comparing(Case::number)).findFirst().takeIf { it.isPresent }?.get()?: Case()
    }
    infix fun getCases(guild: Guild) = cases.get(guild, guild.idLong)
    infix fun addCase(case: Case) = cases.add(*case.toDBArgs())
    infix fun updateCase(case: Case) = cases.updateCase(case)

    fun isPrefixFor(guild: Guild, prefix: String) : Boolean {
        val prefixes = getPrefixes(guild)
        return prefixes.isNotEmpty() && prefixes.stream().anyMatch { it.equals(prefix, ignoreCase = true) }
    }
    infix fun getPrefixes(guild: Guild) = prefixes.get(guild, guild.idLong)
    fun addPrefix(guild: Guild, prefix: String) = prefixes.add(guild.idLong, prefix)
    fun removePrefix(guild: Guild, prefix: String) = prefixes.remove(guild.idLong, prefix)

    infix fun hasWelcome(guild: Guild) = welcomeChannels.has(guild.idLong) && welcomesMessages.has(guild.idLong)
    infix fun resetWelcome(guild: Guild) {
        welcomeChannels.reset(guild.idLong)
        welcomesMessages.reset(guild.idLong)
    }
    infix fun getWelcomeChannel(guild: Guild) = welcomeChannels.get(guild, guild.idLong)
    infix fun setWelcomeChannel(channel: TextChannel) = if(getWelcomeChannel(channel.guild)!=null)
        welcomeChannels.update(channel.idLong, channel.guild.idLong)
    else
        welcomeChannels.set(channel.guild.idLong, channel.idLong)
    infix fun getWelcomeMessage(guild: Guild) = welcomesMessages.get(guild, guild.idLong)
    fun setWelcomeMessage(guild: Guild, welcome: String) {
        if(getWelcomeMessage(guild)!=null)
            welcomesMessages.update(welcome, guild.idLong)
        else
            welcomesMessages.set(guild.idLong, welcome)
    }

    fun hasLimit(guild: Guild, command: String) = commandLimits.hasLimit(guild, command.toLowerCase())
    fun getLimit(guild: Guild, command: String) = commandLimits.getLimit(guild, command.toLowerCase())
    fun setLimit(guild: Guild, command: String, limit: Int) = if(hasLimit(guild, command))
        commandLimits.setLimit(guild, command.toLowerCase(), limit)
    else
        commandLimits.addLimit(guild, command.toLowerCase(), limit)
    fun removeLimit(guild: Guild, command: String) = commandLimits.removeLimit(guild, command.toLowerCase())

    @Suppress("Unused")
    infix fun evaluate(string: String) = try {
        connection prepare string closeAfter { execute() }
    } catch (e: SQLException) { throw e }

    fun shutdown() = try {
        connection.close()
    } catch (e: SQLException) { SQL.LOG.warn(e) }

    enum class TableData(vararg val parameters: String)
    {
        CASES("number int",     "guild_id long",      "message_id long",    "mod_id long",
              "target_id long", "is_on_user boolean", "action varchar(20)", "reason varchar(200)"),

        CHANNELS("guild_id long", "channel_id long", "type varchar(20)"),

        PREFIXES("guild_id long", "prefix varchar(50)"),

        ROLES("guild_id long", "role_id long", "type varchar(20)"),

        GLOBAL_TAGS("name varchar(50)", "owner_id long", "content varchar(1900)"),

        LOCAL_TAGS("name varchar(50)", "guild_id long", "owner_id long", "content varchar(1900)"),

        CUSTOM_COMMANDS("name varchar(50)", "content varchar(1900)", "guild_id long"),

        WELCOMES("guild_id long", "welcome varchar(1900)"),

        COMMAND_LIMITS("guild_id long", "command_name varchar(100)", "limit_number int");
    }
}