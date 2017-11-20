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
package xyz.nightfury.menus

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
abstract class Menu(
    protected val waiter: EventWaiter,
    internal val users: Set<User>,
    internal val roles: Set<Role>,
    protected val timeout: Long,
    protected val unit: TimeUnit,
    protected val finalAction: (Message) -> Unit
) {
    abstract fun displayIn(channel: MessageChannel)
    abstract fun displayAs(message: Message)

    @Suppress("UNCHECKED_CAST")
    abstract class Builder<B: Builder<B, M>, M: Menu> {
        protected lateinit var waiter: EventWaiter
        protected var timeout: Long = -1
        protected var unit: TimeUnit = TimeUnit.SECONDS
        protected val users: MutableSet<User> = HashSet()
        protected val roles: MutableSet<Role> = HashSet()
        protected var finalAction: (Message) -> Unit = { }

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

        protected infix inline fun waiter(block: () -> EventWaiter): B {
            waiter = block()
            return this as B
        }

        protected infix inline fun timeout(block: () -> Long?): B {
            timeout = block() ?: -1
            return this as B
        }

        protected infix inline fun unit(block: () -> TimeUnit?): B {
            unit = block() ?: TimeUnit.SECONDS.also { timeout = -1 }
            return this as B
        }

        protected infix inline fun role(block: () -> Role): B = plus(block())

        protected infix inline fun user(block: () -> User): B = plus(block())
    }
}