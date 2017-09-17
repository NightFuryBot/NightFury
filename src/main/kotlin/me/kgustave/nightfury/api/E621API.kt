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

import me.kgustave.nightfury.entities.SimpleLog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONTokener

/**
 * @author Kaidan Gustave
 */
class E621API : AbstractAPICache<JSONArray>()
{
    private companion object
    {
        private val BASE_URL = "https://e621.net/post/index.json?"
        private val LOG = SimpleLog.getLog("E621")
    }

    override val hoursToDecay : Long = 1

    private val client = OkHttpClient()

    fun search(limit: Int, vararg tags: String) : JSONArray?
    {
        if(tags.size>6) throw IllegalArgumentException("Only 6 tags may be specified!")
        if(limit>320) throw IllegalArgumentException("Only a maximum of 320 to retrieve may be specified!")
        val key = buildString {tags.forEach{append(if (it.startsWith("+") || it.startsWith("-")) it else "+$it")}}
        val cached = getFromCache(key)
        if(cached!=null)
            return cached
        try {
            client.newCall(Request.Builder().get()
                    .header("User-Agent", "NightFury")
                    .url(BASE_URL + "tags=$key&limit=$limit").build())
                    .execute().body()?.charStream()
                    .use {
                        return if(it == null) {
                            LOG.warn("Reader retrieved from e621.net was null!")
                            null
                        } else {
                            val arr = JSONArray(JSONTokener(it))
                            addToCache(key, arr)
                            arr
                        }
                    }
        } catch (e : Exception) {
            LOG.warn("Failed to retrieve from e621.net!")
            LOG.log(e)
            return null
        }
    }
}