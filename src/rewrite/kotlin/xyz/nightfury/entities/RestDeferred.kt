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
@file:Suppress("MemberVisibilityCanBePrivate", "Unused")
package xyz.nightfury.entities

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.requests.RestAction
import xyz.nightfury.util.createLogger
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
class RestDeferred<out T>(
    private val action: RestAction<T>,
    val context: CoroutineContext = DefaultDispatcher,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    success: ((T) -> Unit)? = null,
    failure: ((Throwable) -> Unit)? = null
): Deferred<T> by async(context, start, block = {
    return@async suspendCoroutine<T> { cont ->
        action.queue({
            success?.invoke(it)
            cont.resume(it)
        }, {
            failure?.invoke(it)
            cont.resumeWithException(it)
        })
    }
}) {
    companion object {
        val LOG = createLogger(RestDeferred::class)
        val DEFAULT_FAILURE = { t: Throwable ->
            LOG.warn("A RestDeferred encountered an exception while processing", t)
        }
    }

    fun promise() {}

    fun promise(then: suspend (T) -> Unit) {
        launch(context) {
            try {
                then(this@RestDeferred.await())
            } catch(t: Throwable) {
                DEFAULT_FAILURE(t)
            }
        }
    }

    fun promise(then: suspend (T) -> Unit, catch: suspend (Throwable) -> Unit) {
        launch(context) {
            try {
                then(this@RestDeferred.await())
            } catch(t: Throwable) {
                catch(t)
            }
        }
    }
}
