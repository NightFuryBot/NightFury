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

import club.minnced.kjda.RestPromise
import club.minnced.kjda.promise
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.requests.restaction.RoleAction
import java.awt.Color

infix fun RoleAction.build(init: RoleBuilder.() -> Unit) : Unit = with(RoleBuilder(this))
{
    init()
    build()
}

infix fun RoleAction.buildPromise(init: RoleBuilder.() -> Unit) : RestPromise<Role> = with(RoleBuilder(this))
{
    init()
    buildPromise()
}

class RoleBuilder internal constructor(private val roleAction: RoleAction)
{
    var name : String? = null
    var color : Color? = null
    var permissions : List<Permission> = ArrayList()
    var isHoisted : Boolean = false
    var isMentionable : Boolean = false

    operator fun component1() = name
    operator fun component2() = color
    operator fun component3() = permissions
    operator fun component4() = isHoisted
    operator fun component5() = isMentionable

    fun build() = with(roleAction)
    {
        val(name, color, permissions, isHoisted, isMentionable) = this@RoleBuilder
        if(name!=null)
            setName(name)
        if(color!=null)
            setColor(color)
        if(permissions.isNotEmpty())
            setPermissions(permissions)
        setHoisted(isHoisted)
        setMentionable(isMentionable)
        return@with queue()
    }

    fun buildPromise() = with(roleAction)
    {
        val(name, color, permissions, isHoisted, isMentionable) = this@RoleBuilder
        if(name!=null)
            setName(name)
        if(color!=null)
            setColor(color)
        if(permissions.isNotEmpty())
            setPermissions(permissions)
        setHoisted(isHoisted)
        setMentionable(isMentionable)
        return@with promise()
    }
}