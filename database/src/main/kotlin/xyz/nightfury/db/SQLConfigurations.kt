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
interface ISQLLimits {
    fun hasLimit(guildId: Long, command: String): Boolean

    fun getLimit(guildId: Long, command: String): Int

    fun setLimit(guildId: Long, command: String, limit: Int)

    fun removeLimit(guildId: Long, command: String)
}

/**
 * @author Kaidan Gustave
 */
object SQLLimits : Table(), ISQLLimits {
    private const val get = "SELECT LIMIT_NUMBER FROM COMMAND_LIMITS WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)"
    private const val add = "INSERT INTO COMMAND_LIMITS (GUILD_ID, COMMAND_NAME, LIMIT_NUMBER) VALUES (?, ?, ?)"
    private const val set = "UPDATE COMMAND_LIMITS SET LIMIT_NUMBER = ? WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)"
    private const val remove = "DELETE FROM COMMAND_LIMITS WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)"

    override fun hasLimit(guildId: Long, command: String) = getLimit(guildId, command) != 0

    fun hasLimit(guild: Guild, command: String) = getLimit(guild.idLong, command)

    override fun getLimit(guildId: Long, command: String): Int {
        return using(connection.prepareStatement(get), default = 0) {
            this[1] = guildId
            this[2] = command
            using(executeQuery(), default = 0) { if(next()) getInt("LIMIT_NUMBER") else 0 }
        }
    }

    fun getLimit(guild: Guild, command: String): Int {
        return getLimit(guild.idLong, command)
    }

    override fun setLimit(guildId: Long, command: String, limit: Int) {
        if(hasLimit(guildId, command)) {
            using(connection.prepareStatement(set)) {
                this[1] = limit
                this[2] = guildId
                this[3] = command
                execute()
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = guildId
                this[2] = command
                this[3] = limit
                execute()
            }
        }
    }

    fun setLimit(guild: Guild, command: String, limit: Int) {
        setLimit(guild.idLong, command, limit)
    }

    override fun removeLimit(guildId: Long, command: String) {
        using(connection.prepareStatement(remove)) {
            this[1] = guildId
            this[2] = command
            execute()
        }
    }

    fun removeLimit(guild: Guild, command: String) {
        removeLimit(guild.idLong, command)
    }
}

/**
 * @author Kaidan Gustave
 */
interface ISQLEnables {
    fun hasStatusFor(guildId: Long, type: Type): Boolean
    fun getStatusFor(guildId: Long, type: Type): Boolean
    fun setStatusFor(guildId: Long, type: Type, status: Boolean)

    enum class Type {
        ROLE_PERSIST, ANTI_ADS
    }
}

/**
 * @author Kaidan Gustave
 */
object SQLEnables : Table(), ISQLEnables {
    private const val has = "SELECT * FROM ENABLES WHERE GUILD_ID = ? AND ENABLE_TYPE = ?"
    private const val get = "SELECT STATUS FROM ENABLES WHERE GUILD_ID = ? AND ENABLE_TYPE = ?"
    private const val set = "UPDATE ENABLES SET STATUS = ? WHERE GUILD_ID = ? AND ENABLE_TYPE = ?"
    private const val add = "INSERT INTO ENABLES (GUILD_ID, ENABLE_TYPE, STATUS) VALUES (?,?,?)"

    override fun hasStatusFor(guildId: Long, type: ISQLEnables.Type): Boolean {
        return using(connection.prepareStatement(has), default = false) {
            this[1] = guildId
            this[2] = type
            using(executeQuery()) { next() }
        }
    }

    override fun getStatusFor(guildId: Long, type: ISQLEnables.Type): Boolean {
        return using(connection.prepareStatement(get), default = false) {
            this[1] = guildId
            this[2] = type
            using(executeQuery()) { if(next()) getBoolean("STATUS") else false }
        }
    }

    override fun setStatusFor(guildId: Long, type: ISQLEnables.Type, status: Boolean) {
        if(hasStatusFor(guildId, type)) {
            using(connection.prepareStatement(set)) {
                this[1] = status
                this[2] = guildId
                this[3] = type
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = guildId
                this[2] = type
                this[3] = status
            }
        }
    }
}

interface ISQLLevel<L> {
    fun hasLevel(guildId: Long, commandName: String): Boolean
    fun getLevel(guildId: Long, commandName: String, default: L): L
    fun setLevel(guildId: Long, commandName: String, level: L)
}