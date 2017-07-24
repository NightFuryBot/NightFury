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
import net.dv8tion.jda.core.utils.SimpleLog
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
@Suppress("unused")
class DatabaseManager(url: String, user: String, pass: String) {

    companion object {
        private val LOG : SimpleLog = SimpleLog.getLog("SQL")
    }

    init {
        try {
            Class.forName("org.h2.Driver").newInstance()
        } catch (e: Exception) { LOG.fatal(e) }
    }

    private val connection : Connection = DriverManager.getConnection(url, user, pass)

    private val roleMe : SQLRoleMe = SQLRoleMe(connection)
    private val colorMe : SQLColorMe = SQLColorMe(connection)
    private val modRole : SQLModeratorRole = SQLModeratorRole(connection)
    private val mutedRole : SQLMutedRole = SQLMutedRole(connection)

    private val modLog : SQLModeratorLog = SQLModeratorLog(connection)
    private val ignoredChannels : SQLIgnoredChannels = SQLIgnoredChannels(connection)

    private val cases : SQLCases = SQLCases(connection)

    private val prefixes : SQLPrefixes = SQLPrefixes(connection)

    private val localTags : SQLLocalTags = SQLLocalTags(connection)
    private val globalTags : SQLGlobalTags = SQLGlobalTags(connection)

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

    fun getModRole(guild: Guild) = modRole.get(guild, guild.idLong)
    fun setModRole(role: Role) {
        if(getModRole(role.guild)!=null)
            modRole.update(role.idLong, role.guild.idLong)
        else
            modRole.set(role.guild.idLong, role.idLong)
    }
    fun resetModRole(guild: Guild) = modRole.reset(guild.idLong)

    fun getMutedRole(guild: Guild) = mutedRole.get(guild, guild.idLong)
    fun setMutedRole(role: Role) {
        if(getMutedRole(role.guild)!=null)
            mutedRole.update(role.idLong, role.guild.idLong)
        else
            mutedRole.set(role.guild.idLong, role.idLong)
    }
    fun resetMutedRole(guild: Guild) = mutedRole.reset(guild.idLong)

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

    fun getLatestCase(guild: Guild) : Case {
        val cases = getCases(guild)
        if(cases.isEmpty()) return Case()
        return cases.stream().filter { c -> c.number == cases.size }.findFirst().get()
    }
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

    fun isLocalTag(name: String, guild: Guild) = localTags.isTag(name, guild)
    fun addLocalTag(name: String, content: String, owner: Member) {
        localTags.addTag(name, owner.user.idLong, content, owner.guild)
    }
    fun editLocalTag(name: String, newContent: String, owner: Member) {
        localTags.editTag(newContent, name, owner.user.idLong, owner.guild)
    }
    fun deleteLocalTag(name: String, owner: Member) {
        localTags.deleteTag(name, owner.user.idLong, owner.guild)
    }
    fun getOriginalNameOfLocalTag(name: String, guild: Guild) = localTags.getOriginalName(name, guild)
    fun getContentForLocalTag(name: String, guild: Guild) = localTags.getTagContent(name, guild)
    fun isLocalTagOwner(name: String, owner: Member) = getOwnerIdForLocalTag(name, owner.guild) == owner.user.idLong
    fun getOwnerIdForLocalTag(name: String, guild: Guild) = localTags.getTagOwnerId(name, guild)
    fun overrideLocalTag(name: String, newContent: String, guild: Guild) {
        if(!isLocalTag(name,guild)) throw IllegalArgumentException("The specified name is not a local tag!")
        else localTags.overrideTag(newContent, name, getOwnerIdForLocalTag(name,guild),guild)
    }

    fun isGlobalTag(name: String) = globalTags.isTag(name)
    fun addGlobalTag(name: String, content: String, owner: User) {
        globalTags.addTag(name, owner.idLong, content)
    }
    fun editGlobalTag(name: String, newContent: String, owner: User) {
        globalTags.editTag(newContent, name, owner.idLong)
    }
    fun deleteGlobalTag(name: String, owner: User) {
        globalTags.deleteTag(name, owner.idLong)
    }
    fun getOriginalNameOfGlobalTag(name: String) = globalTags.getOriginalName(name)
    fun getContentForGlobalTag(name: String) = globalTags.getTagContent(name)
    fun isGlobalTagOwner(name: String, owner: User) = getOwnerIdForGlobalTag(name) == owner.idLong
    fun getOwnerIdForGlobalTag(name: String) = globalTags.getTagOwnerId(name)

    fun evaluate(string: String) {
        try {
            val statement = connection.prepareStatement(string)
            statement.execute()
            statement.close()
        } catch (e: SQLException) { throw e }
    }

    fun shutdown() = try { connection.close() } catch (e: SQLException) { LOG.warn(e) }
}