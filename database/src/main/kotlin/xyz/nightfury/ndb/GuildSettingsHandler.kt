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
package xyz.nightfury.ndb

import xyz.nightfury.ndb.entities.GuildSettings

/**
 * @author Kaidan Gustave
 */
object GuildSettingsHandler : Database.Table() {
    private const val GET_SETTINGS = "SELECT * FROM GUILD_SETTINGS WHERE GUILD_ID = ?"
    private const val ADD_SETTINGS = "INSERT INTO GUILD_SETTINGS(GUILD_ID, IS_ROLE_PERSIST) VALUES (?, ?)"
    private const val SET_SETTINGS = "UPDATE GUILD_SETTINGS SET IS_ROLE_PERSIST = ? WHERE GUILD_ID = ?"
    private const val REMOVE_SETTINGS = "DELETE FROM GUILD_SETTINGS WHERE GUILD_ID = ?"

    fun hasSettings(guildId: Long): Boolean = sql(false) {
        any(GET_SETTINGS) {
            this[1] = guildId
        }
    }

    fun getSettings(guildId: Long): GuildSettings? = sql {
        statement(GET_SETTINGS) {
            this[1] = guildId
            query { GuildSettings(guildId, it.getBoolean("IS_ROLE_PERSIST")) }
        }
    }

    fun setSettings(settings: GuildSettings) = sql {
        if(!hasSettings(settings.guildId)) {
            execute(ADD_SETTINGS) {
                this[1] = settings.guildId
                this[2] = settings.isRolePersist
            }
        } else {
            execute(SET_SETTINGS) {
                this[1] = settings.isRolePersist
                this[2] = settings.guildId
            }
        }
    }

    fun removeSettings(guildId: Long) = sql {
        execute(REMOVE_SETTINGS) {
            this[1] = guildId
        }
    }
}