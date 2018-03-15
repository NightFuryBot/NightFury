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
package xyz.nightfury.util.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import xyz.nightfury.ndb.entities.GuildSettings
import xyz.nightfury.ndb.GuildSettingsHandler as Settings
import xyz.nightfury.ndb.roles.RolePersistenceHandler as RolePersist

inline var <reified G: Guild> G.settings: GuildSettings?
    inline get() = Settings.getSettings(idLong)
    inline set(value) {
        if(value === null) {
            Settings.removeSettings(idLong)
        } else {
            Settings.setSettings(value)
        }
    }

inline val <reified G: Guild> G.hasSettings: Boolean inline get() {
    return Settings.hasSettings(idLong)
}

// Role Persist
inline val <reified M: Member> M.hasRolePersist: Boolean inline get() {
    return RolePersist.hasRolePersist(guild.idLong, user.idLong)
}

inline fun <reified M: Member> M.saveRolePersist() {
    RolePersist.setRolePersist(guild.idLong, user.idLong, *roles.map { it.idLong }.toLongArray())
}

inline val <reified M: Member> M.rolePersist: List<Role> inline get() {
    return RolePersist.getRolePersist(guild.idLong, user.idLong).mapNotNull { guild.getRoleById(it) }
}

inline fun <reified M: Member> M.removeRolePersist() {
    RolePersist.removeRolePersist(guild.idLong, user.idLong)
}
