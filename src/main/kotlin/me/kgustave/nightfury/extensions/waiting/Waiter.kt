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
package me.kgustave.nightfury.extensions.waiting

import com.jagrosh.jdautilities.waiter.EventWaiter
import net.dv8tion.jda.core.events.Event
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate

@Suppress("UNCHECKED_CAST")
infix fun <T : Event> EventWaiter.waitFor(init: KWaitingEventBuilder<T>.() -> Unit) : Unit = with(KWaitingEventBuilder<T>(this))
{
    init()
    build()
}

class KWaitingEventBuilder<T : Event> internal constructor(internal val waiter: EventWaiter)
{
    @Suppress("UNCHECKED_CAST")
    val type : Class<T> = Event::class.java as Class<T>
    var condition : (T) -> Boolean = { true }
    var action : (T) -> Unit = {}
    var delay : Long = -1
    var unit : TimeUnit? = null
    var timeOutAction: () -> Unit = {}

    operator fun component1() = condition
    operator fun component2() = action
    operator fun component3() = delay
    operator fun component4() = unit
    operator fun component5() = timeOutAction

    infix fun then(action: (T) -> Unit) : KWaitingEventBuilder<T>
    {
        this.action = action
        return this
    }

    fun timeoutOf(delay: Long, unit: TimeUnit) : KWaitingEventBuilder<T>
    {
        this.delay = delay
        this.unit = unit
        return this
    }

    internal fun build()
    {
        val(condition, action, delay, unit, timeOutAction) = this@KWaitingEventBuilder
        if(delay != -1L && unit != null)
            waiter.waitForEvent(type, Predicate(condition), Consumer(action))
        else
            waiter.waitForEvent(type, Predicate(condition), Consumer(action), delay, unit, Runnable(timeOutAction))
    }
}