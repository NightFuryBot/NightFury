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
import net.dv8tion.jda.core.entities.TextChannel

/**
 * @author Kaidan Gustave
 */
abstract class SQLChannel(type: String): Table() {
    private val get    = "SELECT CHANNEL_ID FROM CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val add    = "INSERT INTO CHANNELS (GUILD_ID, CHANNEL_ID, TYPE) VALUES (?, ?, '$type')"
    private val set    = "UPDATE CHANNELS SET CHANNEL_ID = ? WHERE TYPE = '$type'"
    private val delete = "DELETE FROM CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"

    fun hasChannel(guild: Guild): Boolean = getChannel(guild) != null

    fun getChannel(guild: Guild): TextChannel? {
        return using(connection.prepareStatement(get)) {
            this[1] = guild.idLong
            using(executeQuery()) {
                if(next())
                    guild.getTextChannelById(getLong("CHANNEL_ID"))
                else null
            }
        }
    }

    fun setChannel(channel: TextChannel) {
        if(hasChannel(channel.guild)) {
            using(connection.prepareStatement(set)) {
                this[1] = channel.idLong
                execute()
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = channel.guild.idLong
                this[2] = channel.idLong
                execute()
            }
        }
    }

    fun deleteChannel(guild: Guild) {
        using(connection.prepareStatement(delete)) {
            this[1] = guild.idLong
            execute()
        }
    }
}

/**
 * @author Kaidan Gustave
 */
abstract class SQLChannels(type: String) : Table() {
    private val isChan = "SELECT * FROM CHANNELS WHERE GUILD_ID = ? AND CHANNEL_ID = ? AND TYPE = '$type'"
    private val get    = "SELECT CHANNEL_ID FROM CHANNELS WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val add    = "INSERT INTO CHANNELS (GUILD_ID, CHANNEL_ID, TYPE) VALUES (?, ?, '$type')"
    private val delete = "DELETE FROM CHANNELS WHERE GUILD_ID = ? AND CHANNEL_ID = ? AND TYPE = '$type'"

    fun isChannel(channel: TextChannel): Boolean {
        return using(connection.prepareStatement(isChan), default = false) {
            this[1] = channel.guild.idLong
            this[2] = channel.idLong
            using(executeQuery()) { next() }
        }
    }

    fun getChannels(guild: Guild): Set<TextChannel> {
        val set = HashSet<TextChannel>()
        using(connection.prepareStatement(get)) {
            this[1] = guild.idLong
            using(executeQuery()) {
                while(next())
                    set += (guild.getTextChannelById(getLong("CHANNEL_ID"))?:continue)
            }
        }
        return set
    }

    fun addChannel(channel: TextChannel) {
        using(connection.prepareStatement(add)) {
            this[1] = channel.guild.idLong
            this[2] = channel.idLong
            execute()
        }
    }

    fun deleteChannel(channel: TextChannel) {
        using(connection.prepareStatement(delete)) {
            this[1] = channel.guild.idLong
            this[2] = channel.idLong
            execute()
        }
    }
}

object SQLModeratorLog : SQLChannel("modlog")
object SQLAnnouncementChannel : SQLChannel("announcement")
object SQLIgnoredChannels : SQLChannels("ignored")