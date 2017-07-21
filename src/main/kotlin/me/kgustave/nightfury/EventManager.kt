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
package me.kgustave.nightfury

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import net.dv8tion.jda.core.utils.SimpleLog
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author Kaidan Gustave
 */
class EventManager : IEventManager {

    private val listeners: CopyOnWriteArrayList<EventListener> = CopyOnWriteArrayList()
    private val threadpool: ExecutorService = Executors.newFixedThreadPool(100)

    companion object {
        private val LOG = SimpleLog.getLog("EventManager")
    }

    override fun handle(event: Event) {
        if(!threadpool.isShutdown) {
            threadpool.submit {
                listeners.forEach {
                    try { it.onEvent(event) }
                    catch(e: Throwable) {
                        LOG.fatal("An EventListener caught an Exception!")
                        LOG.log(e)
                    }
                }
                if(event is ShutdownEvent)
                    shutdown()
            }
        }
    }

    override fun register(listener: Any) {
        if(listener is EventListener)
            listeners.add(listener)
        else throw IllegalArgumentException("Object must implement EventListener!")
    }

    override fun unregister(listener: Any) {
        if(listener is EventListener && listeners.contains(listener))
            listeners.remove(listener)
    }

    override fun getRegisteredListeners() : MutableList<EventListener> = listeners.toMutableList()

    private fun shutdown() = threadpool.shutdown()
}