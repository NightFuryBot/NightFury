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
package xyz.nightfury.ndb

import xyz.nightfury.ndb.entities.CommandSettings
import xyz.nightfury.ndb.internal.*

/**
 * @author Kaidan Gustave
 */
object CommandSettingsManager : Database.Table() {
    private const val GET_SETTINGS        = "SELECT * FROM COMMAND_SETTINGS WHERE GUILD_ID = ? AND COMMAND = UPPER(?)"
    private const val SET_SETTINGS        = "UPDATE COMMAND_SETTINGS SET LEVEL = ? AND LIMIT_NUMBER = ? WHERE GUILD_ID = ? AND COMMAND = UPPER(?)"
    private const val ADD_SETTINGS        = "INSERT INTO COMMAND_SETTINGS(GUILD_ID, COMMAND, LEVEL, LIMIT_NUMBER) VALUES (?, ?, ?, UPPER(?))"
    private const val REMOVE_SETTINGS     = "DELETE FROM COMMAND_SETTINGS WHERE GUILD_ID = ? AND COMMAND = UPPER(?)"
    private const val REMOVE_ALL_SETTINGS = "DELETE FROM COMMAND_SETTINGS WHERE GUILD_ID = ?"

    fun hasSettings(guildId: Long, command: String): Boolean = sql(false) {
        any(GET_SETTINGS) {
            this[1] = guildId
            this[2] = command
        }
    }

    fun getSettings(guildId: Long, command: String): CommandSettings? = sql {
        statement(GET_SETTINGS) {
            this[1] = guildId
            this[2] = command
            query {
                CommandSettings(
                    it.getLong("GUILD_ID"), it.getString("COMMAND_NAME"),
                    it.getString("COMMAND"), it.getInt("LIMIT_NUMBER")
                )
            }
        }
    }

    fun setSettings(settings: CommandSettings) = sql {
        if(hasSettings(settings.guildId, settings.command)) {
            execute(SET_SETTINGS) {
                this[1] = settings.level
                this[2] = settings.limitNumber
            }
        } else {
            execute(ADD_SETTINGS) {
                this[1] = settings.guildId
                this[2] = settings.command
                this[3] = settings.level
                this[4] = settings.limitNumber
            }
        }
    }

    fun removeSettings(guildId: Long, command: String) = sql {
        execute(REMOVE_SETTINGS) {
            this[1] = guildId
            this[2] = command
        }
    }

    fun removeAllSettings(guildId: Long) = sql {
        execute(REMOVE_ALL_SETTINGS) {
            this[1] = guildId
        }
    }
}