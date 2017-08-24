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
class SQLPrefixes(connection: Connection) : SQLCollection<Guild, String>(connection)
{
    override val getStatement = "SELECT prefix FROM prefixes WHERE guild_id = ?"
    override val addStatement = "INSERT INTO prefixes (guild_id, prefix) VALUES (?, ?)"
    override val removeStatement = "DELETE FROM prefixes WHERE guild_id = ? AND LOWER(prefix) = LOWER(?)"
    override val removeAllStatement = "DELETE FROM prefixes WHERE guild_id = ?"

    override fun get(results: ResultSet, env: Guild): Set<String> {
        val prefixes = HashSet<String>()
        while (results.next())
        {
            prefixes.add(results.getString("prefix"))
        }
        return prefixes
    }
}