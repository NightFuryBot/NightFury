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
import net.dv8tion.jda.core.entities.Role

/**
 * @author Kaidan Gustave
 */
abstract class SQLRoles(type: String): Table() {
    private val isRole = "SELECT * FROM ROLES WHERE GUILD_ID = ? AND ROLE_ID = ? AND TYPE = '$type'"
    private val get    = "SELECT ROLE_ID FROM ROLES WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val add    = "INSERT INTO ROLES (GUILD_ID, ROLE_ID, TYPE) VALUES (?, ?, '$type')"
    private val delete = "DELETE FROM ROLES WHERE GUILD_ID = ? AND ROLE_ID = ? AND TYPE = '$type'"

    fun isRole(role: Role): Boolean {
        return using(connection.prepareStatement(isRole), default = false) {
            this[1] = role.guild.idLong
            this[2] = role.idLong
            using(executeQuery()) { next() }
        }
    }

    fun getRoles(guild: Guild): Set<Role> {
        val set = HashSet<Role>()
        using(connection.prepareStatement(get)) {
            this[1] = guild.idLong
            using(executeQuery()) {
                while(next()) set += (guild.getRoleById(getLong("ROLE_ID")) ?: continue)
            }
        }
        return set
    }

    fun addRole(role: Role) {
        using(connection.prepareStatement(add)) {
            this[1] = role.guild.idLong
            this[2] = role.idLong
            execute()
        }
    }

    fun deleteRole(role: Role) {
        using(connection.prepareStatement(delete)) {
            this[1] = role.guild.idLong
            this[2] = role.idLong
            execute()
        }
    }
}

/**
 * @author Kaidan Gustave
 */
abstract class SQLRole(type: String): Table() {
    private val get    = "SELECT ROLE_ID FROM ROLES WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val add    = "INSERT INTO ROLES (GUILD_ID, ROLE_ID, TYPE) VALUES (?, ?, '$type')"
    private val set    = "UPDATE ROLES SET ROLE_ID = ? WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val delete = "DELETE FROM ROLES WHERE GUILD_ID = ? AND TYPE = '$type'"

    fun hasRole(guild: Guild): Boolean = getRole(guild) != null

    fun getRole(guild: Guild): Role? {
        return using(connection.prepareStatement(get)) {
            this[1] = guild.idLong
            using(executeQuery()) {
                if(next()) guild.getRoleById(getLong("ROLE_ID"))
                else null
            }
        }
    }

    fun setRole(role: Role) {
        if(hasRole(role.guild)) {
            using(connection.prepareStatement(set)) {
                this[1] = role.idLong
                this[2] = role.guild.idLong
                execute()
            }
        } else {
            using(connection.prepareStatement(add)) {
                this[1] = role.guild.idLong
                this[2] = role.idLong
                execute()
            }
        }
    }

    fun deleteRole(guild: Guild) {
        using(connection.prepareStatement(delete)) {
            this[1] = guild.idLong
            execute()
        }
    }
}

object SQLRoleMe : SQLRoles("roleme")
object SQLColorMe : SQLRoles("colorme")
object SQLAnnouncementRoles : SQLRoles("announcement")
object SQLModeratorRole : SQLRole("moderator")
object SQLMutedRole : SQLRole("muted")
