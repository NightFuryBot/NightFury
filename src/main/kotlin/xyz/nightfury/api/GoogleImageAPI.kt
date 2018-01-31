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
package xyz.nightfury.api

import org.jsoup.Jsoup
import xyz.nightfury.extensions.createLogger
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * @author Kaidan Gustave
 */
class GoogleImageAPI : AbstractAPICache<List<String>>()
{
    override val hoursToDecay = 5L

    companion object {
        private const val URL = "https://www.google.com/search?site=imghp&tbm=isch&source=hp&biw=1680&bih=940&q=%s&safe=active"
        private const val ENCODING = "UTF-8"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) " +
                                       "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                       "Chrome/53.0.2785.116 Safari/537.36"

        private val LOG = createLogger(GoogleImageAPI::class)
    }

    fun search(query: String) : List<String>?
    {
        getFromCache(query)?.let { return it }
        val request = try {
            URL.format(URLEncoder.encode(query, ENCODING))
        } catch (e: UnsupportedOperationException) {
            LOG.error("Error processing request: $e")
            return@search null
        }

        val result = ArrayList<String>()
        try {
            Jsoup.connect(request).userAgent(USER_AGENT)
                    .referrer("https://google.com/")
                    .timeout(7500) // Timeout
                    .get().select("div.rg_meta").stream()
                    .filter  { it.childNodeSize() > 0 }
                    .forEach {
                        try {
                            val node = it.childNode(0).toString()
                            val frontIndex = node.indexOf("\"ou\":") + 6 // Find the front index of the json key

                            result += URLDecoder.decode(node.substring(frontIndex,
                                node.indexOf("\",", frontIndex)), ENCODING)
                        } catch (e: UnsupportedOperationException) {
                            LOG.error("An exception was thrown while decoding an image URL: $e")
                        } catch (e: IndexOutOfBoundsException) {
                            LOG.error("An exception was thrown due to improper indexing: $e")
                        }
                    }
        } catch (e: IOException) {
            LOG.error("Encountered an IOException: $e")
            return@search null
        }

        addToCache(query, result)
        return result
    }
}
