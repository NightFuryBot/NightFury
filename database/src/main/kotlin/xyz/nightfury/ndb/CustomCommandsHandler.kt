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
object CustomCommandsHandler : Database.Table() {
    private const val GET_ALL_COMMANDS = "SELECT NAME, CONTENT FROM CUSTOM_COMMANDS WHERE GUILD_ID = ?"
    private const val GET_CONTENT = "SELECT CONTENT FROM CUSTOM_COMMANDS WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)"
    private const val ADD_CONTENT = "INSERT INTO CUSTOM_COMMANDS(NAME, CONTENT, GUILD_ID) VALUES (?,?,?)"
    private const val SET_CONTENT = "UPDATE CUSTOM_COMMANDS SET CONTENT = ? WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)"
    private const val REMOVE_COMMAND = "DELETE FROM CUSTOM_COMMANDS WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)"

    fun getAllCommands(guildId: Long): List<Pair<String, String>> = sql({ emptyList() }) {
        val commands = ArrayList<Pair<String, String>>()
        statement(GET_ALL_COMMANDS) {
            this[1] = guildId
            queryAll { commands += it.getString("NAME") to it.getString("CONTENT") }
        }
        return commands
    }

    fun isCommand(guildId: Long, name: String) = sql(false) {
        any(GET_CONTENT) {
            this[1] = guildId
            this[2] = name
        }
    }

    fun getCommandContent(guildId: Long, name: String): String? = sql {
        statement(GET_CONTENT) {
            this[1] = guildId
            this[2] = name
            query { it.getString("CONTENT") }
        }
    }

    fun setCommandContent(guildId: Long, name: String, content: String) = sql {
        if(!isCommand(guildId, name)) {
            execute(ADD_CONTENT) {
                this[1] = name
                this[2] = content
                this[3] = guildId
            }
        } else {
            execute(SET_CONTENT) {
                this[1] = content
                this[2] = guildId
                this[3] = name
            }
        }
    }

    fun removeCommand(guildId: Long, name: String) = sql {
        execute(REMOVE_COMMAND) {
            this[1] = guildId
            this[2] = name
        }
    }
}