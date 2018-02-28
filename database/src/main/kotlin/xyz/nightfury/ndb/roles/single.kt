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

import xyz.nightfury.ndb.*

/**
 * @author Kaidan Gustave
 */
abstract class SingleRoleHandler(type: DBRoleType): Database.Table() {
    init {
        require(!type.isMulti) { "Singleton role DB handler cannot be created for multi-type '$type'" }
    }

    private val getRole    = "SELECT ROLE_ID FROM GUILD_ROLES WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val addRole    = "INSERT INTO GUILD_ROLES (GUILD_ID, ROLE_ID, TYPE) VALUES (?, ?, '$type')"
    private val setRole    = "UPDATE GUILD_ROLES SET ROLE_ID = ? WHERE GUILD_ID = ? AND TYPE = '$type'"
    private val removeRole = "DELETE FROM GUILD_ROLES WHERE GUILD_ID = ? AND TYPE = '$type'"

    fun hasRole(guildId: Long): Boolean = sql(false) {
        any(getRole) {
            this[1] = guildId
        }
    }

    fun getRole(guildId: Long): Long = sql(0L) {
        statement(getRole) {
            this[1] = guildId
            query(0L) { it.getLong("ROLE_ID") }
        }
    }

    fun setRole(guildId: Long, roleId: Long) = sql {
        if(hasRole(guildId)) {
            execute(setRole) {
                this[1] = roleId
                this[2] = guildId
            }
        } else {
            execute(addRole) {
                this[1] = guildId
                this[2] = roleId
            }
        }
    }

    fun removeRole(guildId: Long) = sql {
        execute(removeRole) {
            this[1] = guildId
        }
    }
}

object ModRoleHandler : SingleRoleHandler(DBRoleType.MODERATOR)
object MutedRoleHandler : SingleRoleHandler(DBRoleType.MUTED)
