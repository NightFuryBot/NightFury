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
abstract class SQLRoles(connection: Connection, type: String) : SQLCollection<Guild, Role>(connection) {

    init {
        getStatement = "SELECT $ROLE_ID FROM $ROLES WHERE $GUILD_ID = ? AND $TYPE = '$type'"
        addStatement = "INSERT INTO $ROLES ($GUILD_ID, $ROLE_ID, $TYPE) VALUES (?, ?, '$type')"
        removeStatement = "DELETE FROM $ROLES WHERE $GUILD_ID = ? AND $ROLE_ID = ? AND $TYPE = '$type'"
    }

    override fun get(results: ResultSet, env: Guild) : Set<Role>
    {
        val roles = HashSet<Role>()
        while (results.next())
        {
            val role = env.getRoleById(results.getLong(ROLE_ID))
            if(role != null)
                roles.add(role)
        }
        return roles
    }
}

abstract class SQLRole(connection: Connection, type: String) : SQLSingleton<Guild, Role>(connection) {

    init {
        getStatement = "SELECT $ROLE_ID FROM $ROLES WHERE $GUILD_ID = ? AND $TYPE = '$type'"
        setStatement = "INSERT INTO $ROLES ($GUILD_ID, $ROLE_ID, $TYPE) VALUES (?, ?, '$type')"
        updateStatement = "UPDATE $ROLES SET $ROLE_ID = ? WHERE $GUILD_ID = ? AND $TYPE = '$type'"
        resetStatement = "DELETE FROM $ROLES WHERE $GUILD_ID = ? AND $TYPE = '$type'"
    }

    override fun get(results: ResultSet, env: Guild) : Role?
    {
        if(results.next())
            return env.getRoleById(results.getLong(ROLE_ID))
        return null
    }
}

class SQLRoleMe(connection: Connection) : SQLRoles(connection,"roleme")
class SQLColorMe(connection: Connection) : SQLRoles(connection,"colorme")
class SQLModeratorRole(connection: Connection) : SQLRole(connection, "moderator")
class SQLMutedRole(connection: Connection) : SQLRole(connection, "muted")

private val ROLES = "roles"         // Table Name
private val GUILD_ID = "guild_id"   // Long
private val ROLE_ID = "role_id"     // Long
private val TYPE = "type"           // varchar(20)