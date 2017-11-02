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

import xyz.nightfury.music.MusicSettings
import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
object SQLMusicSettings : Table() {
    /*
     * GUILD_ID | VOICE_CHANNEL_ID | TEXT_CHANNEL_ID | NP_IN_TOPIC
     */

    private val select = "SELECT * FROM MUSIC_SETTINGS WHERE GUILD_ID = ?"
    private val insert = "INSERT INTO MUSIC_SETTINGS(GUILD_ID, VOICE_CHANNEL_ID, TEXT_CHANNEL_ID, NP_IN_TOPIC) VALUES (?,?,?,?)"
    private val update = "UPDATE MUSIC_SETTINGS SET VOICE_CHANNEL_ID = ?, TEXT_CHANNEL_ID = ?, NP_IN_TOPIC = ? WHERE GUILD_ID = ?"

    fun hasMusicSettings(guild: Guild): Boolean {
        return using(connection.prepareStatement(select), false)
        {
            this[1] = guild.idLong
            using(executeQuery())
            {
                next()
            }
        }
    }

    fun getMusicSettings(guild: Guild): MusicSettings? {
        return using(connection.prepareStatement(select))
        {
            this[1] = guild.idLong
            using(executeQuery())
            {
                if(next())
                    MusicSettings(guild, guild.getVoiceChannelById(getLong("VOICE_CHANNEL_ID")),
                                  guild.getTextChannelById(getLong("TEXT_CHANNEL_ID")), getBoolean("NP_IN_TOPIC"))
                else null
            }
        }
    }

    fun setMusicSettings(settings: MusicSettings) {
        if(hasMusicSettings(settings.guild))
        {
            using(connection.prepareStatement(insert))
            {
                this[1] = settings.guild.idLong
                this[2] = settings.voiceChannel?.idLong ?: 0
                this[3] = settings.textChannel?.idLong ?: 0
                this[4] = settings.npInTopic
                executeQuery()
            }
        }
        else
        {
            using(connection.prepareStatement(update))
            {
                this[1] = settings.voiceChannel?.idLong ?: 0
                this[2] = settings.textChannel?.idLong ?: 0
                this[3] = settings.npInTopic
                this[4] = settings.guild.idLong
                executeQuery()
            }
        }
    }
}