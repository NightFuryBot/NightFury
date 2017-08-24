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
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
@Suppress("RedundantUnitReturnType")
class SQLCustomCommands(val connection: Connection)
{
    private val getAll     = "SELECT name FROM custom_commands WHERE guild_id = ?"
    private val getContent = "SELECT content FROM custom_commands WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val add        = "INSERT INTO custom_commands (name, content, guild_id) VALUES (?, ?, ?)"
    private val remove     = "DELETE FROM custom_commands WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val removeAll  = "DELETE FROM custom_commands WHERE guild_id = ?"

    fun getAll(guild: Guild) : Set<String> {
        val all = HashSet<String>()
        try {
            connection prepare getAll closeAfter {
                insert(guild.idLong) executeQuery { while(it.next()) all.add(it.getString("name")) }
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return all
    }

    fun getContentFor(name : String, guild: Guild) = try {
        connection prepare getContent closeAfter {
            insert(name, guild.idLong) executeQuery { if(it.next()) it.getString("content")?:"" else "" }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        ""
    }

    fun add(name: String, content: String, guild: Guild) : Unit = try {
        connection prepare(add) closeAfter { insert(name, content, guild.idLong).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun remove(name: String, guild: Guild) : Unit = try {
        connection prepare(remove) closeAfter { insert(name, guild.idLong).execute() }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
    }

    fun removeAll(guild: Guild) : Unit = try {
        connection prepare removeAll closeAfter { insert(guild.idLong).execute() }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
    }
}