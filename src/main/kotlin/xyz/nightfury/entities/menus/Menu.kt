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
package xyz.nightfury.entities.menus

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
abstract class Menu(builder: Builder<*,*>) {
    protected val waiter: EventWaiter = builder.waiter
    internal val users: Set<User> = builder.users
    internal val roles: Set<Role> = builder.roles
    protected val timeout: Long = builder.timeout
    protected val unit: TimeUnit = builder.unit
    protected val finalAction: (Message) -> Unit = builder.finalAction

    abstract fun displayIn(channel: MessageChannel)
    abstract fun displayAs(message: Message)

    @Suppress("UNCHECKED_CAST")
    abstract class Builder<B: Builder<B, M>, M: Menu> {
        lateinit var waiter: EventWaiter
        var timeout: Long = -1
        var unit: TimeUnit = TimeUnit.SECONDS
        val users: MutableSet<User> = HashSet()
        val roles: MutableSet<Role> = HashSet()
        var finalAction: (Message) -> Unit = { }

        abstract fun build(): M

        operator fun plusAssign(user: User) {
            this + user
        }

        operator fun plus(user: User): B {
            users.add(user)
            return this as B
        }

        operator fun plusAssign(role: Role) {
            this + role
        }

        operator fun plus(role: Role): B {
            roles.add(role)
            return this as B
        }

        infix inline fun waiter(block: () -> EventWaiter): B {
            waiter = block()
            return this as B
        }

        infix inline fun timeout(block: TimeOut.() -> Unit): B {
            TimeOut().apply {
                block()
                timeout = delay
                this@Builder.unit = unit
            }
            return this as B
        }

        infix inline fun role(block: () -> Role): B = plus(block())

        infix inline fun user(block: () -> User): B = plus(block())

        infix inline fun finalAction(crossinline block: (Message) -> Unit): B {
            finalAction = { block(it) }
            return this as B
        }


        infix inline fun displayIn(lazy: () -> MessageChannel) = build().displayIn(lazy())
        infix inline fun displayAs(lazy: () -> Message) = build().displayAs(lazy())
    }
}