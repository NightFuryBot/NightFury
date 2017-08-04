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
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
class SQLCases(connection: Connection) : SQLCollection<Guild, Case>(connection) {

    init {
        getStatement = "SELECT * FROM $CASES WHERE $GUILD_ID = ?"
        addStatement = "INSERT INTO $CASES ($NUMBER, $GUILD_ID, $MESSAGE_ID, $MOD_ID, $TARGET_ID, $IS_ON_USER, $ACTION, $REASON) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        removeAllStatement = "DELETE FROM $CASES WHERE $GUILD_ID = ?"
    }

    override fun get(results: ResultSet, env: Guild): Set<Case>
    {
        val cases = HashSet<Case>()
        while (results.next())
        {
            val case = Case()
            case.number = results.getInt(NUMBER)
            case.guildId = results.getLong(GUILD_ID)
            case.messageId = results.getLong(MESSAGE_ID)
            case.modId = results.getLong(MOD_ID)
            case.targetId = results.getLong(TARGET_ID)
            case.isOnUser = results.getBoolean(IS_ON_USER)
            case.action = LogAction.getActionByAct(results.getString(ACTION))
            case.reason = results.getString(REASON)
            cases.add(case)
        }
        return cases
    }

    fun updateCase(case: Case)
    {
        try {
            connection.prepareStatement(
                    "UPDATE $CASES SET $REASON = ? WHERE $GUILD_ID = ? AND $NUMBER = ?")
            .use {
                it.setString(1, case.reason)
                it.setLong(2, case.guildId)
                it.setInt(3, case.number)
                it.execute()
            }
        } catch (e : SQLException) { SQL.LOG.warn(e) }
    }
}

private val CASES = "cases"
private val NUMBER = "number"
private val GUILD_ID = "guild_id"
private val MESSAGE_ID = "message_id"
private val MOD_ID = "mod_id"
private val TARGET_ID = "target_id"
private val IS_ON_USER = "is_on_user"
private val ACTION = "action"
private val REASON = "reason"