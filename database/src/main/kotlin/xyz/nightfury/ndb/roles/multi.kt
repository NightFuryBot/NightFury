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
package xyz.nightfury.ndb.roles

import xyz.nightfury.ndb.Database
import xyz.nightfury.ndb.internal.*

/**
 * @author Kaidan Gustave
 */
abstract class MultiRoleHandler(type: DBRoleType): Database.Table() {
    init {
        require(type.isMulti) { "Singleton role DB handler cannot be created for multi-type '$type'" }
    }

    private val isRole     = "SELECT * FROM GUILD_ROLES WHERE GUILD_ID = ? AND ROLE_ID = ? AND TYPE = '$type'"
    private val getRole    = "SELECT ROLE_ID FROM GUILD_ROLES WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val addRole    = "INSERT INTO GUILD_ROLES(GUILD_ID, ROLE_ID, TYPE) VALUES (?, ?, '$type')"
    private val removeRole = "DELETE FROM GUILD_ROLES WHERE GUILD_ID = ? AND ROLE_ID = ? AND TYPE = '$type'"

    fun isRole(guildId: Long, roleId: Long): Boolean = sql(false) {
        any(isRole) {
            this[1] = guildId
            this[2] = roleId
        }
    }

    fun getRoles(guildId: Long): List<Long> = sql({ emptyList() }) {
        val roles = ArrayList<Long>()
        statement(getRole) {
            this[1] = guildId
            queryAll { roles += it.get<Long>("ROLE_ID")!! }
        }
        return roles
    }

    fun addRole(guildId: Long, roleId: Long) = sql {
        execute(addRole) {
            this[1] = guildId
            this[2] = roleId
        }
    }

    fun removeRole(guildId: Long, roleId: Long) = sql {
        execute(removeRole) {
            this[1] = guildId
            this[2] = roleId
        }
    }
}

object RoleMeHandler : MultiRoleHandler(DBRoleType.ROLE_ME)
object ColorMeHandler : MultiRoleHandler(DBRoleType.COLOR_ME)
object AnnouncementRolesHandler : MultiRoleHandler(DBRoleType.ANNOUNCEMENTS)
