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
@file:Suppress("Unused")
package xyz.nightfury.entities.menus

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * @author Kaidan Gustave
 */
class EventWaiter : EventListener {
    private val events: MutableMap<KClass<*>, MutableList<WaiterEvent<*>>> = HashMap()
    private val dispatcher: ThreadPoolDispatcher = newSingleThreadContext("EventWaiter")

    inline fun <reified E: Event> waitFor(
        delay: Long = -1, unit: TimeUnit = TimeUnit.SECONDS,
        noinline timeout: () -> Unit = {},
        noinline condition: (E) -> Boolean,
        noinline action: (E) -> Unit
    ) = waitForEvent(E::class, condition, action, delay, unit, timeout)

    @Suppress("UNCHECKED_CAST")
    fun <E: Event> waitForEvent(
        klazz: KClass<E>, condition: (E) -> Boolean, action: (E) -> Unit,
        delay: Long = -1, unit: TimeUnit = TimeUnit.SECONDS, timeout: () -> Unit = {}
    ) {
        val eventList: MutableList<WaiterEvent<E>> = events[klazz].let {
            it as? MutableList<WaiterEvent<E>> ?: ArrayList<WaiterEvent<E>>().apply {
                events.put(klazz, this as MutableList<WaiterEvent<*>>)
            }
        }

        val waiting: WaiterEvent<E> = WaiterEvent(condition, action)
        eventList.add(waiting)

        if(delay > 0) {
            launch(dispatcher) {
                delay(delay, unit)
                if(eventList.remove(waiting))
                    timeout()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatchEventType(event: Event, klazz: KClass<*>) {
        val list = events[klazz] ?: return
        list -= list.toMutableList().filter { (it as WaiterEvent<Event>)(event) }
    }

    override fun onEvent(event: Event) {
        if(event is ShutdownEvent)
            return dispatcher.close()

        val klazz = event::class

        dispatchEventType(event, klazz)

        klazz.superclasses.forEach {
            dispatchEventType(event, klazz)
        }
    }

    private inner class WaiterEvent<in T: Event>
    constructor(internal val condition: (T) -> Boolean, internal val action: (T) -> Unit): (T) -> Boolean {
        override fun invoke(event: T): Boolean {
            if(condition(event)) {
                action(event)
                return true
            }
            return false
        }
    }
}