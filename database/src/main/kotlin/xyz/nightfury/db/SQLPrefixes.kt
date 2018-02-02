/*
 * Copyright 2017-2018 Kaidan Gustave
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
@file:Suppress("Unused", "MemberVisibilityCanBePrivate")
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
interface ISQLPrefixes {
    fun isPrefix(guildId: Long, prefix: String): Boolean
    fun getPrefixes(guildId: Long): Set<String>
    fun addPrefix(guildId: Long, prefix: String)
    fun removePrefix(guildId: Long, prefix: String)
    fun removeAllPrefixes(guildId: Long)
}

/**
 * @author Kaidan Gustave
 */
object SQLPrefixes : Table(), ISQLPrefixes {
    private const val isPrefix  = "SELECT PREFIX FROM PREFIXES WHERE GUILD_ID = ? AND LOWER(PREFIX) = LOWER(?)"
    private const val get       = "SELECT PREFIX FROM PREFIXES WHERE GUILD_ID = ?"
    private const val add       = "INSERT INTO PREFIXES (GUILD_ID, PREFIX) VALUES (?, ?)"
    private const val remove    = "DELETE FROM PREFIXES WHERE GUILD_ID = ? AND LOWER(PREFIX) = LOWER(?)"
    private const val removeAll = "DELETE FROM PREFIXES WHERE GUILD_ID = ?"

    fun isPrefix(guild: Guild, prefix: String) = isPrefix(guild.idLong, prefix)
    override fun isPrefix(guildId: Long, prefix: String): Boolean {
        return using(connection.prepareStatement(isPrefix), default = false) {
            this[1] = guildId
            this[2] = prefix
            using(executeQuery()) { next() }
        }
    }

    fun getPrefixes(guild: Guild): Set<String> = getPrefixes(guild.idLong)
    override fun getPrefixes(guildId: Long): Set<String> {
        val prefixes = HashSet<String>()
        using(connection.prepareStatement(get)) {
            this[1] = guildId
            using(executeQuery()) {
                while(next()) prefixes += getString("PREFIX")
            }
        }
        return prefixes
    }

    fun addPrefix(guild: Guild, prefix: String) = addPrefix(guild.idLong, prefix)
    override fun addPrefix(guildId: Long, prefix: String) {
        using(connection.prepareStatement(add)) {
            this[1] = guildId
            this[2] = prefix.toLowerCase()
            execute()
        }
    }

    fun removePrefix(guild: Guild, prefix: String) = removePrefix(guild.idLong, prefix)
    override fun removePrefix(guildId: Long, prefix: String) {
        using(connection.prepareStatement(remove)) {
            this[1] = guildId
            this[2] = prefix
            execute()
        }
    }

    fun removeAllPrefixes(guild: Guild) = removeAllPrefixes(guild.idLong)
    override fun removeAllPrefixes(guildId: Long) {
        using(connection.prepareStatement(removeAll)) {
            this[1] = guildId
            execute()
        }
    }
}
