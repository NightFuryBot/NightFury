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
package xyz.nightfury.requests

import org.jsoup.Jsoup
import xyz.nightfury.extensions.createLogger
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class GoogleAPI : AbstractAPICache<List<String>>()
{
    private companion object
    {
        private const val URL_FORMAT : String = "https://www.google.com/search?q=%s&num=10"
        private const val ENCODING = "UTF-8"
        private const val USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
        private val LOG = createLogger(GoogleAPI::class)
    }

    override val hoursToDecay: Long = 5

    fun search(query: String): List<String>? {
        val cached = getFromCache(query)
        if(cached != null)
            return cached
        val request = try {
            URL_FORMAT.format(URLEncoder.encode(query, ENCODING))
        } catch (e: UnsupportedOperationException) {
            LOG.error("Error processing request: $e")
            return@search null
        }
        val result: List<String> = try {
            Jsoup.connect(request).userAgent(USER_AGENT).timeout(7500).get()
                    .select("a[href]").stream()
                    .map { it.attr("href") }
                    .filter { it.startsWith("/url?q=") }
                    .map {
                        try {
                            URLDecoder.decode(it.substring(7, it.indexOf("&sa=")), ENCODING)
                        } catch (e: UnsupportedOperationException) { "" }
                    }
                    .filter { it.isNotEmpty() && it != "/settings/ads/preferences?hl=en" }
                    .toList()
        } catch(e: IOException) {
            LOG.error("Encountered an IOException: $e")
            return@search null
        }
        addToCache(query, result)
        return result
    }
}
