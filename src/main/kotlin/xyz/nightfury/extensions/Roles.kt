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
@file:Suppress("UNUSED")
package xyz.nightfury.extensions

import xyz.nightfury.entities.promise
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.managers.GuildController
import net.dv8tion.jda.core.managers.RoleManagerUpdatable
import net.dv8tion.jda.core.requests.restaction.RoleAction
import xyz.nightfury.entities.RestPromise
import java.awt.Color

infix inline fun GuildController.promiseRole(init: RoleAction.() -> Unit) = createRole() promise(init)

infix inline fun RoleAction.promise(init: RoleAction.() -> Unit): RestPromise<Role> {
    init()
    return promise()
}

infix inline fun Role.edit(init: RoleManagerUpdatable.() -> Unit): RestPromise<Void> {
    return managerUpdatable.run {
        init()
        update().promise()
    }
}

infix inline fun RoleManagerUpdatable.name(lazy: () -> String): RoleManagerUpdatable {
    nameField.value = lazy().takeUnless { it.length > 32 || it.isEmpty() } ?: nameField.originalValue
    return this
}
infix inline fun RoleManagerUpdatable.color(lazy: () -> Color?): RoleManagerUpdatable {
    colorField.value = lazy()
    return this
}
infix inline fun RoleManagerUpdatable.hoisted(lazy: () -> Boolean): RoleManagerUpdatable {
    hoistedField.value = lazy()
    return this
}
infix inline fun RoleManagerUpdatable.mentionable(lazy: () -> Boolean): RoleManagerUpdatable {
    mentionableField.value = lazy()
    return this
}
infix inline fun RoleManagerUpdatable.give(lazy: MutableList<Permission>.() -> Unit): RoleManagerUpdatable {
    permissionField.givePermissions(ArrayList<Permission>().apply(lazy))
    return this
}
infix inline fun RoleManagerUpdatable.set(lazy: MutableList<Permission>.() -> Unit): RoleManagerUpdatable {
    permissionField.setPermissions(ArrayList<Permission>().apply(lazy))
    return this
}
infix inline fun RoleManagerUpdatable.revoke(lazy: MutableList<Permission>.() -> Unit): RoleManagerUpdatable {
    permissionField.revokePermissions(ArrayList<Permission>().apply(lazy))
    return this
}

infix inline fun RoleAction.name(lazy: () -> String?): RoleAction = setName(lazy())
infix inline fun RoleAction.color(lazy: () -> Color?): RoleAction = setColor(lazy())
infix inline fun RoleAction.hoisted(lazy: () -> Boolean): RoleAction = setHoisted(lazy())
infix inline fun RoleAction.mentionable(lazy: () -> Boolean): RoleAction = setMentionable(lazy())
infix inline fun RoleAction.permissions(lazy: MutableList<Permission>.() -> Unit): RoleAction = with(ArrayList<Permission>()) {
    lazy()
    setPermissions(this)
}
