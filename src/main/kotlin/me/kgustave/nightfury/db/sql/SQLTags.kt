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
class SQLGlobalTags(val connection: Connection)
{
    private val isTag              = "SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)"
    private val addTag             = "INSERT INTO global_tags (name, owner_id, content) VALUES (?, ?, ?)"
    private val editTag            = "UPDATE global_tags SET content = ? WHERE LOWER(name) = LOWER(?) AND owner_id = ?"
    private val deleteTag          = "DELETE FROM global_tags WHERE LOWER(name) = LOWER(?) AND owner_id = ?"
    private val getTagName         = "SELECT name FROM global_tags WHERE LOWER(name) = LOWER(?)"
    private val getTagContent      = "SELECT content FROM global_tags WHERE LOWER(name) = LOWER(?)"
    private val getTagOwnerId      = "SELECT owner_id FROM global_tags WHERE LOWER(name) = LOWER(?)"
    private val getAll             = "SELECT name FROM global_tags WHERE owner_id = ?"

    fun isTag(name: String) = try {
        connection prepare isTag closeAfter { insert(name) executeQuery { it.next() } }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        false
    }

    fun addTag(name: String, ownerId: Long, content: String) : Unit = try {
        connection prepare addTag closeAfter { insert(name, ownerId, content).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun editTag(newContent: String, name: String, ownerId: Long) : Unit = try {
        connection prepare editTag closeAfter { insert(newContent, name, ownerId).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun deleteTag(name: String, ownerId: Long) : Unit = try {
        connection prepare deleteTag closeAfter { insert(name, ownerId).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun getOriginalName(name: String) = try {
        connection prepare getTagName closeAfter {
            insert(name) executeQuery { if(it.next()) it.getString("name")?:"" else "" }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        ""
    }

    fun getTagContent(name: String) = try {
        connection prepare getTagContent closeAfter {
            insert(name) executeQuery { if(it.next()) it.getString("content")?:"" else "" }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        ""
    }

    fun getTagOwnerId(name: String) = try {
        connection prepare getTagOwnerId closeAfter {
            insert(name) executeQuery { if(it.next()) it.getLong("owner_id") else 0L }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        0L
    }

    fun getAllTags(userid: Long) : Set<String> {
        val names = HashSet<String>()
        try {
            connection prepare getAll closeAfter {
                insert(userid) executeQuery { while(it.next()) names.add(it.getString("name")) }
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return names
    }
}

/**
 * @author Kaidan Gustave
 */
@Suppress("RedundantUnitReturnType")
class SQLLocalTags(val connection: Connection)
{
    private val isTag              = "SELECT * FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val addTag             = "INSERT INTO local_tags (name, guild_id, owner_id, content) VALUES (?, ?, ?, ?)"
    private val editTag            = "UPDATE local_tags SET content = ? WHERE LOWER(name) = LOWER(?) AND owner_id = ? AND guild_id = ?"
    private val deleteTag          = "DELETE FROM local_tags WHERE LOWER(name) = LOWER(?) AND owner_id = ? AND guild_id = ?"
    private val deleteAllTags      = "DELETE FROM local_tags WHERE guild_id = ?"
    private val getTagName         = "SELECT name FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val getTagContent      = "SELECT content FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val getTagOwnerId      = "SELECT owner_id FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?"
    private val getAll             = "SELECT name FROM local_tags WHERE owner_id = ? AND guild_id = ?"

    private val overrideTag        = "UPDATE local_tags SET content = ?, owner_id = ? " +
                                     "WHERE LOWER(name) = LOWER(?) AND owner_id = ? AND guild_id = ?"

    fun isTag(name: String, guild: Guild) = try {
        connection prepare isTag closeAfter { insert(name, guild.idLong) executeQuery { it.next() } }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        false
    }

    fun addTag(name: String, ownerId: Long, content: String, guild: Guild) : Unit = try {
        connection prepare addTag closeAfter { insert(name, guild.idLong, ownerId, content).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun editTag(newContent: String, name: String, ownerId: Long, guild: Guild) : Unit = try {
        connection prepare editTag closeAfter { insert(newContent, name, ownerId, guild.idLong).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun deleteTag(name: String, ownerId: Long, guild: Guild) : Unit = try {
        connection prepare deleteTag closeAfter { insert(name, ownerId, guild.idLong).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun deleteAllTags(guild: Guild) : Unit = try {
        connection prepare deleteAllTags closeAfter { insert(guild.idLong).execute() }
    } catch (e : SQLException) { SQL.LOG.warn(e) }

    fun getOriginalName(name: String, guild: Guild) = try {
        connection prepare getTagName closeAfter {
            insert(name, guild.idLong) executeQuery { if(it.next()) it.getString("name")?:"" else "" }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        ""
    }

    fun getTagContent(name: String, guild: Guild) = try {
        connection prepare getTagContent closeAfter {
            insert(name, guild.idLong) executeQuery { if(it.next()) it.getString("content")?:"" else "" }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        ""
    }

    fun getTagOwnerId(name: String, guild: Guild) = try {
        connection prepare getTagOwnerId closeAfter {
            insert(name, guild.idLong) executeQuery { if(it.next()) it.getLong("owner_id") else 0L }
        }
    } catch (e : SQLException) {
        SQL.LOG.warn(e)
        0L
    }

    fun getAllTags(guild: Guild) : Set<String> {
        val names = HashSet<String>()
        try {
            connection prepare "SELECT names FROM local_tags WHERE guild_id = ?" closeAfter {
                insert(guild.idLong) executeQuery { while(it.next()) names.add(it.getString("name")) }
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return names
    }

    fun getAllTags(userid: Long, guild: Guild) : Set<String> {
        val names = HashSet<String>()
        try {
            connection prepare getAll closeAfter {
                insert(userid, guild.idLong) executeQuery { while(it.next()) names.add(it.getString("name")) }
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return names
    }

    fun overrideTag(newContent: String, name: String, originalOwnerId: Long, guild: Guild) : Unit = try {
        connection prepare overrideTag closeAfter {
            // Overrides have an owner ID of 1L (likely won't have issues with this)
            insert(newContent, 1L, name, originalOwnerId, guild.idLong).execute()
        }
    } catch (e : SQLException) { SQL.LOG.warn(e) }
}