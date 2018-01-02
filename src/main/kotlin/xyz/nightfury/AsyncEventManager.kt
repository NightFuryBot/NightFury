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
package xyz.nightfury

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Kaidan Gustave
 */
class AsyncEventManager: IEventManager {
    companion object {
        @JvmField val THREAD_NUMBER: AtomicInteger = AtomicInteger(1)
    }

    private val executor: ExecutorService = newCachedThreadPool {
        val thisThreadName = "EventThread-${THREAD_NUMBER.getAndIncrement()}".also {
            NightFury.LOG.debug("Creating new thread: '$it'")
        }

        Thread(it, thisThreadName).also { it.isDaemon = true }
    }

    var isShutdown: Boolean = false

    private val listeners: MutableSet<EventListener> = CopyOnWriteArraySet()

    override fun handle(event: Event?) {
        if(isShutdown || event == null)
            return

        executor.execute {
            listeners.forEach {
                try {
                    it.onEvent(event)
                } catch (e: Throwable) {
                    NightFury.LOG.error("One of the EventListeners caught an exception:", e)
                }
            }
        }

        if(event is ShutdownEvent)
            isShutdown = true
    }

    override fun register(listener: Any?) {
        listeners += requireNotNull(listener as? EventListener) {
            "Listener must implement EventListener or SuspendedEventListener!"
        }
    }

    override fun unregister(listener: Any?) {
        if(listener is EventListener)
            listeners -= listener
    }

    override fun getRegisteredListeners(): MutableList<Any> = mutableListOf(*listeners.toTypedArray())
}