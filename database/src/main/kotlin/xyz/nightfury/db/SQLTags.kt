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
@file:Suppress("Unused")
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
interface ISQLGlobalTags {
    fun isTag(name: String): Boolean
    fun addTag(name: String, ownerId: Long, content: String)
    fun editTag(newContent: String, name: String, ownerId: Long)
    fun deleteTag(name: String, ownerId: Long)
    fun getOriginalName(name: String): String
    fun getTagContent(name: String): String
    fun getTagOwnerId(name: String): Long
    fun getAllTags(userId: Long): Set<String>
}

/**
 * @author Kaidan Gustave
 */
object SQLGlobalTags : Table(), ISQLGlobalTags {
    private const val isTag         = "SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private const val addTag        = "INSERT INTO GLOBAL_TAGS (NAME, OWNER_ID, CONTENT) VALUES (?, ?, ?)"
    private const val editTag       = "UPDATE GLOBAL_TAGS SET CONTENT = ? WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ?"
    private const val deleteTag     = "DELETE FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ?"
    private const val getTagName    = "SELECT NAME FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private const val getTagContent = "SELECT CONTENT FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private const val getTagOwnerId = "SELECT OWNER_ID FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private const val getAll        = "SELECT NAME FROM GLOBAL_TAGS WHERE OWNER_ID = ?"

    override fun isTag(name: String): Boolean {
        return using(connection.prepareStatement(isTag), default = false) {
            this[1] = name
            using(executeQuery()) { next() }
        }
    }

    override fun addTag(name: String, ownerId: Long, content: String) {
        using(connection.prepareStatement(addTag)) {
            this[1] = name
            this[2] = ownerId
            this[3] = content
            execute()
        }
    }

    override fun editTag(newContent: String, name: String, ownerId: Long) {
        using(connection.prepareStatement(editTag)) {
            this[1] = newContent
            this[2] = name
            this[3] = ownerId
            execute()
        }
    }

    override fun deleteTag(name: String, ownerId: Long) {
        using(connection.prepareStatement(deleteTag)) {
            this[1] = name
            this[2] = ownerId
            execute()
        }
    }

    override fun getOriginalName(name: String): String {
        return using(connection.prepareStatement(getTagName), default = "") {
            this[1] = name
            using(executeQuery()) { if(next()) getString("NAME") ?: "" else "" }
        }
    }

    override fun getTagContent(name: String): String {
        return using(connection.prepareStatement(getTagContent), default = "") {
            this[1] = name
            using(executeQuery()) { if(next()) getString("CONTENT") ?: "" else "" }
        }
    }

    override fun getTagOwnerId(name: String): Long {
        return using(connection.prepareStatement(getTagOwnerId), default = 0L) {
            this[1] = name
            using(executeQuery()) { if(next()) getLong("OWNER_ID") else 0L }
        }
    }

    override fun getAllTags(userId: Long): Set<String> {
        val names = HashSet<String>()
        using(connection.prepareStatement(getAll)) {
            this[1] = userId
            using(executeQuery()) {
                while(next()) names += getString("NAME")
            }
        }
        return names
    }
}

/**
 * @author Kaidan Gustave
 */
interface ISQLLocalTags {
    fun isTag(name: String, guildId: Long): Boolean
    fun addTag(name: String, ownerId: Long, content: String, guildId: Long)
    fun editTag(newContent: String, name: String, ownerId: Long, guildId: Long)
    fun deleteTag(name: String, ownerId: Long, guildId: Long)
    fun getOriginalName(name: String, guildId: Long): String
    fun getTagContent(name: String, guildId: Long): String
    fun getTagOwnerId(name: String, guildId: Long): Long
    fun getAllTags(guildId: Long): Set<String>
    fun getAllTags(userId: Long, guildId: Long): Set<String>
    fun overrideTag(newContent: String, name: String, originalOwnerId: Long, guildId: Long)
}

/**
 * @author Kaidan Gustave
 */
object SQLLocalTags : Table(), ISQLLocalTags {
    private const val isTag          = "SELECT * FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private const val addTag         = "INSERT INTO LOCAL_TAGS (NAME, GUILD_ID, OWNER_ID, CONTENT) VALUES (?, ?, ?, ?)"
    private const val editTag        = "UPDATE LOCAL_TAGS SET CONTENT = ? WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"
    private const val deleteTag      = "DELETE FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"
    private const val getTagName     = "SELECT NAME FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private const val getTagContent  = "SELECT CONTENT FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private const val getTagOwnerId  = "SELECT OWNER_ID FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private const val getAllForGuild = "SELECT NAME FROM LOCAL_TAGS WHERE GUILD_ID = ?"
    private const val getAll         = "SELECT NAME FROM LOCAL_TAGS WHERE OWNER_ID = ? AND GUILD_ID = ?"
    private const val overrideTag    = "UPDATE LOCAL_TAGS SET CONTENT = ?, OWNER_ID = ? WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"

    override fun isTag(name: String, guildId: Long): Boolean {
        return using(connection.prepareStatement(isTag), default = false) {
            this[1] = name
            this[2] = guildId
            using(executeQuery()) { next() }
        }
    }

    fun isTag(name: String, guild: Guild): Boolean {
        return isTag(name, guild.idLong)
    }

    override fun addTag(name: String, ownerId: Long, content: String, guildId: Long) {
        using(connection.prepareStatement(addTag)) {
            this[1] = name
            this[2] = guildId
            this[3] = ownerId
            this[4] = content
            execute()
        }
    }

    fun addTag(name: String, ownerId: Long, content: String, guild: Guild) {
        addTag(name, ownerId, content, guild.idLong)
    }

    override fun editTag(newContent: String, name: String, ownerId: Long, guildId: Long) {
        using(connection.prepareStatement(editTag)) {
            this[1] = newContent
            this[2] = name
            this[3] = ownerId
            this[4] = guildId
            execute()
        }
    }

    fun editTag(newContent: String, name: String, ownerId: Long, guild: Guild) {
        editTag(newContent, name, ownerId, guild.idLong)
    }

    override fun deleteTag(name: String, ownerId: Long, guildId: Long) {
        using(connection.prepareStatement(deleteTag)) {
            this[1] = name
            this[2] = ownerId
            this[3] = guildId
            execute()
        }
    }

    fun deleteTag(name: String, ownerId: Long, guild: Guild) {
        deleteTag(name, ownerId, guild.idLong)
    }

    override fun getOriginalName(name: String, guildId: Long): String {
        return using(connection.prepareStatement(getTagName), default = "") {
            this[1] = name
            this[2] = guildId
            using(executeQuery()) { if(next()) getString("NAME") ?: "" else "" }
        }
    }

    fun getOriginalName(name: String, guild: Guild): String {
        return getOriginalName(name, guild.idLong)
    }

    override fun getTagContent(name: String, guildId: Long): String {
        return using(connection.prepareStatement(getTagContent), default = "") {
            this[1] = name
            this[2] = guildId
            using(executeQuery()) { if(next()) getString("CONTENT") ?: "" else "" }
        }
    }

    fun getTagContent(name: String, guild: Guild): String {
        return getTagContent(name, guild.idLong)
    }

    override fun getTagOwnerId(name: String, guildId: Long): Long {
        return using(connection.prepareStatement(getTagOwnerId), default = 0L) {
            this[1] = name
            this[2] = guildId
            using(executeQuery()) { if(next()) getLong("OWNER_ID") else 0L }
        }
    }

    fun getTagOwnerId(name: String, guild: Guild): Long {
        return getTagOwnerId(name, guild.idLong)
    }

    override fun getAllTags(guildId: Long): Set<String> {
        val names = HashSet<String>()
        using(connection.prepareStatement(getAllForGuild)) {
            this[1] = guildId
            using(executeQuery()) {
                while(next()) names += getString("NAME")
            }
        }
        return names
    }

    fun getAllTags(guild: Guild): Set<String> {
        return getAllTags(guild.idLong)
    }

    override fun getAllTags(userId: Long, guildId: Long): Set<String> {
        val names = HashSet<String>()
        using(connection.prepareStatement(getAll)) {
            this[1] = userId
            this[2] = guildId
            using(executeQuery()) {
                while(next()) names += getString("NAME")
            }
        }
        return names
    }

    fun getAllTags(userId: Long, guild: Guild): Set<String> {
        return getAllTags(userId, guild.idLong)
    }

    override fun overrideTag(newContent: String, name: String, originalOwnerId: Long, guildId: Long) {
        using(connection.prepareStatement(overrideTag)) {
            this[1] = newContent
            this[2] = 1L
            this[3] = name
            this[4] = originalOwnerId
            this[5] = guildId
            execute()
        }
    }

    fun overrideTag(newContent: String, name: String, originalOwnerId: Long, guild: Guild) {
        overrideTag(newContent, name, originalOwnerId, guild.idLong)
    }
}
