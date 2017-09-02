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
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder

/**
 * @author Kaidan Gustave
 */
class GoogleImageAPI : AbstractAPICache<List<String>>()
{
    override val hoursToDecay: Long
        get() = 5

    companion object {
        private val URL = "https://www.google.com/search?site=imghp&tbm=isch&source=hp&biw=1680&bih=940&q=%s&safe=active"
        private val LOG : SimpleLog = SimpleLog.getLog("Google Image")
        private val ENCODING = "UTF-8"
        private val USER_AGENT =
                "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36"
    }

    fun search(query: String) : List<String>?
    {
        val cached = getFromCache(query)
        if(cached!=null)
            return cached
        val request = try {
            val enc = URLEncoder.encode(query, ENCODING)
            String.format(URL, enc)
        } catch (e: UnsupportedOperationException) {
            LOG.fatal(e)
            return@search null
        }
        val result = ArrayList<String>()
        try {
            Jsoup.connect(request).userAgent(USER_AGENT)
                    .referrer("https://google.com/")
                    .timeout(7500) // Timeout
                    .get().select("div.rg_meta").stream()
                    .filter  { it.childNodeSize() > 0 }
                    .forEach { result.add(JSONObject(it.childNode(0).toString()).getString("ou")) }
        } catch (e: IOException) {
            LOG.fatal(e)
            return@search null
        }
        addToCache(query, result)
        return result
    }
}