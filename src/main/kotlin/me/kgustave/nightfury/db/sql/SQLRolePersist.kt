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

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import java.sql.Connection
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
class SQLRolePersist(connection: Connection) : SQLCollection<Guild, Role>(connection)
{
    override val getStatement = "SELECT role_ids FROM role_persist WHERE guild_id = ? AND user_id = ?"
    override val addStatement = "INSERT INTO role_persist (guild_id, user_id, role_ids) VALUES (?, ?, ?)"
    override val removeStatement = "DELETE FROM role_persist WHERE guild_id = ? AND user_id = ?"
    override val removeAllStatement = "DELETE FROM role_persist WHERE guild_id = ?"

    override fun get(results: ResultSet, env: Guild): Set<Role>
    {
        val set = HashSet<Role>()
        if(results.next())
            results.getString("role_ids")
                    .split(Regex("\\|"))
                    .mapNotNullTo(set) { env.getRoleById(it) }
        return set
    }
}