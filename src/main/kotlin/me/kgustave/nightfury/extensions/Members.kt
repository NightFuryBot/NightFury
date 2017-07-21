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
package me.kgustave.nightfury.extensions

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role


/**
 * @author Kaidan Gustave
 */
val Member.isAdmin: Boolean
    get() = permissions.contains(Permission.ADMINISTRATOR) || this.isOwner

fun Member.ban(delDays: Int) = guild.controller.ban(this, delDays)!!
fun Member.ban(delDays: Int, reason: String) = guild.controller.ban(this, delDays, reason)!!

fun Member.kick() = guild.controller.kick(this)!!
fun Member.kick(reason: String) = guild.controller.kick(this, reason)!!

fun Member.giveRole(role: Role) = guild.controller.addRolesToMember(this, role)!!
fun Member.giveRoles(vararg roles: Role) = guild.controller.addRolesToMember(this, *roles)!!
fun Member.giveRoles(roles: Collection<Role>) = guild.controller.addRolesToMember(this, roles)!!

fun Member.removeRole(role: Role) = guild.controller.removeRolesFromMember(this, role)!!
fun Member.removeRoles(vararg roles: Role) = guild.controller.removeRolesFromMember(this, *roles)!!
fun Member.removeRoles(roles: Collection<Role>) = guild.controller.removeRolesFromMember(this, roles)!!