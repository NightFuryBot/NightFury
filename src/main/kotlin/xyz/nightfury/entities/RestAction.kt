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
package xyz.nightfury.entities

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

// Copied and modified from club.minnced.kjda.RestPromise

fun <V> RestAction<V?>.promise() = RestPromise(this)

infix fun <V> RestAction<V?>.then(apply: V?.() -> Unit): RestPromise<V> = promise() then apply

infix fun <V> RestAction<V?>.catch(apply: Throwable?.() -> Unit): RestPromise<V> = promise() catch apply

fun <V> RestAction<V?>.onlyIf(condition: Boolean, block: V?.() -> Unit = {}): RestPromise<V>
    = if(condition) then(block) else promise()

fun <V> RestAction<V?>.unless(condition: Boolean, block: V?.() -> Unit = {}): RestPromise<V>
    = if(!condition) then(block) else promise()

suspend fun <V> RestAction<V?>.await(): V? = suspendCoroutine { cont ->
    queue({ cont.resume(it) }, { cont.resumeWithException(it) })
}

suspend fun <V> RestAction<V?>.awaitAfter(time: Long, unit: TimeUnit = TimeUnit.SECONDS): V? {
    delay(time, unit)
    return await()
}

class RestPromise<V>(action: RestAction<V?>) {
    private val success = Callback<V>()
    private val failure = Callback<Throwable>()

    infix fun then(lazyCallback: (V?) -> Unit): RestPromise<V> {
        success.backing = lazyCallback
        return this
    }

    infix fun catch(lazyHandler: (Throwable?) -> Unit): RestPromise<V> {
        failure.backing = lazyHandler
        return this
    }

    init {
        action.queue(success, failure)
    }
}

internal class Callback<T> : (T?) -> Unit {
    private var finishedValue : T?      = null
    private var finished      : Boolean = false

    var backing : (T?) -> Unit = {}
        set(value) {
            if(finished)
                value(finishedValue)
            field = value
        }

    override fun invoke(p1: T?) {
        finished = true
        finishedValue = p1
        backing(p1)
    }
}
