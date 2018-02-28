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
@file:Suppress("NOTHING_TO_INLINE")
package xyz.nightfury.util.ext

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.managers.GuildController
import net.dv8tion.jda.core.managers.RoleManagerUpdatable
import net.dv8tion.jda.core.requests.RestAction
import net.dv8tion.jda.core.requests.restaction.RoleAction
import xyz.nightfury.util.functional.AddRemoveBlock
import java.awt.Color

inline fun GuildController.createRole(block: RoleAction.() -> Unit): RoleAction {
    return createRole().apply(block)
}

inline fun RoleAction.name(lazy: () -> String?): RoleAction = setName(lazy())
inline fun RoleAction.color(lazy: () -> Color?): RoleAction = setColor(lazy())
inline fun RoleAction.hoisted(lazy: () -> Boolean): RoleAction = setHoisted(lazy())
inline fun RoleAction.mentionable(lazy: () -> Boolean): RoleAction = setMentionable(lazy())
inline fun RoleAction.permissions(lazy: AddRemoveBlock<Permission>.() -> Unit) {
    val block = object : AddRemoveBlock<Permission> {
        val set = HashSet<Permission>()

        override fun add(element: Permission) {
            set.add(element)
        }

        override fun remove(element: Permission) {
            set.remove(element)
        }
    }

    block.lazy()

    setPermissions(block.set)
}
inline fun RoleAction.permissions(vararg perms: Permission): RoleAction = setPermissions(*perms)

inline fun Role.edit(init: RoleManagerUpdatable.() -> Unit): RestAction<Void> {
    return managerUpdatable.apply(init).update()
}

inline fun RoleManagerUpdatable.name(lazy: () -> String): RoleManagerUpdatable {
    nameField.value = lazy().takeUnless { it.length > 32 || it.isEmpty() } ?: nameField.originalValue
    return this
}

inline fun RoleManagerUpdatable.color(lazy: () -> Color?): RoleManagerUpdatable {
    colorField.value = lazy()
    return this
}

inline fun RoleManagerUpdatable.hoisted(lazy: () -> Boolean): RoleManagerUpdatable {
    hoistedField.value = lazy()
    return this
}

inline fun RoleManagerUpdatable.mentionable(lazy: () -> Boolean): RoleManagerUpdatable {
    mentionableField.value = lazy()
    return this
}

inline fun RoleManagerUpdatable.give(lazy: MutableList<Permission>.() -> Unit): RoleManagerUpdatable {
    permissionField.givePermissions(ArrayList<Permission>().apply(lazy))
    return this
}

inline fun RoleManagerUpdatable.set(lazy: MutableList<Permission>.() -> Unit): RoleManagerUpdatable {
    permissionField.setPermissions(ArrayList<Permission>().apply(lazy))
    return this
}

inline fun RoleManagerUpdatable.revoke(lazy: MutableList<Permission>.() -> Unit): RoleManagerUpdatable {
    permissionField.revokePermissions(ArrayList<Permission>().apply(lazy))
    return this
}
