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
@file:Suppress("Unused")
package xyz.nightfury.listeners

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import xyz.nightfury.util.createLogger
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * @author Kaidan Gustave
 */
class EventWaiter
private constructor(private val dispatcher: CoroutineDispatcher): EventListener, CoroutineContext by dispatcher {
    companion object {
        private val LOG = createLogger(EventWaiter::class)
    }

    private val tasks = HashMap<KClass<*>, MutableList<ITask<*>>>()

    constructor(): this(newCachedThreadPool(Factory()).asCoroutineDispatcher())

    inline fun <reified E: Event> waitFor(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline timeout: (suspend () -> Unit)? = null,
        noinline condition: suspend (E) -> Boolean,
        noinline action: suspend (E) -> Unit
    ) = waitForEvent(E::class, condition, action, delay, unit, timeout)

    inline fun <reified E: Event> receive(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline condition: suspend (E) -> Boolean
    ): Deferred<E?> = receiveEvent(E::class, condition, delay, unit)

    fun <E: Event> waitForEvent(
        klazz: KClass<E>,
        condition: suspend (E) -> Boolean,
        action: suspend (E) -> Unit,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        timeout: (suspend () -> Unit)? = null
    ) {
        val eventList = getTaskListType(klazz)

        val waiting = QueuedTask(condition, action)

        eventList += waiting

        if(delay > 0) {
            launch(this) {
                delay(delay, unit)
                if(eventList.remove(waiting))
                    timeout?.invoke()
            }
        }
    }

    fun <E: Event> receiveEvent(
        klazz: KClass<E>,
        condition: suspend (E) -> Boolean,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS
    ): Deferred<E?> {
        val eventList = getTaskListType(klazz)

        val deferred = CompletableDeferred<E?>()
        val waiting = AwaitableTask(condition, deferred)

        eventList += waiting

        if(delay > 0) {
            launch(this) {
                delay(delay, unit)
                eventList.remove(waiting)
                // The receiveEvent method is supposed to return null
                // if no matching Events are fired within its
                // lifecycle.
                // Regardless of whether or not the AwaitableTask
                // was removed, we invoke this. In the event that
                // it has not completed, we need to make sure the
                // coroutine does not deadlock.
                deferred.complete(null)
            }
        }

        return deferred
    }

    private fun <E: Event> getTaskListType(klazz: KClass<E>): MutableList<ITask<E>> {
        @Suppress("UNCHECKED_CAST")
        return tasks[klazz].let {
            it as? MutableList<ITask<E>> ?: ArrayList<ITask<E>>().apply {
                tasks[klazz] = this as MutableList<ITask<*>>
            }
        }
    }

    private suspend fun <T: Event> dispatchEventType(event: T, klazz: KClass<*>) {
        val list = tasks[klazz] ?: return
        list -= list.toMutableList().filter {
            @Suppress("UNCHECKED_CAST")
            val waiting = (it as ITask<T>)
            waiting(event)
        }
    }

    override fun onEvent(event: Event) {
        launch(dispatcher) {
            val klazz = event::class
            dispatchEventType(event, klazz)
            klazz.superclasses.forEach { dispatchEventType(event, it) }
        }
    }

    private interface ITask<in T: Event> {
        suspend operator fun invoke(event: T): Boolean
    }

    private class QueuedTask<in T: Event>(
        private val condition: suspend (T) -> Boolean,
        private val action: suspend (T) -> Unit
    ): ITask<T> {
        override suspend operator fun invoke(event: T): Boolean {
            if(condition(event)) {
                try {
                    action(event)
                } catch(ignored: Throwable) {}
                return true
            }
            return false
        }
    }

    private class AwaitableTask<in T: Event>(
        private val condition: suspend (T) -> Boolean,
        private val completion: CompletableDeferred<T?>
    ): ITask<T> {
        override suspend operator fun invoke(event: T): Boolean {
            try {
                if(condition(event)) {
                    completion.complete(event)
                    return true
                }
                return false
            } catch(t: Throwable) {
                // In the case this ever throws an error,
                // we need to complete this exceptionally.
                completion.completeExceptionally(t)
                return true
            }
        }
    }

    private class Factory: ThreadFactory {
        private val number = AtomicInteger(0)
        private val name: String get() = "EventWaiter Thread - ${number.getAndIncrement()}"
        override fun newThread(r: Runnable): Thread = Thread(r, name).also { it.isDaemon = true }
    }
}
