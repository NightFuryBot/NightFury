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
class SQLLimits(val connection: Connection)
{
    private val getCommandLimit = "SELECT limit_number FROM command_limits WHERE guild_id = ? AND command_name = ?"
    private val addCommandLimit = "INSERT INTO command_limits (guild_id, command_name, limit_number) VALUES (?, ?, ?)"
    private val setCommandLimit = "UPDATE command_limits SET limit_number = ? WHERE guild_id = ? AND command_name = ?"
    private val removeCommandLimit = "DELETE FROM command_limits WHERE guild_id = ? AND command_name = ?"
    private val removeAllCommandLimits = "DELETE FROM command_limits WHERE guild_id = ?"

    fun hasLimit(guild: Guild, command: String) : Boolean = getLimit(guild, command) != 0

    fun getLimit(guild: Guild, command: String) : Int
    {
        return try {
            connection.prepareStatement(getCommandLimit).use {
                it.setLong(1, guild.idLong)
                it.setString(2, command)
                it.executeQuery().use { if(it.next()) it.getInt("limit_number") else 0 }
            }
        } catch (e: SQLException) {
            SQL.LOG.warn(e)
            0
        }
    }

    fun addLimit(guild: Guild, command: String, limit: Int)
    {
        try {
            connection.prepareStatement(addCommandLimit).use {
                it.setLong(1, guild.idLong)
                it.setString(2, command)
                it.setInt(3, limit)
                it.execute()
            }
        } catch (e: SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun setLimit(guild: Guild, command: String, limit: Int)
    {
        try {
            connection.prepareStatement(setCommandLimit).use {
                it.setInt(1, limit)
                it.setLong(2, guild.idLong)
                it.setString(3, command)
                it.execute()
            }
        } catch (e: SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun removeLimit(guild: Guild, command: String)
    {
        try {
            connection.prepareStatement(removeCommandLimit).use {
                it.setLong(1, guild.idLong)
                it.setString(2, command)
                it.execute()
            }
        } catch (e: SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun removeAllLimits(guild: Guild)
    {
        try {
            connection.prepareStatement(removeAllCommandLimits).use {
                it.setLong(1, guild.idLong)
                it.execute()
            }
        } catch (e: SQLException) {
            SQL.LOG.warn(e)
        }
    }
}