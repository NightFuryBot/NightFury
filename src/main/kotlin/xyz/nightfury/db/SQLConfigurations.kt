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

import xyz.nightfury.Command
import xyz.nightfury.CommandLevel
import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
object SQLLimits : Table() {
    private val get = "SELECT LIMIT_NUMBER FROM COMMAND_LIMITS WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)"
    private val add = "INSERT INTO COMMAND_LIMITS (GUILD_ID, COMMAND_NAME, LIMIT_NUMBER) VALUES (?, ?, ?)"
    private val set = "UPDATE COMMAND_LIMITS SET LIMIT_NUMBER = ? WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)"
    private val remove = "DELETE FROM COMMAND_LIMITS WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)"

    fun hasLimit(guild: Guild, command: String) = getLimit(guild, command) != 0

    fun getLimit(guild: Guild, command: String): Int {
        return using(connection.prepareStatement(get), default = 0) {
            this[1] = guild.idLong
            this[2] = command
            using(executeQuery(), default = 0) { if(next()) getInt("LIMIT_NUMBER") else 0 }
        }
    }

    fun setLimit(guild: Guild, command: String, limit: Int) {
        if(hasLimit(guild, command)) {
            using(connection.prepareStatement(set)) {
                this[1] = limit
                this[2] = guild.idLong
                this[3] = command
                execute()
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = guild.idLong
                this[2] = command
                this[3] = limit
                execute()
            }
        }
    }

    fun removeLimit(guild: Guild, command: String) {
        using(connection.prepareStatement(remove)) {
            this[1] = guild.idLong
            this[2] = command
            execute()
        }
    }
}

/**
 * @author Kaidan Gustave
 */
object SQLEnables : Table() {
    private val has = "SELECT * FROM ENABLES WHERE GUILD_ID = ? AND ENABLE_TYPE = ?"
    private val get = "SELECT STATUS FROM ENABLES WHERE GUILD_ID = ? AND ENABLE_TYPE = ?"
    private val set = "UPDATE ENABLES SET STATUS = ? WHERE GUILD_ID = ? AND ENABLE_TYPE = ?"
    private val add = "INSERT INTO ENABLES (GUILD_ID, ENABLE_TYPE, STATUS) VALUES (?,?,?)"

    fun hasStatusFor(guild: Guild, type: Type): Boolean {
        return using(connection.prepareStatement(has), default = false) {
            this[1] = guild.idLong
            this[2] = type
            using(executeQuery()) { next() }
        }
    }

    fun getStatusFor(guild: Guild, type: Type): Boolean {
        return using(connection.prepareStatement(get), default = false) {
            this[1] = guild.idLong
            this[2] = type
            using(executeQuery()) { if(next()) getBoolean("STATUS") else false }
        }
    }

    fun setStatusFor(guild: Guild, type: Type, status: Boolean) {
        if(hasStatusFor(guild, type)) {
            using(connection.prepareStatement(set)) {
                this[1] = status
                this[2] = guild.idLong
                this[3] = type
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = guild.idLong
                this[2] = type
                this[3] = status
            }
        }
    }

    enum class Type {
        ROLE_PERSIST, ANTI_ADS
    }
}

/**
 * @author Kaidan Gustave
 */
object SQLLevel : Table() {
    private val get = "SELECT LEVEL FROM COMMAND_LEVELS WHERE GUILD_ID = ? AND LOWER(COMMAND) = LOWER(?)"
    private val set = "INSERT INTO COMMAND_LEVELS(GUILD_ID, COMMAND, LEVEL) VALUES (?,LOWER(?),?)"
    private val update = "UPDATE COMMAND_LEVELS SET LEVEL = ? WHERE GUILD_ID = ? AND LOWER(COMMAND) = LOWER(?)"

    fun hasLevel(guild: Guild, command: Command): Boolean {
        return using(connection.prepareStatement(get), false) {
            this[1] = guild.idLong
            this[2] = command.name
            using(executeQuery()) { next() }
        }
    }

    fun getLevel(guild: Guild, command: Command): CommandLevel {
        return using(connection.prepareStatement(get), command.defaultLevel) {
            this[1] = guild.idLong
            this[2] = command.name
            using(executeQuery()) {
                if(next()) {
                    val levelString = getString("LEVEL")
                    if(levelString != null) {
                        try {
                            CommandLevel.valueOf(levelString.toUpperCase())
                        } catch (e: Throwable) { null }
                    } else null
                } else null
            }
        }
    }

    fun setLevel(guild: Guild, command: Command, level: CommandLevel) {
        if(hasLevel(guild, command)) {
            using(connection.prepareStatement(update)) {
                this[1] = level
                this[2] = guild.idLong
                this[3] = command.name
            }
        } else {
            using(connection.prepareStatement(set)) {
                this[1] = guild.idLong
                this[2] = command.name
                this[3] = level
                execute()
            }
        }
    }
}