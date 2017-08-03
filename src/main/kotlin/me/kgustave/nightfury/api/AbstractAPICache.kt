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

/**
 * @author Kaidan Gustave
 */
abstract class AbstractAPICache<T>
{
    protected val cache : HashMap<String, Pair<T, OffsetDateTime>> = HashMap()

    fun addToCache(query : String, item : T) = synchronized(cache) { cache.put(query.toLowerCase(), Pair(item, OffsetDateTime.now())) }

    fun getFromCache(query: String) : T? = synchronized(cache) { cache[query.toLowerCase()]?.first }

    abstract fun clearCache()
}