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
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.Guild
import xyz.nightfury.Command
import xyz.nightfury.CommandLevel

/**
 * @author Kaidan Gustave
 */
object SQLLevel : Table(), ISQLLevel<CommandLevel> {
    private const val get = "SELECT LEVEL FROM COMMAND_LEVELS WHERE GUILD_ID = ? AND LOWER(COMMAND) = LOWER(?)"
    private const val set = "INSERT INTO COMMAND_LEVELS(GUILD_ID, COMMAND, LEVEL) VALUES (?,LOWER(?),?)"
    private const val update = "UPDATE COMMAND_LEVELS SET LEVEL = ? WHERE GUILD_ID = ? AND LOWER(COMMAND) = LOWER(?)"

    override fun hasLevel(guild: Guild, commandName: String): Boolean {
        return using(connection.prepareStatement(get), false) {
            this[1] = guild.idLong
            this[2] = commandName
            using(executeQuery()) { next() }
        }
    }

    override fun getLevel(guild: Guild, commandName: String, default: CommandLevel): CommandLevel {
        return using(connection.prepareStatement(get), default) {
            this[1] = guild.idLong
            this[2] = commandName.toLowerCase()
            using(executeQuery()) {
                if(next()) {
                    val levelString = getString("LEVEL")
                    if(levelString != null) {
                        try {
                            CommandLevel.valueOf(levelString.toUpperCase())
                        } catch(e: Throwable) {
                            null
                        }
                    } else null
                } else null
            }
        }
    }

    fun getLevel(guild: Guild, command: Command): CommandLevel {
        return getLevel(guild, command.fullname, command.defaultLevel)
    }

    override fun setLevel(guild: Guild, commandName: String, level: CommandLevel) {
        if(hasLevel(guild, commandName)) {
            using(connection.prepareStatement(update)) {
                this[1] = level
                this[2] = guild.idLong
                this[3] = commandName.toLowerCase()
            }
        } else {
            using(connection.prepareStatement(set)) {
                this[1] = guild.idLong
                this[2] = commandName.toLowerCase()
                this[3] = level
                execute()
            }
        }
    }

    fun setLevel(guild: Guild, command: Command, level: CommandLevel) {
        setLevel(guild, command.fullname, level)
    }
}
