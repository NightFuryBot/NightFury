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
class DatabaseManager(url: String, user: String, pass: String)
{
    init {
        try {
            Class.forName("org.h2.Driver").newInstance()
        } catch (e: Exception) { SQL.LOG.fatal(e) }
    }

    private val connection : Connection = DriverManager.getConnection(url, user, pass)

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

    fun setupDatabase() : Boolean
    {
        return setupCasesTable() && setupChannelsTable()
        && setupPrefixesTable() && setupRolesTable()
        && setupGlobalTagsTable() && setupLocalTagsTable()
        && setupCommandsTable() && setupWelcomesTable()
    }

    fun setupCasesTable() : Boolean = createTable("Cases")
    {
        "CREATE TABLE cases (" +
                "number int, guild_id long, message_id long, mod_id long, target_id long," +
                " is_on_user boolean, action varchar(20), reason varchar(200)" +
            ")"
    }

    fun setupChannelsTable() : Boolean = createTable("Channels")
    { "CREATE TABLE channels (guild_id long, channel_id long, type varchar(20))" }

    fun setupPrefixesTable() : Boolean = createTable("Prefixes")
    { "CREATE TABLE prefixes (guild_id long, prefix varchar(50))" }

    fun setupRolesTable() : Boolean = createTable("Roles")
    { "CREATE TABLE roles (guild_id long, role_id long, type varchar(20))" }

    fun setupGlobalTagsTable() : Boolean = createTable("Global Tags")
    { "CREATE TABLE global_tags (name varchar(50), owner_id long, content varchar(1900))" }

    fun setupLocalTagsTable() : Boolean = createTable("Local Tags")
    { "CREATE TABLE local_tags (name varchar(50), guild_id long, owner_id long, content varchar(1900))" }

    fun setupCommandsTable() : Boolean = createTable("Custom Commands")
    { "CREATE TABLE custom_commands (name varchar(50), content varchar(1900), guild_id long)" }

    fun setupWelcomesTable() : Boolean = createTable("Welcomes")
    { "CREATE TABLE welcomes (guild_id long, welcome varchar(1900))" }

    fun setupLimitsTable() : Boolean = createTable("command_limits")
    { "CREATE TABLE command_limits (guild_id long, command_name varchar(100), limit_number int)"}

    private fun createTable(tableName : String, sql : () -> String) : Boolean
    {
        try {
            evaluate(sql())
            SQL.LOG.info("Created $tableName Table!")
            return true
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return false
        }
    }

    fun isRoleMe(role: Role) : Boolean {
        val rolemes = getRoleMes(role.guild)
        return rolemes.isNotEmpty() && rolemes.contains(role)
    }
    fun getRoleMes(guild: Guild) = roleMe.get(guild, guild.idLong)
    fun addRoleMe(role: Role) {
        roleMe.add(role.guild.idLong, role.idLong)
    }
    fun removeRoleMe(role: Role) = roleMe.remove(role.guild.idLong, role.idLong)

    fun isColorMe(role: Role) : Boolean {
        val colormes = getColorMes(role.guild)
        return colormes.isNotEmpty() && colormes.contains(role)
    }
    fun getColorMes(guild: Guild) = colorMe.get(guild, guild.idLong)
    fun addColorMe(role: Role) = colorMe.add(role.guild.idLong, role.idLong)
    fun removeColorMe(role: Role) = colorMe.remove(role.guild.idLong, role.idLong)

    fun hasModRole(guild: Guild) = modRole.has(guild.idLong)
    fun getModRole(guild: Guild) = modRole.get(guild, guild.idLong)
    fun setModRole(role: Role) {
        if(getModRole(role.guild)!=null)
            modRole.update(role.idLong, role.guild.idLong)
        else
            modRole.set(role.guild.idLong, role.idLong)
    }
    fun resetModRole(guild: Guild) = modRole.reset(guild.idLong)

    fun hasMutedRole(guild: Guild) = mutedRole.has(guild.idLong)
    fun getMutedRole(guild: Guild) = mutedRole.get(guild, guild.idLong)
    fun setMutedRole(role: Role) {
        if(getMutedRole(role.guild)!=null)
            mutedRole.update(role.idLong, role.guild.idLong)
        else
            mutedRole.set(role.guild.idLong, role.idLong)
    }
    fun resetMutedRole(guild: Guild) = mutedRole.reset(guild.idLong)

    fun hasModLog(guild: Guild) = modLog.has(guild.idLong)
    fun getModLog(guild: Guild) = modLog.get(guild, guild.idLong)
    fun setModLog(channel: TextChannel) {
        if(getModLog(channel.guild)!=null)
            modLog.update(channel.idLong, channel.guild.idLong)
        else
            modLog.set(channel.guild.idLong, channel.idLong)
    }
    fun resetModLog(guild: Guild) = modLog.reset(guild.idLong)

    fun isIgnoredChannel(channel: TextChannel) : Boolean {
        val ignored = getIgnoredChannels(channel.guild)
        return ignored.isNotEmpty() && ignored.contains(channel)
    }
    fun getIgnoredChannels(guild: Guild) = ignoredChannels.get(guild, guild.idLong)
    fun addIgnoredChannel(channel: TextChannel) = ignoredChannels.add(channel.guild.idLong, channel.idLong)
    fun removeIgnoredChannel(channel: TextChannel) = ignoredChannels.remove(channel.guild.idLong, channel.idLong)

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
    fun getCases(guild: Guild) = cases.get(guild, guild.idLong)
    fun addCase(case: Case) = cases.add(*case.toDBArgs())
    fun updateCase(case: Case) = cases.updateCase(case)

    fun isPrefixFor(guild: Guild, prefix: String) : Boolean {
        val prefixes = getPrefixes(guild)
        return prefixes.isNotEmpty() && prefixes.stream().anyMatch { p -> p.equals(prefix, ignoreCase = true) }
    }
    fun getPrefixes(guild: Guild) = prefixes.get(guild, guild.idLong)
    fun addPrefix(guild: Guild, prefix: String) = prefixes.add(guild.idLong, prefix)
    fun removePrefix(guild: Guild, prefix: String) = prefixes.remove(guild.idLong, prefix)

    fun hasWelcome(guild: Guild) = welcomeChannels.has(guild.idLong) && welcomesMessages.has(guild.idLong)
    fun resetWelcome(guild: Guild) {
        welcomeChannels.reset(guild.idLong)
        welcomesMessages.reset(guild.idLong)
    }
    fun getWelcomeChannel(guild: Guild) = welcomeChannels.get(guild, guild.idLong)
    fun setWelcomeChannel(channel: TextChannel) {
        if(getWelcomeChannel(channel.guild)!=null)
            welcomeChannels.update(channel.idLong, channel.guild.idLong)
        else
            welcomeChannels.set(channel.guild.idLong, channel.idLong)
    }
    fun getWelcomeMessage(guild: Guild) = welcomesMessages.get(guild, guild.idLong)
    fun setWelcomeMessage(guild: Guild, welcome: String) {
        if(getWelcomeMessage(guild)!=null)
            welcomesMessages.update(welcome, guild.idLong)
        else
            welcomesMessages.set(guild.idLong, welcome)
    }

    fun hasLimit(guild: Guild, command: String) = commandLimits.hasLimit(guild, command.toLowerCase())
    fun getLimit(guild: Guild, command: String) = commandLimits.getLimit(guild, command.toLowerCase())
    fun setLimit(guild: Guild, command: String, limit: Int) {
        if(hasLimit(guild, command))
            commandLimits.setLimit(guild, command.toLowerCase(), limit)
        else
            commandLimits.addLimit(guild, command.toLowerCase(), limit)
    }
    fun removeLimit(guild: Guild, command: String) = commandLimits.removeLimit(guild, command.toLowerCase())

    fun evaluate(string: String) {
        try {
            val statement = connection.prepareStatement(string)
            statement.use { it.execute() }
        } catch (e: SQLException) { throw e }
    }

    fun leaveGuild(guild: Guild)
    {
        roleMe.removeAll(guild.idLong)
        colorMe.removeAll(guild.idLong)
        modRole.reset(guild.idLong)
        mutedRole.reset(guild.idLong)
        modLog.reset(guild.idLong)
        ignoredChannels.removeAll(guild.idLong)
        cases.removeAll(guild.idLong)
        prefixes.removeAll(guild.idLong)
        localTags.deleteAllTags(guild)
        customCommands.removeAll(guild)
        welcomeChannels.reset(guild.idLong)
        welcomesMessages.reset(guild.idLong)
        commandLimits.removeAllLimits(guild)
    }

    fun shutdown() = try { connection.close() } catch (e: SQLException) { SQL.LOG.warn(e) }
}