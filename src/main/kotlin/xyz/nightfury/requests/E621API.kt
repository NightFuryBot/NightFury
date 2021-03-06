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
@file:Suppress("LiftReturnOrAssignment")
package xyz.nightfury.requests

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONTokener
import xyz.nightfury.extensions.createLogger

/**
 * @author Kaidan Gustave
 */
class E621API : AbstractAPICache<JSONArray>() {
    private companion object {
        private const val BASE_URL = "https://e621.net/post/index.json?"
        private val LOG = createLogger(E621API::class)
    }

    override val hoursToDecay = 1L

    private val client = OkHttpClient()

    fun search(limit: Int, vararg tags: String): JSONArray? {
        require(tags.size <= 6) { "Only 6 tags may be specified!" }
        require(limit <= 320) { "Only a maximum of 320 to retrieve may be specified!" }

        val key = buildString {
            tags.forEach{
                append(if (it.startsWith("+") || it.startsWith("-")) it else "+$it")
            }
        }

        getFromCache(key)?.let { return it }

        try {
            return client.newCall(Request.Builder().get()
                .header("User-Agent", "NightFury")
                .url(BASE_URL + "tags=$key&limit=$limit").build())
                .execute().body()?.charStream()
                .use { it?.let { JSONArray(JSONTokener(it)).also { addToCache(key, it) } } }
        } catch(e: Exception) {
            LOG.warn("Failed to retrieve from e621.net!",e)
            return null
        }
    }
}
