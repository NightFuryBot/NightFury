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

import xyz.nightfury.ndb.*
import xyz.nightfury.ndb.channels.DBChannelType
import xyz.nightfury.ndb.channels.SingleChannelHandler
import xyz.nightfury.ndb.entities.StarboardSettings

/**
 * @author Kaidan Gustave
 */
object StarboardSettingsHandler : SingleChannelHandler(DBChannelType.STARBOARD) {
    private const val CREATE_SETTINGS = "INSERT INTO STARBOARD_SETTINGS(GUILD_ID, THRESHOLD, MAX_AGE) VALUES (?, ?, ?)"
    private const val GET_SETTINGS    = "SELECT * FROM STARBOARD_SETTINGS WHERE GUILD_ID = ?"
    private const val REMOVE_SETTINGS = "DELETE FROM STARBOARD_SETTINGS WHERE GUILD_ID = ?"
    private const val UPDATE_SETTINGS = "UPDATE STARBOARD_SETTINGS SET THRESHOLD = ? AND MAX_AGE = ? WHERE GUILD_ID = ?"

    // this also has a joint row-type in SQLChannels

    fun createSettings(settings: StarboardSettings) {
        sql {
            execute(CREATE_SETTINGS) {
                this[1] = settings.guildId
                this[2] = settings.threshold
                this[3] = settings.maxAge
            }
        }
        setChannel(settings.guildId, settings.channelId)
    }

    fun hasSettings(guildId: Long): Boolean = sql(false) {
        any(GET_SETTINGS) {
            this[1] = guildId
        }
    }

    fun removeSettings(guildId: Long) {
        sql {
            execute(REMOVE_SETTINGS) {
                this[1] = guildId
            }
        }
        removeChannel(guildId)
        // kill all entries
        StarboardEntriesHandler.clearEntries(guildId)
    }

    fun getSettings(guildId: Long): StarboardSettings? {
        val channelId = getChannel(guildId)
        if(channelId == 0L) {
            return null
        }
        return sql {
            statement(GET_SETTINGS) {
                this[1] = guildId
                query {
                    StarboardSettings(it.getLong("GUILD_ID"), channelId, it.getInt("THRESHOLD"), it.getInt("MAX_AGE"))
                }
            }
        }
    }

    fun updateSettings(settings: StarboardSettings) {
        sql {
            execute(UPDATE_SETTINGS) {
                this[1] = settings.threshold
                this[2] = settings.maxAge
                this[3] = settings.guildId
            }
        }
        setChannel(settings.guildId, settings.channelId)
    }
}