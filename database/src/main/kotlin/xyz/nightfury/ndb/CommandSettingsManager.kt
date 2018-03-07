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

/**
 * @author Kaidan Gustave
 */
object CommandSettingsManager : Database.Table() {
    private const val GET_SETTINGS        = "SELECT * FROM COMMAND_SETTINGS WHERE GUILD_ID = ? AND COMMAND = UPPER(?)"
    private const val ADD_SETTINGS        = "INSERT INTO COMMAND_SETTINGS(GUILD_ID, COMMAND, LEVEL, LIMIT_NUMBER) VALUES (?, UPPER(?), UPPER(?), ?)"
    private const val REMOVE_ALL_SETTINGS = "DELETE FROM COMMAND_SETTINGS WHERE GUILD_ID = ?"
    private const val CHECK_TO_REMOVE     = "DELETE FROM COMMAND_SETTINGS WHERE GUILD_ID = ? AND COMMAND = UPPER(?) " +
                                            "AND LEVEL IS NULL AND LIMIT_NUMBER IS NULL"

    private const val SET_COLUMN = "UPDATE COMMAND_SETTINGS SET %s = ? WHERE GUILD_ID = ? AND COMMAND = ?"

    fun hasSettings(guildId: Long, command: String): Boolean = sql(false) {
        any(GET_SETTINGS) {
            this[1] = guildId
            this[2] = command
        }
    }

    fun getLevel(guildId: Long, command: String): String? = sql {
        statement(GET_SETTINGS) {
            this[1] = guildId
            this[2] = command
            query { it.getString("LEVEL") }
        }
    }

    fun getLimit(guildId: Long, command: String): Int? = sql {
        statement(GET_SETTINGS) {
            this[1] = guildId
            this[2] = command
            query { res -> res.getInt("LIMIT_NUMBER").takeIf { !res.wasNull() } }
        }
    }

    fun setLevel(guildId: Long, command: String, level: String?) {
        when {
            hasSettings(guildId, command) -> sql {
                execute(SET_COLUMN.format("LEVEL")) {
                    this[1] = level
                    this[2] = guildId
                    this[3] = command
                }
            }
            level !== null -> addSettings(guildId, command, level, null)
            else -> checkToRemove(guildId, command)
        }
    }

    fun setLimit(guildId: Long, command: String, limit: Int?) {
        when {
            hasSettings(guildId, command) -> sql {
                execute(SET_COLUMN.format("LIMIT_NUMBER")) {
                    this[1] = limit
                    this[2] = guildId
                    this[3] = command
                }
            }
            limit !== null -> addSettings(guildId, command, null, limit)
            else -> checkToRemove(guildId, command)
        }
    }

    fun removeAllSettings(guildId: Long) = sql {
        execute(REMOVE_ALL_SETTINGS) {
            this[1] = guildId
        }
    }

    private fun addSettings(guildId: Long, command: String, level: String?, limit: Int?) = sql {
        execute(ADD_SETTINGS) {
            this[1] = guildId
            this[2] = command
            this[3] = level
            this[4] = limit
        }
    }

    private fun checkToRemove(guildId: Long, command: String) = sql {
        execute(CHECK_TO_REMOVE) {
            this[1] = guildId
            this[2] = command
        }
    }
}