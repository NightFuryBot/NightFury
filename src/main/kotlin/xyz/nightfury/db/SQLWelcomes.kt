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
object SQLWelcomes : Table() {
    private val getMessage = "SELECT MESSAGE FROM WELCOMES WHERE GUILD_ID = ?"
    private val getChannel = "SELECT CHANNEL_ID FROM WELCOMES WHERE GUILD_ID = ?"
    private val setWelcome = "INSERT INTO WELCOMES (GUILD_ID, CHANNEL_ID, MESSAGE) VALUES(?,?,?)"
    private val removeWelcome = "DELETE FROM WELCOMES WHERE GUILD_ID = ?"

    fun hasWelcome(guild: Guild): Boolean = getChannel(guild) == null

    fun getMessage(guild: Guild): String? {
        return using(connection.prepareStatement(getMessage))
        {
            this[1] = guild.idLong
            using(executeQuery()) { if(next()) getString("MESSAGE") else null }
        }
    }

    fun getChannel(guild: Guild): TextChannel? {
        return using(connection.prepareStatement(getChannel))
        {
            this[1] = guild.idLong
            using(executeQuery())
            {
                if(next())
                    guild.getTextChannelById(getLong("CHANNEL_ID"))
                else null
            }
        }
    }

    fun setWelcome(channel: TextChannel, message: String) {
        using(connection.prepareStatement(setWelcome))
        {
            this[1] = channel.guild.idLong
            this[2] = channel.idLong
            this[3] = message
            execute()
        }
    }

    fun removeWelcome(guild: Guild) {
        using(connection.prepareStatement(removeWelcome))
        {
            this[1] = guild.idLong
            execute()
        }
    }
}