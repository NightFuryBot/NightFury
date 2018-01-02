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
package xyz.nightfury.resources

import kotlinx.coroutines.experimental.*
import java.util.concurrent.Executors.*
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext

class CachedThreadPoolDispatcher internal constructor(threadFactory: (Runnable) -> Thread): CoroutineDispatcher() {
    private val baseDispatch: CoroutineDispatcher = newCachedThreadPool(threadFactory).asCoroutineDispatcher()

    override val key: CoroutineContext.Key<*>
        get() = baseDispatch.key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return baseDispatch.interceptContinuation(continuation)
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        baseDispatch.dispatch(context, block)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return baseDispatch.isDispatchNeeded(context)
    }

    override fun toString(): String {
        return baseDispatch.toString()
    }
}

fun newCachedThreadContext(factory: (Runnable) -> Thread): CachedThreadPoolDispatcher = CachedThreadPoolDispatcher(factory)