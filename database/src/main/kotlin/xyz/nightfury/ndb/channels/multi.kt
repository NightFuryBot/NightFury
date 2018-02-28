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
package xyz.nightfury.ndb.channels

import xyz.nightfury.ndb.*

/**
 * @author Kaidan Gustave
 */
abstract class MultiChannelHandler(type: DBChannelType): Database.Table() {
    init {
        require(type.isMulti) { "Singleton channel DB handler cannot be created for multi-type '$type'" }
    }

    private val isChannel     = "SELECT * FROM GUILD_CHANNELS WHERE GUILD_ID = ? AND CHANNEL_ID = ? AND TYPE = '$type'"
    private val getChannel    = "SELECT CHANNEL_ID FROM GUILD_CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val addChannel    = "INSERT INTO GUILD_CHANNELS(GUILD_ID, CHANNEL_ID, TYPE) VALUES (?, ?, '$type')"
    private val removeChannel = "DELETE FROM GUILD_CHANNELS WHERE GUILD_ID = ? AND CHANNEL_ID = ? AND TYPE = '$type'"

    fun isChannel(guildId: Long, channelId: Long): Boolean = sql(false) {
        any(isChannel) {
            this[1] = guildId
            this[2] = channelId
        }
    }

    fun getChannels(guildId: Long): List<Long> = sql({ emptyList() }) {
        val channels = ArrayList<Long>()
        statement(getChannel) {
            this[1] = guildId
            queryAll { channels += it.get<Long>("CHANNEL_ID")!! }
        }
        return channels
    }

    fun addChannel(guildId: Long, channelId: Long) = sql {
        execute(addChannel) {
            this[1] = guildId
            this[2] = channelId
        }
    }

    fun removeChannel(guildId: Long, channelId: Long) = sql {
        execute(removeChannel) {
            this[1] = guildId
            this[2] = channelId
        }
    }
}

object IgnoredChannelsHandler : MultiChannelHandler(DBChannelType.IGNORED)