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
@file:Suppress("Unused")
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
object SQLGlobalTags : Table() {
    private val isTag         = "SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private val addTag        = "INSERT INTO GLOBAL_TAGS (NAME, OWNER_ID, CONTENT) VALUES (?, ?, ?)"
    private val editTag       = "UPDATE GLOBAL_TAGS SET CONTENT = ? WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ?"
    private val deleteTag     = "DELETE FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ?"
    private val getTagName    = "SELECT NAME FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private val getTagContent = "SELECT CONTENT FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private val getTagOwnerId = "SELECT OWNER_ID FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)"
    private val getAll        = "SELECT NAME FROM GLOBAL_TAGS WHERE OWNER_ID = ?"

    fun isTag(name: String): Boolean {
        return using(connection.prepareStatement(isTag), default = false) {
            this[1] = name
            using(executeQuery()) { next() }
        }
    }

    fun addTag(name: String, ownerId: Long, content: String) {
        using(connection.prepareStatement(addTag)) {
            this[1] = name
            this[2] = ownerId
            this[3] = content
            execute()
        }
    }

    fun editTag(newContent: String, name: String, ownerId: Long) {
        using(connection.prepareStatement(editTag)) {
            this[1] = newContent
            this[2] = name
            this[3] = ownerId
            execute()
        }
    }

    fun deleteTag(name: String, ownerId: Long) {
        using(connection.prepareStatement(deleteTag)) {
            this[1] = name
            this[2] = ownerId
            execute()
        }
    }

    fun getOriginalName(name: String): String {
        return using(connection.prepareStatement(getTagName), default = "") {
            this[1] = name
            using(executeQuery()) { if(next()) getString("NAME")?: "" else "" }
        }
    }

    fun getTagContent(name: String): String {
        return using(connection.prepareStatement(getTagContent), default = "") {
            this[1] = name
            using(executeQuery()) { if(next()) getString("CONTENT")?:"" else "" }
        }
    }

    fun getTagOwnerId(name: String): Long {
        return using(connection.prepareStatement(getTagOwnerId), default = 0L) {
            this[1] = name
            using(executeQuery()) { if(next()) getLong("OWNER_ID") else 0L }
        }
    }

    fun getAllTags(userId: Long) : Set<String> {
        val names = HashSet<String>()
        using(connection.prepareStatement(getAll)) {
            this[1] = userId
            using(executeQuery()) {
                while(next())
                    names += getString("NAME")
            }
        }
        return names
    }
}

/**
 * @author Kaidan Gustave
 */
object SQLLocalTags : Table() {
    private val isTag          = "SELECT * FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private val addTag         = "INSERT INTO LOCAL_TAGS (NAME, GUILD_ID, OWNER_ID, CONTENT) VALUES (?, ?, ?, ?)"
    private val editTag        = "UPDATE LOCAL_TAGS SET CONTENT = ? WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"
    private val deleteTag      = "DELETE FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"
    private val getTagName     = "SELECT NAME FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private val getTagContent  = "SELECT CONTENT FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private val getTagOwnerId  = "SELECT OWNER_ID FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private val getAllForGuild = "SELECT NAME FROM LOCAL_TAGS WHERE GUILD_ID = ?"
    private val getAll         = "SELECT NAME FROM LOCAL_TAGS WHERE OWNER_ID = ? AND GUILD_ID = ?"
    private val overrideTag    = "UPDATE LOCAL_TAGS SET CONTENT = ?, OWNER_ID = ? WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"

    fun isTag(name: String, guild: Guild): Boolean {
        return using(connection.prepareStatement(isTag), default = false) {
            this[1] = name
            this[2] = guild.idLong
            using(executeQuery()) { next() }
        }
    }

    fun addTag(name: String, ownerId: Long, content: String, guild: Guild) {
        using(connection.prepareStatement(addTag)) {
            this[1] = name
            this[2] = guild.idLong
            this[3] = ownerId
            this[4] = content
            execute()
        }
    }

    fun editTag(newContent: String, name: String, ownerId: Long, guild: Guild) {
        using(connection.prepareStatement(editTag)) {
            this[1] = newContent
            this[2] = name
            this[3] = ownerId
            this[4] = guild.idLong
            execute()
        }
    }

    fun deleteTag(name: String, ownerId: Long, guild: Guild) {
        using(connection.prepareStatement(deleteTag)) {
            this[1] = name
            this[2] = ownerId
            this[3] = guild.idLong
            execute()
        }
    }

    fun getOriginalName(name: String, guild: Guild): String {
        return using(connection.prepareStatement(getTagName), default = "") {
            this[1] = name
            this[2] = guild.idLong
            using(executeQuery()) { if(next()) getString("NAME")?:"" else "" }
        }
    }

    fun getTagContent(name: String, guild: Guild): String {
        return using(connection.prepareStatement(getTagContent), default = "") {
            this[1] = name
            this[2] = guild.idLong
            using(executeQuery()) { if(next()) getString("CONTENT")?:"" else "" }
        }
    }

    fun getTagOwnerId(name: String, guild: Guild): Long {
        return using(connection.prepareStatement(getTagOwnerId), default = 0L) {
            this[1] = name
            this[2] = guild.idLong
            using(executeQuery()) { if(next()) getLong("OWNER_ID") else 0L }
        }
    }

    @Suppress("UNUSED")
    fun getAllTags(guild: Guild) : Set<String> {
        val names = HashSet<String>()
        using(connection.prepareStatement(getAllForGuild)) {
            this[1] = guild.idLong
            using(executeQuery()) {
                while(next())
                    names += getString("NAME")
            }
        }
        return names
    }

    fun getAllTags(userId: Long, guild: Guild) : Set<String> {
        val names = HashSet<String>()
        using(connection.prepareStatement(getAll)) {
            this[1] = userId
            this[2] = guild.idLong
            using(executeQuery()) {
                while(next())
                    names += getString("NAME")
            }
        }
        return names
    }

    fun overrideTag(newContent: String, name: String, originalOwnerId: Long, guild: Guild) {
        using(connection.prepareStatement(overrideTag)) {
            this[1] = newContent
            this[2] = 1L
            this[3] = name
            this[4] = originalOwnerId
            this[5] = guild.idLong
            execute()
        }
    }
}