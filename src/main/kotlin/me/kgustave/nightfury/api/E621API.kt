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

import me.monitor.je621.E621Array
import me.monitor.je621.JE621Builder
import java.time.OffsetDateTime
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class E621API
{
    private val cache : HashMap<String, Pair<E621Array, OffsetDateTime>> = HashMap()

    private val jE621 = JE621Builder("NightFury").build()

    fun search(limit: Int, vararg tags: String) : E621Array?
    {
        if(tags.size>6) throw IllegalArgumentException("Only 6 tags may be specified!")
        if(limit>320) throw IllegalArgumentException("Only a maximum of 320 to retrieve may be specified!")
        val key = buildString { tags.forEach { append("$it ") } }.toLowerCase()
        if(synchronized(cache) { cache.containsKey(key) })
        {
            val cached = cache[key]?.first
            if(cached != null && cached.size()==limit)
                return cached
        }
        val arr = jE621.startNewSearch().setMaxRetrieved(limit).addTags(*tags).search().get()
        synchronized(cache) { cache.put(key, Pair(arr, OffsetDateTime.now())) }
        return arr
    }

    fun clearCache()
    {
        synchronized(cache) {
            val now = OffsetDateTime.now()
            cache.keys.stream()
                    .filter { key -> now.isAfter(cache[key]!!.second.plusHours(1)) }
                    .toList().forEach { toRemove -> cache.remove(toRemove) }
        }
    }
}