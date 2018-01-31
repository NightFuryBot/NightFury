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
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.*
import xyz.nightfury.entities.starboard.IStarMessage
import xyz.nightfury.entities.starboard.StarReaction

/**
 * @author Kaidan Gustave
 */
object SQLStarboardEntries : Table() {

    /*
     * schema STARBOARD_ENTRIES
     * col STARRED_ID BIGINT (the starred message id)
     * col ENTRY_ID BIGINT DEFAULT NULL (the starboard entry message id)
     * col STARBOARD_ID BIGINT (the starboard channel id)
     * col GUILD_ID BIGINT (the guild id)
     * col USER_ID BIGINT (the user id)
     *
     * Additional schema functions:
     * - We can get the number of stars simply by querying the number of rows where STARRED_ID = ?
     * - Starboard channel info will be stored as a SQLChannel type object.
     */

    fun addStar(message: Message, user: User) {
        val starredId = message.idLong
        val guildId = message.guild.idLong
        val entryId = findEntryId(starredId, guildId)
        val starboardId = SQLStarboardSettings.getChannel(message.guild)!!.idLong
        val userId = user.idLong

        using(connection.prepareStatement("INSERT INTO STARBOARD_ENTRIES(STARRED_ID, ENTRY_ID, STARBOARD_ID, GUILD_ID, USER_ID) VALUES (?,?,?,?,?)")) {
            this[1] = starredId
            this[2] = entryId
            this[3] = starboardId
            this[4] = guildId
            this[5] = userId
            execute()
        }
    }

    fun getStars(message: IStarMessage<*,*>): ArrayList<StarReaction> {
        val list = ArrayList<StarReaction>()

        using(connection.prepareStatement("SELECT USER_ID FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?")) {
            this[1] = message.starred.idLong
            this[2] = message.starred.guild.idLong

            using(executeQuery()) {
                while(next()) {
                    list += StarReaction(getLong("USER_ID"), message)
                }
            }
        }

        return list
    }

    fun getStarCount(message: IStarMessage<*,*>): Int {
        var count = 0

        using(connection.prepareStatement("SELECT * FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?")) {
            this[1] = message.starred.idLong
            this[2] = message.starred.guild.idLong

            using(executeQuery()) {
                while(next()) {
                    count++
                }
            }
        }

        return count
    }

    fun isStarring(message: Message, user: User): Boolean {
        val guild = message.guild
        val guildId = guild.idLong
        val starredId = message.idLong
        val userId = user.idLong

        return using(connection.prepareStatement("SELECT * FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND USER_ID = ? AND GUILD_ID = ?"), false) {
            this[1] = starredId
            this[2] = userId
            this[3] = guildId

            using(executeQuery(), default = false) { next() }
        }
    }

    fun setEntry(message: IStarMessage<*,*>) {
        require(message.entryIsCreated) { "An entry was not created" }

        using(connection.prepareStatement("UPDATE STARBOARD_ENTRIES SET ENTRY_ID = ? WHERE STARRED_ID = ? AND GUILD_ID = ?")) {
            this[1] = message.entry.idLong
            this[2] = message.starred.idLong
            this[3] = message.starred.guild.idLong
            execute()
        }
    }

    fun removeEntry(user: User, message: IStarMessage<*,*>) {
        using(connection.prepareStatement("DELETE FROM STARBOARD_ENTRIES WHERE USER_ID = ? AND GUILD_ID = ? AND STARRED_ID = ?")) {
            this[1] = user.idLong
            this[2] = message.starred.guild.idLong
            this[3] = message.starred.idLong
            execute()
        }
    }

    fun removeAllEntries(message: IStarMessage<*,*>) {
        using(connection.prepareStatement("DELETE FROM STARBOARD_ENTRIES WHERE GUILD_ID = ? AND STARRED_ID = ?")) {
            this[1] = message.starred.guild.idLong
            this[2] = message.starred.idLong
            execute()
        }
    }

    fun clearAllEntries(guild: Guild) {
        using(connection.prepareStatement("DELETE FROM STARBOARD_ENTRIES WHERE GUILD_ID = ?")) {
            this[1] = guild.idLong
            execute()
        }
    }

    private fun findEntryId(starredId: Long, guildId: Long): Long? {
        return using(connection.prepareStatement("SELECT ENTRY_ID FROM STARBOARD_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?")) {
            this[1] = starredId
            this[2] = guildId
            using(executeQuery()) { if(next()) getLong("ENTRY_ID") else null }
        }
    }
}

object SQLStarboardSettings : SQLChannel("starboard") {

    /*
     * schema STARBOARD_SETTINGS
     * col THRESHOLD INT DEFAULT 5 (number of reactions to create starboard entry)
     * col MAX_AGE INT DEFAULT 72 (in hours, default is 3 days)
     * col GUILD_ID BIGINT
     */

    // this also has a joint row-type in SQLChannels

    fun createSettingsFor(channel: TextChannel) {
        using(connection.prepareStatement("INSERT INTO STARBOARD_SETTINGS(GUILD_ID) VALUES (?)")) {
            this[1] = channel.guild.idLong
            execute()
        }
        setChannel(channel)
    }

    fun hasSettingsFor(guild: Guild): Boolean {
        return using(connection.prepareStatement("SELECT * FROM STARBOARD_SETTINGS WHERE GUILD_ID = ?"), false) {
            this[1] = guild.idLong

            using(executeQuery(), default = false) { next() }
        }
    }

    fun deleteSettingsFor(guild: Guild) {
        using(connection.prepareStatement("DELETE FROM STARBOARD_SETTINGS WHERE GUILD_ID = ?")) {
            this[1] = guild.idLong
            execute()
        }
        deleteChannel(guild)
        // kill all entries
        SQLStarboardEntries.clearAllEntries(guild)
    }

    fun getThresholdFor(guild: Guild): Int? {
        return using(connection.prepareStatement("SELECT THRESHOLD FROM STARBOARD_SETTINGS WHERE GUILD_ID = ?")) {
            this[1] = guild.idLong
            using(executeQuery()) { if(next()) getInt("THRESHOLD") else null }
        }
    }

    fun setThresholdFor(guild: Guild, threshold: Int) {
        using(connection.prepareStatement("UPDATE STARBOARD_SETTINGS SET THRESHOLD = ? WHERE GUILD_ID = ?")) {
            this[1] = threshold
            this[2] = guild.idLong
            execute()
        }
    }

    fun getMaxAgeFor(guild: Guild): Int? {
        return using(connection.prepareStatement("SELECT MAX_AGE FROM STARBOARD_SETTINGS WHERE GUILD_ID = ?")) {
            this[1] = guild.idLong
            using(executeQuery()) { if(next()) getInt("MAX_AGE") else null }
        }
    }

    fun setMaxAgeFor(guild: Guild, maxAge: Int) {
        using(connection.prepareStatement("UPDATE STARBOARD_SETTINGS SET MAX_AGE = ? WHERE GUILD_ID = ?")) {
            this[1] = maxAge
            this[2] = guild.idLong
            execute()
        }
    }
}
