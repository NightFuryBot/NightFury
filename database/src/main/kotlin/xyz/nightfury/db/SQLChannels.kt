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
@file:Suppress("Unused", "MemberVisibilityCanBePrivate")
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel

/**
 * @author Kaidan Gustave
 */
interface ISQLChannel {
    fun hasChannel(guildId: Long): Boolean
    fun getChannel(guildId: Long): Long?
    fun setChannel(guildId: Long, channelId: Long)
    fun deleteChannel(guildId: Long)
}

/**
 * @author Kaidan Gustave
 */
abstract class SQLChannel(type: String): Table(), ISQLChannel {
    private val get    = "SELECT CHANNEL_ID FROM CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val add    = "INSERT INTO CHANNELS (GUILD_ID, CHANNEL_ID, TYPE) VALUES (?, ?, '$type')"
    private val set    = "UPDATE CHANNELS SET CHANNEL_ID = ? WHERE TYPE = '$type'"
    private val delete = "DELETE FROM CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"

    override fun hasChannel(guildId: Long): Boolean = getChannel(guildId) != null

    fun hasChannel(guild: Guild): Boolean = getChannel(guild) != null

    override fun getChannel(guildId: Long): Long? {
        return using(connection.prepareStatement(get)) {
            this[1] = guildId
            using(executeQuery()) {
                if(next()) getLong("CHANNEL_ID") else null
            }
        }
    }

    fun getChannel(guild: Guild): TextChannel? {
        return getChannel(guild.idLong)?.let { guild.getTextChannelById(it) }
    }

    override fun setChannel(guildId: Long, channelId: Long) {
        if(hasChannel(guildId)) {
            using(connection.prepareStatement(set)) {
                this[1] = channelId
                execute()
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = guildId
                this[2] = channelId
                execute()
            }
        }
    }

    fun setChannel(channel: TextChannel) {
        setChannel(channel.guild.idLong, channel.idLong)
    }

    override fun deleteChannel(guildId: Long) {
        using(connection.prepareStatement(delete)) {
            this[1] = guildId
            execute()
        }
    }

    fun deleteChannel(guild: Guild) {
        deleteChannel(guild.idLong)
    }
}

/**
 * @author Kaidan Gustave
 */
interface ISQLChannels {
    fun isChannel(guildId: Long, channelId: Long): Boolean
    fun getChannels(guildId: Long): Set<Long>
    fun addChannel(guildId: Long, channelId: Long)
    fun deleteChannel(channelId: Long)
}

/**
 * @author Kaidan Gustave
 */
abstract class SQLChannels(type: String): Table(), ISQLChannels {
    private val isChan = "SELECT * FROM CHANNELS WHERE GUILD_ID = ? AND CHANNEL_ID = ? AND TYPE = '$type'"
    private val get    = "SELECT CHANNEL_ID FROM CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val add    = "INSERT INTO CHANNELS (GUILD_ID, CHANNEL_ID, TYPE) VALUES (?, ?, '$type')"
    private val delete = "DELETE FROM CHANNELS WHERE CHANNEL_ID = ? AND TYPE = '$type'"

    override fun isChannel(guildId: Long, channelId: Long): Boolean {
        return using(connection.prepareStatement(isChan), default = false) {
            this[1] = guildId
            this[2] = channelId
            using(executeQuery()) { next() }
        }
    }

    fun isChannel(channel: TextChannel): Boolean {
        return isChannel(channel.guild.idLong, channel.idLong)
    }

    override fun getChannels(guildId: Long): Set<Long> {
        val set = HashSet<Long>()
        using(connection.prepareStatement(get)) {
            this[1] = guildId
            using(executeQuery()) {
                while(next()) {
                    // ResultSet#getLong(String) returns 0L if the column is null
                    set += getLong("CHANNEL_ID").takeIf { it != 0L } ?: continue
                }
            }
        }
        return set
    }

    fun getChannels(guild: Guild): Set<TextChannel> {
        return getChannels(guild.idLong).mapNotNullTo(HashSet()) { guild.getTextChannelById(it) }
    }

    override fun addChannel(guildId: Long, channelId: Long) {
        using(connection.prepareStatement(add)) {
            this[1] = guildId
            this[2] = channelId
            execute()
        }
    }

    fun addChannel(channel: TextChannel) {
        addChannel(channel.guild.idLong, channel.idLong)
    }

    override fun deleteChannel(channelId: Long) {
        using(connection.prepareStatement(delete)) {
            this[1] = channelId
            execute()
        }
    }

    fun deleteChannel(channel: TextChannel) {
        deleteChannel(channel.idLong)
    }
}

object SQLModeratorLog : SQLChannel("modlog")
object SQLAnnouncementChannel : SQLChannel("announcement")
object SQLIgnoredChannels : SQLChannels("ignored")
