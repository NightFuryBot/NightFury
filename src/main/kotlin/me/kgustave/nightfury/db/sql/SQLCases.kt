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
package me.kgustave.nightfury.db.sql

import me.kgustave.nightfury.entities.Case
import me.kgustave.nightfury.entities.LogAction
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import java.sql.Connection

/**
 * @author Kaidan Gustave
 */
class SQLCases(private val connection: Connection)
{
    private val get = "SELECT * FROM CASES WHERE GUILD_ID = ?"
    private val getByUser = "SELECT * FROM CASES WHERE GUILD_ID = ? AND MOD_ID = ? ORDER BY NUMBER DESC"
    private val getNumber = "SELECT * FROM CASES WHERE GUILD_ID = ? AND NUMBER = ?"
    private val add = "INSERT INTO CASES (NUMBER, GUILD_ID, MESSAGE_ID, MOD_ID, TARGET_ID, IS_ON_USER, ACTION, REASON) "+
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    private val update = "UPDATE CASES SET REASON = ? WHERE GUILD_ID = ? AND NUMBER = ?"

    fun getCases(guild: Guild): List<Case> {
        val list = ArrayList<Case>()
        using(connection.prepareStatement(get))
        {
            this[1] = guild.idLong

            using(executeQuery())
            {
                while(next())
                    list += Case().apply {
                        number = getInt("NUMBER")
                        guildId = getLong("GUILD_ID")
                        messageId = getLong("MESSAGE_ID")
                        modId = getLong("MOD_ID")
                        targetId = getLong("TARGET_ID")
                        isOnUser = getBoolean("IS_ON_USER")
                        action = LogAction.getActionByAct(getString("ACTION"))
                        reason = getString("REASON")
                    }
            }
        }
        return list
    }

    fun getCaseNumber(guild: Guild, number: Int): Case? {
        return using(connection.prepareStatement(getNumber))
        {
            this[1] = guild.idLong
            this[2] = number

            using(executeQuery())
            {
                if(next()) {
                    Case().apply {
                        this.number = getInt("NUMBER")
                        this.guildId = getLong("GUILD_ID")
                        this.messageId = getLong("MESSAGE_ID")
                        this.modId = getLong("MOD_ID")
                        this.targetId = getLong("TARGET_ID")
                        this.isOnUser = getBoolean("IS_ON_USER")
                        this.action = LogAction.getActionByAct(getString("ACTION"))
                        this.reason = getString("REASON")
                    }
                } else null
            }
        }
    }

    fun getCasesByUser(guild: Guild, user: User): List<Case> {
        val list = ArrayList<Case>()
        using(connection.prepareStatement(getByUser))
        {
            this[1] = guild.idLong
            this[2] = user.idLong
            using(executeQuery())
            {
                list += Case().apply {
                    this.number = getInt("NUMBER")
                    this.guildId = getLong("GUILD_ID")
                    this.messageId = getLong("MESSAGE_ID")
                    this.modId = getLong("MOD_ID")
                    this.targetId = getLong("TARGET_ID")
                    this.isOnUser = getBoolean("IS_ON_USER")
                    this.action = LogAction.getActionByAct(getString("ACTION"))
                    this.reason = getString("REASON")
                }
            }
        }
        return list
    }

    fun addCase(case: Case) {
        using(connection.prepareStatement(add))
        {
            this[1] = case.number
            this[2] = case.guildId
            this[3] = case.messageId
            this[4] = case.modId
            this[5] = case.targetId
            this[6] = case.isOnUser
            this[7] = case.action
            this[8] = case.reason

            execute()
        }
    }

    fun updateCase(case: Case) {
        using(connection.prepareStatement(update))
        {
            this[1] = case.reason
            this[2] = case.guildId
            this[3] = case.number
            execute()
        }
    }
}