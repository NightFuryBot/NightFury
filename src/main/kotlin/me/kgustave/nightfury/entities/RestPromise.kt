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
@file:Suppress("unused")
package me.kgustave.nightfury.entities

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.coroutines.experimental.CoroutineContext

// Copied and modified from club.minnced.kjda.RestPromise

inline val <reified V> RestAction<V?>.promise : RestPromise<V>
    inline get() = RestPromise(this)

fun<V> RestAction<V?>.promise() = RestPromise(this)

infix fun<V> RestAction<V?>.then(apply: V?.() -> Unit) = promise() then apply

infix fun<V> RestAction<V?>.catch(apply: Throwable?.() -> Unit) = promise() catch apply

fun<V> RestAction<V?>.onlyIf(condition: Boolean, block: V?.() -> Unit = {}) = if(condition) then(block) else promise()

fun<V> RestAction<V?>.unless(condition: Boolean, block: V?.() -> Unit = {}) = if(!condition) then(block) else promise()

fun<V> RestAction<V?>.prepare(context: CoroutineContext = CommonPool) = async(context, false) { promise() }

fun<V> RestAction<V?>.start(context: CoroutineContext = CommonPool) = launch(context) { queue() }

suspend fun<V> RestAction<V?>.get(context: CoroutineContext = CommonPool) = run(context) { complete() }

suspend fun<V> RestAction<V?>.after(time: Long, unit: TimeUnit = MILLISECONDS, context: CoroutineContext = CommonPool) = run(context) {
    delay(time, unit)
    get()
}

class RestPromise<V>(action: RestAction<V?>)
{
    private val success = Callback<V>()
    private val failure = Callback<Throwable>()

    infix fun then(lazyCallback: (V?) -> Unit): RestPromise<V>
    {
        success.backing = lazyCallback
        return this
    }

    infix fun catch(lazyHandler: (Throwable?) -> Unit): RestPromise<V>
    {
        failure.backing = lazyHandler
        return this
    }

    init
    {
        action.queue(success, failure)
    }
}

@FunctionalInterface
internal class Callback<T> : (T?) -> Unit
{
    private var finishedValue : T?      = null
    private var finished      : Boolean = false

    var backing : (T?) -> Unit = {}
        set(value) {
            if(finished)
                value(finishedValue)
            field = value
        }

    override fun invoke(p1: T?)
    {
        finished = true
        finishedValue = p1
        backing(p1)
    }
}
