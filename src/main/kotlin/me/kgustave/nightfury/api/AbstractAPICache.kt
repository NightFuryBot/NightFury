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
package me.kgustave.nightfury.api

import java.time.OffsetDateTime
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
abstract class AbstractAPICache<T>
{
    private val cache : HashMap<String, Pair<T, OffsetDateTime>> = HashMap()

    abstract val hoursToDecay : Long

    fun addToCache(query : String, item : T) = synchronized(cache)
    {
        cache.put(query.toLowerCase(), item.to(OffsetDateTime.now()))
    }

    fun getFromCache(query: String) : T? = synchronized(cache)
    {
        cache[query.toLowerCase()]?.takeIf { it.second.plusHours(hoursToDecay).isBefore(OffsetDateTime.now()) }?.first
    }

    fun clearCache()
    {
        synchronized(cache)
        {
            val now = OffsetDateTime.now()
            cache.keys.stream()
                    .filter { now.isAfter(cache[it]!!.second.plusHours(hoursToDecay)) }
                    .toList().forEach { cache.remove(it) }
        }
    }
}