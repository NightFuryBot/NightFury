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

import club.minnced.kjda.RestPromise
import club.minnced.kjda.promise
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.managers.GuildController
import net.dv8tion.jda.core.requests.restaction.RoleAction
import java.awt.Color

infix fun RoleAction.promise(init: RolePromise.() -> Unit) : RestPromise<Role> = with(RolePromise(this))
{
    init()
    promise()
}

infix fun GuildController.createRole(init: RolePromise.() -> Unit) : RestPromise<Role> = this.createRole().promise(init)

class RolePromise internal constructor(private val roleAction: RoleAction)
{
    var name : String? = null
    var color : Color? = null
    val permissions : MutableList<Permission> = ArrayList()
    var isHoisted : Boolean = false
    var isMentionable : Boolean = false

    operator fun component1() = name
    operator fun component2() = color
    operator fun component3() = permissions
    operator fun component4() = isHoisted
    operator fun component5() = isMentionable

    internal fun promise() = with(roleAction)
    {
        val(name, color, permissions, isHoisted, isMentionable) = this@RolePromise
        setName(name)
        setColor(color)
        setPermissions(permissions)
        setHoisted(isHoisted)
        setMentionable(isMentionable)
        return@with promise()
    }

    /** Lazy setter for [name]. */
    infix inline fun name(lazy: () -> String?) : RolePromise
    {
        this.name = lazy()
        return this
    }

    /** Lazy setter for [color]. */
    infix inline fun color(lazy: () -> Color?) : RolePromise
    {
        this.color = lazy()
        return this
    }

    /** Lazy block for modifying [permissions]. */
    infix inline fun permissions(lazy: MutableList<Permission>.() -> Unit) : RolePromise
    {
        lazy(permissions)
        return this
    }

    /** Clears all existing [Permission] set for this [RolePromise]. */
    fun clearPermissions() : RolePromise
    {
        permissions.clear()
        return this
    }

    /** Lazy setter for [isHoisted]. */
    infix inline fun hoisted(lazy: () -> Boolean) : RolePromise
    {
        this.isHoisted = lazy()
        return this
    }

    /** Lazy setter for [isMentionable]. */
    infix inline fun mentionable(lazy: () -> Boolean) : RolePromise
    {
        this.isMentionable = lazy()
        return this
    }
}