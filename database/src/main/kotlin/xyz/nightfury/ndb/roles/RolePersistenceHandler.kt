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
object RolePersistenceHandler : Database.Table() {
    private const val GET_ROLE_PERSIST = "SELECT ROLE_IDS FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?"
    private const val ADD_ROLE_PERSIST = "INSERT INTO ROLE_PERSIST(GUILD_ID, USER_ID, ROLE_IDS) VALUES (?,?,?)"
    private const val SET_ROLE_PERSIST = "UPDATE ROLE_PERSIST SET ROLE_IDS = ? WHERE GUILD_ID = ? AND USER_ID = ?"
    private const val REMOVE_ROLE_PERSIST = "DELETE FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?"

    fun hasRolePersist(guildId: Long, userId: Long): Boolean = sql(false) {
        any(GET_ROLE_PERSIST) {
            this[1] = guildId
            this[2] = userId
        }
    }

    fun getRolePersist(guildId: Long, userId: Long): List<Long> = sql({ emptyList() }) {
        val roleIds = ArrayList<Long>()
        statement(GET_ROLE_PERSIST) {
            this[1] = guildId
            this[2] = userId
            query {
                val array = it.getArray("ROLE_IDS").array as LongArray
                array.forEach { roleIds.add(it) }
            }
        }
        return@sql roleIds
    }

    fun setRolePersist(guildId: Long, userId: Long, vararg roleIds: Long) = sql {
        if(hasRolePersist(guildId, userId)) {
            execute(SET_ROLE_PERSIST) {
                this[1] = roleIds
                this[2] = guildId
                this[3] = userId
            }
        } else {
            execute(ADD_ROLE_PERSIST) {
                this[1] = guildId
                this[2] = userId
                this[3] = roleIds
            }
        }
    }

    fun removeRolePersist(guildId: Long, userId: Long) = sql {
        execute(REMOVE_ROLE_PERSIST) {
            this[1] = guildId
            this[2] = userId
        }
    }
}