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
package xyz.nightfury.ndb.starboard

import xyz.nightfury.ndb.Database
import xyz.nightfury.ndb.internal.*

/**
 * @author Kaidan Gustave
 */
object StarboardEntriesHandler : Database.Table() {
    private const val ADD_STAR = "INSERT INTO STARBOARD_ENTRIES(STARRED_ID, ENTRY_ID, STARBOARD_ID, GUILD_ID, USER_ID) VALUES (?,?,?,?,?)"
    private const val FIND_ENTRY_ID = "SELECT ENTRY_ID FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ? AND ENTRY_ID IS NOT NULL"
    private const val GET_STARS = "SELECT USER_ID FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?"
    private const val IS_STARRING = "SELECT * FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND USER_ID = ? AND GUILD_ID = ?"
    private const val GET_STAR_COUNT = "SELECT * FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?"
    private const val SET_ENTRY = "UPDATE STARBOARD_ENTRIES SET ENTRY_ID = ? WHERE STARRED_ID = ? AND GUILD_ID = ?"
    private const val REMOVE_ENTRY = "DELETE FROM STARBOARD_ENTRIES WHERE USER_ID = ? AND STARRED_ID = ? AND GUILD_ID = ?"
    private const val REMOVE_ALL_ENTRIES = "DELETE FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?"
    private const val CLEAR_ENTRIES = "DELETE FROM STARBOARD_ENTRIES WHERE GUILD_ID = ?"


    fun addStar(starredId: Long, guildId: Long, userId: Long) = sql {
        val entryId = statement(FIND_ENTRY_ID) {
            this[1] = starredId
            this[2] = guildId
            query { it.getLong("ENTRY_ID") }
        }
        val starboardId = StarboardSettingsHandler.getChannel(guildId)

        execute(ADD_STAR) {
            this[1] = starredId
            this[2] = entryId
            this[3] = starboardId
            this[4] = guildId
            this[5] = userId
        }
    }

    fun getStars(starredId: Long, guildId: Long): List<Pair<Long, Long>> {
        val list = ArrayList<Pair<Long, Long>>()
        sql {
            statement(GET_STARS) {
                this[1] = starredId
                this[2] = guildId
                queryAll { list += it.getLong("MESSAGE_ID") to it.getLong("USER_ID") }
            }
        }
        return list
    }

    fun isStarring(starredId: Long, userId: Long, guildId: Long): Boolean = sql(false) {
        any(IS_STARRING) {
            this[1] = starredId
            this[2] = userId
            this[3] = guildId
        }
    }

    fun getStarCount(starredId: Long, guildId: Long): Int {
        var count = 0
        sql {
            statement(GET_STAR_COUNT) {
                this[1] = starredId
                this[2] = guildId
                queryAll { count++ }
            }
        }
        return count
    }

    fun setEntry(entryId: Long, starredId: Long, guildId: Long) = sql {
        execute(SET_ENTRY) {
            this[1] = entryId
            this[2] = starredId
            this[3] = guildId
        }
    }

    fun removeEntry(userId: Long, starredId: Long, guildId: Long) = sql {
        execute(REMOVE_ENTRY) {
            this[1] = userId
            this[2] = starredId
            this[3] = guildId
        }
    }

    fun removeAllEntries(starredId: Long, guildId: Long) = sql {
        execute(REMOVE_ALL_ENTRIES) {
            this[1] = starredId
            this[2] = guildId
        }
    }

    fun clearEntries(guildId: Long) = sql {
        execute(CLEAR_ENTRIES) {
            this[1] = guildId
        }
    }
}