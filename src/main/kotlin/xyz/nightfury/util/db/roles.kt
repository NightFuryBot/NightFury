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
import xyz.nightfury.ndb.roles.*
import xyz.nightfury.util.jda.giveRole
import xyz.nightfury.util.jda.removeRole

inline var <reified R: Role> R.isRoleMe: Boolean
    inline get() = RoleMeHandler.isRole(guild.idLong, idLong)
    inline set(value) {
        if(value) RoleMeHandler.addRole(guild.idLong, idLong)
        else RoleMeHandler.removeRole(guild.idLong, idLong)
    }

inline var <reified R: Role> R.isColorMe: Boolean
    inline get() = ColorMeHandler.isRole(guild.idLong, idLong)
    inline set(value) {
        if(value) ColorMeHandler.addRole(guild.idLong, idLong)
        else ColorMeHandler.removeRole(guild.idLong, idLong)
    }

inline var <reified R: Role> R.isAnnouncements: Boolean
    inline get() = AnnouncementRolesHandler.isRole(guild.idLong, idLong)
    inline set(value) {
        if(value) AnnouncementRolesHandler.addRole(guild.idLong, idLong)
        else AnnouncementRolesHandler.removeRole(guild.idLong, idLong)
    }

inline val <reified G: Guild> G.hasModRole: Boolean inline get() {
    return ModRoleHandler.hasRole(idLong)
}

inline val <reified G: Guild> G.hasMutedRole: Boolean inline get() {
    return MutedRoleHandler.hasRole(idLong)
}

inline var <reified G: Guild> G.modRole: Role?
    inline get() = getRoleById(ModRoleHandler.getRole(idLong))
    inline set(value) {
        if(value !== null) ModRoleHandler.setRole(idLong, value.idLong)
        else ModRoleHandler.removeRole(idLong)
    }

inline var <reified G: Guild> G.mutedRole: Role?
    inline get() = getRoleById(MutedRoleHandler.getRole(idLong))
    inline set(value) {
        if(value !== null) MutedRoleHandler.setRole(idLong, value.idLong)
        else MutedRoleHandler.removeRole(idLong)
    }

inline var <reified M: Member> M.isMod: Boolean
    inline get() = guild.modRole?.let { it in roles } == true
    inline set(value) {
        guild.modRole?.let { if(value) removeRole(it) else giveRole(it) }
    }

inline val <reified G: Guild> G.roleMeRoles: List<Role> inline get() {
    return RoleMeHandler.getRoles(idLong).mapNotNull { getRoleById(it) }
}

inline val <reified G: Guild> G.colorMeRoles: List<Role> inline get() {
    return ColorMeHandler.getRoles(idLong).mapNotNull { getRoleById(it) }
}

inline val <reified G: Guild> G.announcementRoles: List<Role> inline get() {
    return AnnouncementRolesHandler.getRoles(idLong).mapNotNull { getRoleById(it) }
}
