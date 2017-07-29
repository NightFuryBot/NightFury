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

import net.dv8tion.jda.core.utils.SimpleLog
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.OffsetDateTime
import kotlin.streams.toList


/**
 * @author Kaidan Gustave
 */
class GoogleAPI
{
    private companion object
    {
        private val URL_FORMAT : String = "https://www.google.com/search?q=%s&num=10"
        private val LOG : SimpleLog = SimpleLog.getLog("Google")
        private val ENCODING : String = "UTF-8"
        private val USER_AGENT : String = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
    }

    private val cache : HashMap<String, Pair<List<String>, OffsetDateTime>> = HashMap()

    fun search(query: String) : List<String>?
    {
        synchronized(cache)
        {
            val cached = cache[query.toLowerCase()]?.first
            if(cached!=null)
                return cached
        }

        val request: String = try {
            String.format(URL_FORMAT, URLEncoder.encode(query, ENCODING))
        } catch (e: UnsupportedOperationException) {
            LOG.fatal(e)
            return@search null
        }
        val result : List<String> = try {
            Jsoup.connect(request).userAgent(USER_AGENT).timeout(7500)
                    .get().select("a[href]").stream()
                    .map { link -> link.attr("href") }
                    .filter { temp -> temp.startsWith("/url?q=") }
                    .map { temp ->
                        try {
                            URLDecoder.decode(temp.substring(7, temp.indexOf("&sa=")), ENCODING)
                        } catch (e: UnsupportedOperationException) { "" }
                    }
                    .filter { result -> result != "/settings/ads/preferences?hl=en" }
                    .toList()
        } catch (e: IOException) {
            LOG.fatal(e)
            return@search null
        }
        synchronized(cache)
        {
            cache.put(query, Pair(result, OffsetDateTime.now()))
        }
        return result
    }

    fun clearCache()
    {
        synchronized(cache)
        {
            val now = OffsetDateTime.now()
            cache.keys.stream()
                    .filter { key -> now.isAfter(cache[key]!!.second.plusHours(5)) }
                    .toList().forEach { toRemove -> cache.remove(toRemove) }
        }
    }
}