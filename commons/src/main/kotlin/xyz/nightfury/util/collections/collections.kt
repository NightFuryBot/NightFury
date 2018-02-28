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
package xyz.nightfury.util.collections

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

// Shortcuts

fun <T> unmodifiableList(list: List<T>): List<T> {
    return Collections.unmodifiableList(list)
}

fun <T> unmodifiableList(vararg elements: T): List<T> {
    return FixedSizeArrayList(*elements)
}

fun <T: Any> concurrentSet(): MutableSet<T> {
    return ConcurrentHashMap.newKeySet()
}

fun <T: Any> concurrentSet(vararg elements: T): MutableSet<T> {
    return concurrentSet<T>().also { it += elements }
}

fun <K: Any, V: Any> concurrentHashMap(): ConcurrentHashMap<K, V> {
    return ConcurrentHashMap()
}

fun <K: Any, V: Any> concurrentHashMap(vararg pairs: Pair<K, V>): ConcurrentHashMap<K, V> {
    return concurrentHashMap<K, V>().also { it += pairs }
}
