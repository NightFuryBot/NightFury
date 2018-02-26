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
package xyz.nightfury.ndb

import xyz.nightfury.ndb.internal.*

/**
 * @author Kaidan Gustave
 */
object WelcomesHandler : Database.Table() {
    private const val HAS_WELCOME = "SELECT * FROM WELCOMES WHERE GUILD_ID = ?"
    private const val GET_MESSAGE = "SELECT MESSAGE FROM WELCOMES WHERE GUILD_ID = ?"
    private const val GET_CHANNEL = "SELECT CHANNEL_ID FROM WELCOMES WHERE GUILD_ID = ?"
    private const val SET_WELCOME = "UPDATE WELCOMES SET CHANNEL_ID = ? AND MESSAGE = ? WHERE GUILD_ID = ?"
    private const val ADD_WELCOME = "INSERT INTO WELCOMES (GUILD_ID, CHANNEL_ID, MESSAGE) VALUES(?,?,?)"
    private const val REMOVE_WELCOME = "DELETE FROM WELCOMES WHERE GUILD_ID = ?"

    fun hasWelcome(guildId: Long): Boolean = sql(false) {
        any(HAS_WELCOME) {
            this[1] = guildId
        }
    }

    fun getWelcome(guildId: Long): String? = sql {
        statement(GET_MESSAGE) {
            this[1] = guildId
            query { it.getString("MESSAGE") }
        }
    }

    fun getChannel(guildId: Long): Long = sql(0L) {
        statement(GET_CHANNEL) {
            this[1] = guildId
            query(0L) { it["CHANNEL_ID"]!! }
        }
    }

    fun setWelcome(guildId: Long, channelId: Long, message: String) = sql {
        if(hasWelcome(guildId)) {
            execute(SET_WELCOME) {
                this[1] = channelId
                this[2] = message
                this[3] = guildId
            }
        } else {
            execute(ADD_WELCOME) {
                this[1] = guildId
                this[2] = channelId
                this[3] = message
            }
        }
    }

    fun removeWelcome(guildId: Long) = sql {
        execute(REMOVE_WELCOME) {
            this[1] = guildId
        }
    }
}