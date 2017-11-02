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

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
abstract class SQLGuilds(type: String) : Table() {
    private val get    = "SELECT GUILD_ID FROM GUILDS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val getAll = "SELECT GUILD_ID FROM GUILDS WHERE TYPE = '$type'"
    private val add    = "INSERT INTO GUILDS (GUILD_ID, TYPE) VALUES (?, '$type')"
    private val delete = "DELETE FROM GUILDS WHERE GUILD_ID = ? AND TYPE = '$type'"

    fun isGuild(guild: Guild): Boolean {
        return using(connection.prepareStatement(get), default = false)
        {
            this[1] = guild.idLong
            using(executeQuery()) { next() }
        }
    }

    fun getGuilds(jda: JDA): Set<Guild> {
        val set = HashSet<Guild>()
        using(connection.prepareStatement(getAll))
        {
            using(executeQuery())
            {
                while(next())
                    set += (jda.getGuildById(getLong("GUILD_ID"))?:continue)
            }
        }
        return set
    }

    fun getGuildIds(): Set<Long> {
        val set = HashSet<Long>()
        using(connection.prepareStatement(getAll))
        {
            using(executeQuery())
            {
                while(next())
                    set += getLong("GUILD_ID")
            }
        }
        return set
    }

    fun addGuild(guild: Guild) {
        using(connection.prepareStatement(add))
        {
            this[1] = guild.idLong
            execute()
        }
    }

    fun removeGuild(guild: Guild) {
        using(connection.prepareStatement(delete))
        {
            this[1] = guild.idLong
            execute()
        }
    }
}

object SQLMusicWhitelist : SQLGuilds("music")
object SQLBlacklist : SQLGuilds("blacklist")