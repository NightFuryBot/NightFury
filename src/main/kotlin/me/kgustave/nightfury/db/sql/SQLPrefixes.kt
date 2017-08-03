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
package me.kgustave.nightfury.db.sql

import net.dv8tion.jda.core.entities.Guild
import java.sql.Connection
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
class SQLPrefixes(connection: Connection) : SQLCollection<Guild, String>(connection) {

    init {
        getStatement = "SELECT $PREFIX FROM $PREFIXES WHERE $GUILD_ID = ?"
        addStatement = "INSERT INTO $PREFIXES ($GUILD_ID, $PREFIX) VALUES (?, ?)"
        removeStatement = "DELETE FROM $PREFIXES WHERE $GUILD_ID = ? AND $PREFIX = ?"
        removeAllStatement = "DELETE FROM $PREFIXES WHERE $GUILD_ID = ?"
    }

    override fun get(results: ResultSet, env: Guild): Set<String>
    {
        val prefixes = HashSet<String>()
        while (results.next())
        {
            prefixes.add(results.getString(PREFIX))
        }
        return prefixes
    }
}

private val PREFIXES = "prefixes"
private val GUILD_ID = "guild_id"
private val PREFIX = "prefix"