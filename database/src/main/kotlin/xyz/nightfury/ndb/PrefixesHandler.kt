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
package xyz.nightfury.ndb

import xyz.nightfury.ndb.internal.*

/**
 * @author Kaidan Gustave
 */
object PrefixesHandler : Database.Table() {
    private const val GET_PREFIXES = "SELECT PREFIX FROM PREFIXES WHERE GUILD_ID = ?"
    private const val HAS_PREFIX = "SELECT * FROM PREFIXES WHERE GUILD_ID = ? AND PREFIX = LOWER(?)"
    private const val ADD_PREFIX = "INSERT INTO PREFIXES (GUILD_ID, PREFIX) VALUES (?, LOWER(?))"
    private const val REMOVE_PREFIX = "DELETE FROM PREFIXES WHERE GUILD_ID = ? AND PREFIX = LOWER(?)"

    fun getPrefixes(guildId: Long): List<String> = sql({ emptyList() }) {
        val prefixes = ArrayList<String>()
        statement(GET_PREFIXES) {
            this[1] = guildId
            queryAll { prefixes += it.getString("PREFIX") }
        }
        return prefixes
    }

    fun hasPrefix(guildId: Long, prefix: String): Boolean = sql(false) {
        any(HAS_PREFIX) {
            this[1] = guildId
            this[2] = prefix
        }
    }

    fun addPrefix(guildId: Long, prefix: String) = sql {
        execute(ADD_PREFIX) {
            this[1] = guildId
            this[2] = prefix
        }
    }

    fun removePrefix(guildId: Long, prefix: String) = sql {
        execute(REMOVE_PREFIX) {
            this[1] = guildId
            this[2] = prefix
        }
    }
}