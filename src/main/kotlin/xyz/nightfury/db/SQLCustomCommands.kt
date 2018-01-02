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
@file:Suppress("Unused")
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
object SQLCustomCommands : Table() {
    private val getAll = "SELECT NAME FROM CUSTOM_COMMANDS WHERE GUILD_ID = ?"
    private val getContent = "SELECT CONTENT FROM CUSTOM_COMMANDS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"
    private val add = "INSERT INTO CUSTOM_COMMANDS (NAME, CONTENT, GUILD_ID) VALUES (?, ?, ?)"
    private val remove = "DELETE FROM CUSTOM_COMMANDS WHERE LOWER(NAME) = LOWER(?) AND GUILD_ID = ?"

    fun getAll(guild: Guild) : Set<String> {
        val all = HashSet<String>()
        using(connection.prepareStatement(getAll)) {
            this[1] = guild.idLong
            using(executeQuery()) {
                while(next())
                    all += getString("NAME")
            }
        }
        return all
    }

    fun getContentFor(name : String, guild: Guild): String {
        return using(connection.prepareStatement(getContent), default = "") {
            this[1] = name
            this[2] = guild.idLong
            using(executeQuery()) {
                if(next()) getString("CONTENT") else ""
            }
        }
    }

    fun add(name: String, content: String, guild: Guild) {
        using(connection.prepareStatement(add)) {
            this[1] = name
            this[2] = content
            this[3] = guild.idLong
            execute()
        }
    }

    fun remove(name: String, guild: Guild) {
        using(connection.prepareStatement(remove)) {
            this[1] = name
            this[2] = guild.idLong
            execute()
        }
    }
}