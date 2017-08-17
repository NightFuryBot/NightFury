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
@file:Suppress("UNUSED")
package me.kgustave.nightfury.extensions

import club.minnced.kjda.promise
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.managers.GuildController
import net.dv8tion.jda.core.requests.restaction.RoleAction
import java.awt.Color

infix inline fun GuildController.createRole(init: RoleAction.() -> Unit) = createRole() promise(init)

infix inline fun RoleAction.promise(init: RoleAction.() -> Unit) = with(this) { init(); promise() }

infix inline fun RoleAction.name(lazy: () -> String?) = setName(lazy())!!
infix inline fun RoleAction.color(lazy: () -> Color?) = setColor(lazy())!!
infix inline fun RoleAction.hoisted(lazy: () -> Boolean) = setHoisted(lazy())!!
infix inline fun RoleAction.mentionable(lazy: () -> Boolean) = setMentionable(lazy())!!
infix inline fun RoleAction.permissions(lazy: MutableList<Permission>.() -> Unit) = with(ArrayList<Permission>()) {
    lazy()
    setPermissions(this)!!
}