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
@file:Suppress("unused")
@file:JvmName("MenuKt")
package xyz.nightfury.extensions

import com.jagrosh.jdautilities.menu.Menu
import com.jagrosh.jdautilities.waiter.EventWaiter
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.util.concurrent.TimeUnit

/**Automatically builds and displays the [Menu.Builder] in the provided [MessageChannel].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.displayIn(lazy: () -> MessageChannel) = build().display(lazy())
/**Automatically builds and displays the [Menu.Builder] as the provided [Message].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.displayAs(lazy: () -> Message)        = build().display(lazy())

class TimeOut
{
    var delay = 0L
    var unit: TimeUnit = TimeUnit.SECONDS

    infix inline fun delay(lazy: () -> Long) { delay = lazy() }
    infix inline fun unit(lazy: () -> TimeUnit) { unit = lazy() }
}

/**A lazy setter for [Menu.Builder.setUsers].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.users(lazy: () -> Array<out User>) : T = this.setUsers(*lazy()) as T
/**A lazy setter for [Menu.Builder.setUsers] specifying only one [User].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.user(lazy: () -> User) : T = this.setUsers(lazy()) as T
/**A lazy setter for [Menu.Builder.setRoles].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.roles(lazy: () -> Array<out Role>) : T = this.setRoles(*lazy()) as T
/**A lazy setter for [Menu.Builder.setRoles] specifying only one [Role].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.role(lazy: () -> Role) : T = this.setRoles(lazy()) as T
/**A lazy setter for [Menu.Builder.setEventWaiter].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.waiter(lazy: () -> EventWaiter) : T = this.setEventWaiter(lazy()) as T
/**A lazy setter for [Menu.Builder.setColor].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.color(lazy: () -> Color?) : T = this.setColor(lazy()) as T
/**A lazy setter for [Menu.Builder.setTimeout].*/
infix inline fun <reified T : Menu.Builder<*,*>> T.timeout(lazy: TimeOut.() -> Unit) : T = with(TimeOut()) {
    lazy()
    return@with setTimeout(this.delay, this.unit) as T
}