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
class SQLGlobalTags(val connection: Connection)
{
    companion object
    {
        private val global_tags = "global_tags"
        private val name = "name"
        private val owner_id = "owner_id"
        private val content = "content"
    }

    private val isTag              = "SELECT * FROM $global_tags WHERE LOWER($name) = LOWER(?)"
    private val addTag             = "INSERT INTO $global_tags ($name, $owner_id, $content) VALUES (?, ?, ?)"
    private val editTag            = "UPDATE $global_tags SET $content = ? WHERE LOWER($name) = LOWER(?) AND $owner_id = ?"
    private val deleteTag          = "DELETE FROM $global_tags WHERE LOWER($name) = LOWER(?) AND $owner_id = ?"
    private val getTagName         = "SELECT $name FROM $global_tags WHERE LOWER($name) = LOWER(?)"
    private val getTagContent      = "SELECT $content FROM $global_tags WHERE LOWER($name) = LOWER(?)"
    private val getTagOwnerId      = "SELECT $owner_id FROM $global_tags WHERE LOWER($name) = LOWER(?)"
    private val getAll             = "SELECT $name FROM $global_tags WHERE $owner_id = ?"

    fun isTag(name: String) : Boolean
    {
        try {
            connection.prepareStatement(isTag).use {
                it.setString(1, name)
                return it.executeQuery().use { it.next() }
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return false
        }
    }

    fun addTag(name: String, ownerId: Long, content: String)
    {
        try {
            connection.prepareStatement(addTag).use {
                it.setString(1, name)
                it.setLong(2, ownerId)
                it.setString(3, content)
                it.execute()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun editTag(newContent: String, name: String, ownerId: Long)
    {
        try {
            connection.prepareStatement(editTag).use {
                it.setString(1, newContent)
                it.setString(2, name)
                it.setLong(3, ownerId)
                it.execute()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun deleteTag(name: String, ownerId: Long)
    {
        try {
            connection.prepareStatement(deleteTag).use {
                it.setString(1, name)
                it.setLong(2, ownerId)
                it.execute()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun getOriginalName(name: String) : String
    {
        try {
            connection.prepareStatement(getTagName).use {
                it.setString(1, name)
                return it.executeQuery().use {
                    if(it.next()) it.getString(SQLGlobalTags.name)
                    else null
                }?:""
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return ""
        }
    }

    fun getTagContent(name: String) : String
    {
        try {
            connection.prepareStatement(getTagContent).use {
                it.setString(1, name)
                return it.executeQuery().use {
                    if(it.next()) it.getString(content)
                    else null
                }?:""
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return ""
        }
    }

    fun getTagOwnerId(name: String) : Long
    {
        try {
            connection.prepareStatement(getTagOwnerId).use {
                it.setString(1, name)
                return it.executeQuery().use {
                    if(it.next()) it.getLong(owner_id)
                    else null
                }?:0L
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return 0L
        }
    }

    fun getAllTags(userid: Long) : Set<String>
    {
        val names = HashSet<String>()
        try {
            connection.prepareStatement(getAll).use {
                it.setLong(1, userid)
                it.executeQuery().use {
                    while(it.next())
                        names.add(it.getString(name))
                }
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return names
    }
}

class SQLLocalTags(val connection: Connection)
{
    companion object
    {
        private val local_tags = "local_tags"
        private val name = "name"
        private val guild_id = "guild_id"
        private val owner_id = "owner_id"
        private val content = "content"
    }

    private val isTag              = "SELECT * FROM $local_tags WHERE LOWER($name) = LOWER(?) AND $guild_id = ?"
    private val addTag             = "INSERT INTO $local_tags ($name, $guild_id, $owner_id, $content) VALUES (?, ?, ?, ?)"
    private val editTag            = "UPDATE $local_tags SET $content = ? WHERE LOWER($name) = LOWER(?) AND $owner_id = ? AND $guild_id = ?"
    private val deleteTag          = "DELETE FROM $local_tags WHERE LOWER($name) = LOWER(?) AND $owner_id = ? AND $guild_id = ?"
    private val deleteAllTags      = "DELETE FROM $local_tags WHERE $guild_id = ?"
    private val getTagName         = "SELECT $name FROM $local_tags WHERE LOWER($name) = LOWER(?) AND $guild_id = ?"
    private val getTagContent      = "SELECT $content FROM $local_tags WHERE LOWER($name) = LOWER(?) AND $guild_id = ?"
    private val getTagOwnerId      = "SELECT $owner_id FROM $local_tags WHERE LOWER($name) = LOWER(?) AND $guild_id = ?"
    private val getAll             = "SELECT $name FROM $local_tags WHERE $owner_id = ? AND $guild_id = ?"

    private val overrideTag        = "UPDATE $local_tags SET $content = ?, $owner_id = ? " +
                                     "WHERE LOWER($name) = LOWER(?) AND $owner_id = ? AND $guild_id = ?"

    fun isTag(name: String, guild: Guild) : Boolean
    {
        try {
            val statement = connection.prepareStatement(isTag)
            statement.setString(1, name)
            statement.setLong(2, guild.idLong)
            val isTag = statement.executeQuery().use { it.next() }
            statement.close()
            return isTag
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return false
        }
    }

    fun addTag(name: String, ownerId: Long, content: String, guild: Guild)
    {
        try {
            with(connection.prepareStatement(addTag))
            {
                setString(1, name)
                setLong(2, guild.idLong)
                setLong(3, ownerId)
                setString(4, content)
                execute()
                close()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun editTag(newContent: String, name: String, ownerId: Long, guild: Guild)
    {
        try {
            with(connection.prepareStatement(editTag))
            {
                setString(1, newContent)
                setString(2, name)
                setLong(3, ownerId)
                setLong(4, guild.idLong)
                execute()
                close()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun deleteTag(name: String, ownerId: Long, guild: Guild)
    {
        try {
            with(connection.prepareStatement(deleteTag))
            {
                setString(1, name)
                setLong(2, ownerId)
                setLong(3, guild.idLong)
                execute()
                close()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun deleteAllTags(guild: Guild)
    {
        try {
            with(connection.prepareStatement(deleteAllTags))
            {
                setLong(1, guild.idLong)
                execute()
                close()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }

    fun getOriginalName(name: String, guild: Guild) : String
    {
        try {
            val statement = connection.prepareStatement(getTagName)
            statement.setString(1, name)
            statement.setLong(2, guild.idLong)
            val content = statement.executeQuery().use {
                if(it.next()) it.getString(SQLLocalTags.name)
                else null
            }
            statement.close()
            return content?:""
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return ""
        }
    }

    fun getTagContent(name: String, guild: Guild) : String
    {
        try {
            val statement = connection.prepareStatement(getTagContent)
            statement.setString(1, name)
            statement.setLong(2, guild.idLong)
            val content = statement.executeQuery().use {
                if(it.next()) it.getString(content)
                else null
            }
            statement.close()
            return content?:""
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return ""
        }
    }

    fun getTagOwnerId(name: String, guild: Guild) : Long
    {
        try {
            val statement = connection.prepareStatement(getTagOwnerId)
            statement.setString(1, name)
            statement.setLong(2, guild.idLong)
            val id = statement.executeQuery().use {
                if(it.next()) it.getLong(owner_id)
                else null
            }
            statement.close()
            return id?:0L
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
            return 0L
        }
    }

    fun getAllTags(userid: Long, guild: Guild) : Set<String>
    {
        val names = HashSet<String>()
        try {
            val statement = connection.prepareStatement(getAll)
            statement.setLong(1, userid)
            statement.setLong(2, guild.idLong)
            statement.executeQuery().use {
                while(it.next())
                    names.add(it.getString(name))
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
        return names
    }

    fun overrideTag(newContent: String, name: String, originalOwnerId: Long, guild: Guild)
    {
        try {
            with(connection.prepareStatement(overrideTag))
            {
                setString(1, newContent)
                setLong(2, 1L) // Overrides have an owner ID of 1L (likely won't have issues with this)
                setString(3, name)
                setLong(4, originalOwnerId)
                setLong(5, guild.idLong)
                execute()
                close()
            }
        } catch (e : SQLException) {
            SQL.LOG.warn(e)
        }
    }
}