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

/**
 * @author Kaidan Gustave
 */
class CaseInsensitiveHashMap<V>: MutableMap<String, V> {
    private val map = HashMap<String, V>()
    private val savedEntries = HashSet<MutableMap.MutableEntry<String, V>>()

    override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
        get() = savedEntries
    override val size: Int
        get() = map.size
    override val keys: MutableSet<String>
        get() = savedEntries.mapTo(HashSet()) { it.key }
    override val values: MutableCollection<V>
        get() = savedEntries.mapTo(HashSet()) { it.value }

    override fun containsKey(key: String): Boolean = map.containsKey(key.toLowerCase())
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun get(key: String): V? = map[key.toLowerCase()]
    override fun isEmpty(): Boolean = map.isEmpty() && entries.isEmpty()

    override fun clear() {
        savedEntries.clear()
        map.clear()
    }

    override fun put(key: String, value: V): V? {
        savedEntries.add(Entry(key, value))
        return map.put(key.toLowerCase(), value)
    }

    override fun putAll(from: Map<out String, V>) {
        from.forEach { this[it.key] = it.value }
    }

    override fun remove(key: String): V? = map.remove(key.toLowerCase())

    private inner class Entry(override val key: String, value: V): MutableMap.MutableEntry<String, V> {
        override var value: V = value
            get() = map[key.toLowerCase()] ?: field
        override fun setValue(newValue: V): V {
            val oldValue = value
            map[key.toLowerCase()] = newValue
            value = newValue
            return oldValue
        }
    }
}
