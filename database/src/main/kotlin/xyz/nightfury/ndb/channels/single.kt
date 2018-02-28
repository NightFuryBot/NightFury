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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.nightfury.ndb.channels

import xyz.nightfury.ndb.*

/**
 * @author Kaidan Gustave
 */
abstract class SingleChannelHandler(type: DBChannelType): Database.Table() {
    init {
        require(!type.isMulti) { "Singleton channel DB handler cannot be created for multi-type '$type'" }
    }

    private val getChannel    = "SELECT CHANNEL_ID FROM GUILD_CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val addChannel    = "INSERT INTO GUILD_CHANNELS(GUILD_ID, CHANNEL_ID, TYPE) VALUES (?, ?, '$type')"
    private val setChannel    = "UPDATE GUILD_CHANNELS SET CHANNEL_ID = ? WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val removeChannel = "DELETE FROM GUILD_CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"

    fun hasChannel(guildId: Long): Boolean = sql(false) {
        any(getChannel) {
            this[1] = guildId
        }
    }

    fun getChannel(guildId: Long): Long = sql(0L) {
        statement(getChannel) {
            this[1] = guildId
            query(0L) { it.getLong("CHANNEL_ID") }
        }
    }

    fun setChannel(guildId: Long, channelId: Long) = sql {

        if(hasChannel(guildId)) {
            execute(setChannel) {
                this[1] = channelId
                this[2] = guildId
            }
        } else {
            execute(addChannel) {
                this[1] = guildId
                this[2] = channelId
            }
        }
    }

    fun removeChannel(guildId: Long) = sql {
        execute(removeChannel) {
            this[1] = guildId
        }
    }
}

object ModLogHandler : SingleChannelHandler(DBChannelType.MOD_LOG)
object AnnouncementChannelHandler : SingleChannelHandler(DBChannelType.ANNOUNCEMENTS)
