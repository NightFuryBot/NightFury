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
class SQLCases(connection: Connection) : SQLCollection<Guild, Case>(connection)
{
    override val getStatement = "SELECT * FROM cases WHERE guild_id = ?"
    override val addStatement =
            "INSERT INTO cases (number, guild_id, message_id, mod_id, target_id, is_on_user, action, reason) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    override val removeStatement = "" // Unused
    override val removeAllStatement = "DELETE FROM cases WHERE guild_id = ?"

    override fun get(results: ResultSet, env: Guild): Set<Case>
    {
        val cases = HashSet<Case>()
        while (results.next())
        {
            val case = Case()
            case.number = results.getInt("number")
            case.guildId = results.getLong("guild_id")
            case.messageId = results.getLong("message_id")
            case.modId = results.getLong("mod_id")
            case.targetId = results.getLong("target_id")
            case.isOnUser = results.getBoolean("is_on_user")
            case.action = LogAction.getActionByAct(results.getString("action"))
            case.reason = results.getString("reason")
            cases.add(case)
        }
        return cases
    }

    fun updateCase(case: Case) = try {
        connection prepare "UPDATE cases SET reason = ? WHERE guild_id = ? AND number = ?" closeAfter {
            insert(case.reason, case.guildId, case.number).execute()
        }
    } catch (e : SQLException) { SQL.LOG.warn(e) }
}