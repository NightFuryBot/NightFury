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
class SQLLimits(val connection: Connection)
{
    private val getCommandLimit = "SELECT limit_number FROM command_limits WHERE guild_id = ? AND LOWER(command_name) = LOWER(?)"
    private val addCommandLimit = "INSERT INTO command_limits (guild_id, command_name, limit_number) VALUES (?, ?, ?)"
    private val setCommandLimit = "UPDATE command_limits SET limit_number = ? WHERE guild_id = ? AND LOWER(command_name) = LOWER(?)"
    private val removeCommandLimit = "DELETE FROM command_limits WHERE guild_id = ? AND LOWER(command_name) = LOWER(?)"
    private val removeAllCommandLimits = "DELETE FROM command_limits WHERE guild_id = ?"

    fun hasLimit(guild: Guild, command: String) = getLimit(guild, command) != 0

    fun getLimit(guild: Guild, command: String) = try {
        connection prepare getCommandLimit closeAfter {
            insert(guild.idLong, command) executeQuery { if(it.next()) it.getInt("limit_number") else 0 }
        }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e); 0 }

    fun addLimit(guild: Guild, command: String, limit: Int) : Unit = try {
        connection prepare addCommandLimit closeAfter { insert(guild.idLong, command, limit).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }

    fun setLimit(guild: Guild, command: String, limit: Int) : Unit = try {
        connection prepare setCommandLimit closeAfter { insert(limit, guild.idLong, command).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }

    fun removeLimit(guild: Guild, command: String) = removeLimit(guild.idLong, command)

    fun removeLimit(id: Long, command: String) : Unit = try {
        connection prepare removeCommandLimit closeAfter { insert(id, command).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }

    fun removeAllLimits(guild: Guild) = removeAllLimits(guild.idLong)

    fun removeAllLimits(id: Long) : Unit = try {
        connection prepare removeAllCommandLimits closeAfter { insert(id).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }
}

class SQLEnables(private val connection: Connection)
{
    private fun hasStatusFor(guild: Guild, type: Type) = try {
        connection prepare "SELECT * FROM enables WHERE guild_id = ? AND enable_type = ? " closeAfter {
            insert(guild.idLong, type.name) executeQuery {
                it.next()
            }
        }
    } catch (e: SQLException) {
        SQL.LOG.warn("SQLException",e)
        false
    }

    fun getStatusFor(guild: Guild, type: Type): Boolean = try {
        connection prepare "SELECT status FROM enables WHERE guild_id = ? AND enable_type = ?" closeAfter {
            insert(guild.idLong, type.name) executeQuery {
                if(it.next())
                    it.getBoolean("status")
                else false
            }
        }
    } catch (e: SQLException) {
        SQL.LOG.warn("SQLException",e)
        false
    }

    fun setStatusFor(guild: Guild, type: Type, status: Boolean)
    {
        if(hasStatusFor(guild, type))
            connection prepare "UPDATE enables SET status = ? WHERE guild_id = ? AND enable_type = ?" closeAfter {
                insert(status, guild.idLong, type.name).execute()
            }
        else
            connection prepare "INSERT INTO enables (guild_id, enable_type, status) VALUES (?,?,?)" closeAfter {
                insert(guild.idLong, type.name, status).execute()
            }
    }

    enum class Type
    {
        ROLE_PERSIST, ANTI_ADS
    }
}