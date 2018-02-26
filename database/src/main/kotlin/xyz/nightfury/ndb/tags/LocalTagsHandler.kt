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
package xyz.nightfury.ndb.tags

import xyz.nightfury.ndb.Database
import xyz.nightfury.ndb.entities.DBTag
import xyz.nightfury.ndb.entities.Tag
import xyz.nightfury.ndb.internal.*
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
object LocalTagsHandler : Database.Table() {
    private const val GET_TAGS = "SELECT * FROM LOCAL_TAGS WHERE GUILD_ID = ?"
    private const val GET_USER_TAGS = "SELECT * FROM LOCAL_TAGS WHERE GUILD_ID = ? AND OWNER_ID = ?"
    private const val GET_TAG = "SELECT * FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private const val ADD_TAG = "INSERT INTO LOCAL_TAGS (NAME, GUILD_ID, OWNER_ID, CONTENT) VALUES (?, ?, ?, ?)"
    private const val EDIT_TAG = "UPDATE LOCAL_TAGS SET CONTENT = ?, OWNER_ID = ? WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private const val REMOVE_TAG = "DELETE FROM LOCAL_TAGS WHERE LOWER(NAME) = LOWER(?) AND OWNER_ID = ? AND GUILD_ID = ?"

    private inline val <reified R: ResultSet> R.tag: DBTag inline get() {
        return DBTag(getString("NAME"), getString("CONTENT"), getLong("OWNER_ID"), getLong("GUILD_ID"))
    }

    fun isTag(name: String, guildId: Long): Boolean = sql(false) {
        any(GET_TAG) {
            this[1] = name
            this[2] = guildId
        }
    }

    fun getTags(guildId: Long): List<Tag> = sql({ emptyList() }) {
        val tags = ArrayList<Tag>()
        statement(GET_TAGS) {
            this[1] = guildId
            queryAll { tags += it.tag }
        }
        return tags
    }

    fun getTags(guildId: Long, userId: Long): List<Tag> = sql({ emptyList() }) {
        val tags = ArrayList<Tag>()
        statement(GET_USER_TAGS) {
            this[1] = guildId
            this[2] = userId
            queryAll { tags += it.tag }
        }
        return tags
    }

    fun getTagByName(name: String, guildId: Long): Tag? = sql {
        statement(GET_TAG) {
            this[1] = name
            this[2] = guildId
            query { it.tag }
        }
    }

    fun addTag(tag: Tag) = sql {
        execute(ADD_TAG) {
            this[1] = tag.name
            this[2] = tag.guildId
            this[3] = tag.ownerId
            this[4] = tag.content
        }
    }

    fun editTag(tag: Tag) = sql {
        execute(EDIT_TAG) {
            this[1] = tag.content
            this[2] = tag.ownerId
            this[3] = tag.name
            this[4] = requireNotNull(tag.guildId) { "Tried to update a local tag with a null guild ID" }
        }
    }

    fun removeTag(tag: Tag) = sql {
        execute(REMOVE_TAG) {
            this[1] = tag.name
            this[2] = tag.ownerId
            this[3] = tag.guildId
        }
    }
}