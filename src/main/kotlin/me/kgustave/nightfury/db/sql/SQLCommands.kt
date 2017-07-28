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
class SQLCustomCommands(val connection: Connection)
{

    private val getAll     = "SELECT name FROM custom_commands WHERE guild_id = ?"
    private val getContent = "SELECT content FROM custom_commands WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val add        = "INSERT INTO custom_commands (name, content, guild_id) VALUES (?, ?, ?)"
    private val remove     = "DELETE FROM custom_commands WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val removeAll  = "DELETE FROM custom_commands WHERE guild_id = ?"

    fun getAll(guild: Guild) : Set<String>
    {
        val all = HashSet<String>()
        try {
            val statement = connection.prepareStatement(getAll)
            statement.setLong(1, guild.idLong)
            statement.executeQuery().use { while(it.next()) all.add(it.getString("name")) }
            statement.close()
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return all
    }

    fun getContentFor(name : String, guild: Guild) : String
    {
        try {
            val statement = connection.prepareStatement(getContent)
            statement.setString(1, name)
            statement.setLong(2, guild.idLong)
            statement.executeQuery().use { if(it.next()) return it.getString("content") }
            statement.close()
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return ""
    }

    fun add(name: String, content: String, guild: Guild)
    {
        try {
            val statement = connection.prepareStatement(add)
            statement.setString(1, name)
            statement.setString(2, content)
            statement.setLong(3, guild.idLong)
            statement.execute()
            statement.close()
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun remove(name: String, guild: Guild)
    {
        try {
            val statement = connection.prepareStatement(remove)
            statement.setString(1, name)
            statement.setLong(2, guild.idLong)
            statement.execute()
            statement.close()
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun removeAll(guild: Guild)
    {
        try {
            val statement = connection.prepareStatement(removeAll)
            statement.setLong(1, guild.idLong)
            statement.execute()
            statement.close()
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }
}