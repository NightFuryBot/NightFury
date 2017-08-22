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

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import net.dv8tion.jda.core.utils.SimpleLog
import java.io.IOException

/**
 * @author Kaidan Gustave
 */
class YouTubeAPI(apiKey : String) : AbstractAPICache<List<String>>()
{
    private companion object
    {
        private val ytLog = SimpleLog.getLog("YouTube")
        private var maxSearchResults = 20L
    }

    private val youtube = YouTube.Builder(NetHttpTransport(),JacksonFactory(),{}).setApplicationName("NightFury").build()
    private val search : YouTube.Search.List?

    override val hoursToDecay: Long = 1

    init {
        search = try {
            youtube.search().list("id,snippet")
        } catch (e : IOException) {
            ytLog.fatal("Failed to initialize search: $e")
            null
        }

        if(search != null) with(search)
        {
            key = apiKey
            type = "video"
            fields = "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)"
        }
    }

    fun search(query : String) : List<String>?
    {
        if(search == null) {
            ytLog.warn("YouTube searcher initialization failed, search could not be performed!")
            return null
        }
        val cached = getFromCache(query)
        if(cached!=null)
            return cached
        val results = ArrayList<String>()
        search.q = query
        search.maxResults = maxSearchResults

        val response = try {
            search.execute()
        } catch (e : IOException) {
            ytLog.fatal("Search failure: $e")
            return null
        }
        response.items.stream().forEach { results.add(it.id.videoId) }
        addToCache(query,results)
        return results
    }
}