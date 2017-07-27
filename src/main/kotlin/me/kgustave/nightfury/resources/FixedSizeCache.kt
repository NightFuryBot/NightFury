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
package me.kgustave.nightfury.resources

class FixedSizeCache<in K : Any, V>(size: Int)
{
    private val map: HashMap<K, V> = HashMap()

    @Suppress("UNCHECKED_CAST")
    private val keys: Array<K?> =
            if(size<1)
                throw IllegalArgumentException("Cache size must be at least 1!")
            else arrayOfNulls<Any>(size) as Array<K?>

    val size : Int
        get() = currIndex

    private var currIndex = 0

    fun add(key: K, value: V)
    {
        if(keys[currIndex] != null)
            map.remove(keys[currIndex])
        map.put(key, value)
        keys[currIndex] = key
        currIndex = (currIndex + 1) % keys.size
    }

    fun contains(key: K) : Boolean = map.containsKey(key)

    fun get(key: K) : V? = map[key]
}