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
package xyz.nightfury.ndb.guilds

import xyz.nightfury.ndb.*

/**
 * @author Kaidan Gustave
 */
abstract class SingleGuildHandler(type: DBGuildType): Database.Table() {
    private val isGuild     = "SELECT * FROM GUILDS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val getGuilds   = "SELECT GUILD_ID FROM GUILDS WHERE TYPE = '$type'"
    private val addGuild    = "INSERT INTO GUILDS (GUILD_ID, TYPE) VALUES (?, '$type')"
    private val removeGuild = "DELETE FROM GUILDS WHERE GUILD_ID = ? AND TYPE = '$type'"

    fun isGuild(guildId: Long): Boolean = sql(false) {
        any(isGuild) {
            this[1] = guildId
        }
    }

    fun getGuilds(): List<Long> = sql({ emptyList() }) {
        val guilds = ArrayList<Long>()
        statement(getGuilds) {
            queryAll { guilds += it.getLong("GUILD_ID") }
        }
        return guilds
    }

    fun addGuild(guildId: Long) = sql {
        execute(addGuild) {
            this[1] = guildId
        }
    }

    fun removeGuild(guildId: Long) = sql {
        execute(removeGuild) {
            this[1] = guildId
        }
    }
}

object MusicWhitelistHandler : SingleGuildHandler(DBGuildType.MUSIC)
object JoinWhitelistHandler : SingleGuildHandler(DBGuildType.JOIN_WHITELIST)
object BlacklistHandler : SingleGuildHandler(DBGuildType.BLACKLIST)
