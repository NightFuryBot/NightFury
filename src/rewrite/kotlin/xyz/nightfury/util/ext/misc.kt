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
@file:Suppress("unused")
package xyz.nightfury.util.ext

// Collections
inline fun <reified T, reified R> Array<out T>.accumulate(function: (T) -> Collection<R>): List<R> {
    return when {
        this.isEmpty() -> emptyList()
        this.size == 1 -> function(this[0]).toList()
        else -> {
            val list = ArrayList<R>()
            for(element in this) {
                list += function(element)
            }
            return list
        }
    }
}

inline fun <reified T, reified R> Collection<T>.accumulate(function: (T) -> Collection<R>): List<R> {
    return when {
        this.isEmpty() -> emptyList()
        this.size == 1 -> function(this.first()).toList()
        else -> {
            val list = ArrayList<R>()
            for(element in this) {
                list += function(element)
            }
            return list
        }
    }
}

inline fun <reified K, reified V> Array<V>.keyToMap(function: (V) -> K): Map<K, V> {
    return mapOf(*map { function(it) to it }.toTypedArray())
}

inline fun <reified K, reified V> Iterable<V>.keyToMap(function: (V) -> K): Map<K, V> {
    return mapOf(*map { function(it) to it }.toTypedArray())
}

inline fun <reified K, reified V> Array<V>.multikeyToMap(function: (V) -> Iterable<K>): Map<K, V> {
    val map = HashMap<K, V>()
    forEach { v ->
        function(v).forEach { k ->
            map[k] = v
        }
    }
    return map
}

inline fun <reified K, reified V> Iterable<V>.multikeyToMap(function: (V) -> Iterable<K>): Map<K, V> {
    val map = HashMap<K, V>()
    forEach { v ->
        function(v).forEach { k ->
            map[k] = v
        }
    }
    return map
}

inline fun <reified T> Array<T>.sumByLong(transform: (T) -> Long): Long = map(transform).sum()

inline fun <reified T> Iterable<T>.sumByLong(transform: (T) -> Long): Long = map(transform).sum()

inline val <reified E: Enum<E>> E.niceName: String inline get() {
    return name.replace('_', ' ').run {
        if(length < 2) this.toUpperCase()
        else "${this[0].toUpperCase()}${substring(1).toLowerCase()}"
    }
}

// General Extensions
inline fun <reified T> T.modifyIf(condition: Boolean, block: (T) -> T): T = if(condition) block(this) else this
inline fun <reified T> T.modifyUnless(condition: Boolean, block: (T) -> T): T = modifyIf(!condition, block)
