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

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import xyz.nightfury.extensions.createLogger
import java.io.IOException
import java.net.InetAddress

/**
 * @author Kaidan Gustave
 */
class YouTubeAPI(private val apiKey: String?): AbstractAPICache<List<String>>() {
    private companion object {
        private val ytLog = createLogger(YouTubeAPI::class)
        private val netTransport = NetHttpTransport()
        private val jsonFactory = JacksonFactory.getDefaultInstance()
        private val hostAddress = InetAddress.getLocalHost().hostAddress

        var maxSearchResults = 20L
    }

    private val isEnabled = apiKey != null

    private val youtube = YouTube.Builder(netTransport, jsonFactory, {})
        .setApplicationName("NightFury").build()

    private val search: YouTube.Search.List? = if(apiKey == null) null else try {
        youtube.search().list("id,snippet")
    } catch (e : IOException) {
        ytLog.error("Failed to initialize search: $e")
        null
    }

    override val hoursToDecay = 1L

    fun search(query: String): List<String>? {
        if(search == null) {
            if(isEnabled) {
                ytLog.warn("YouTube searcher initialization failed, search could not be performed!")
            }

            return null
        }

        getFromCache(query)?.let { return it }

        with(search) {
            q = query
            maxResults = maxSearchResults
            key = apiKey
            userIp = hostAddress
            type = "video"
            fields = "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)"
        }

        val response = try {
            search.execute()
        } catch(e: GoogleJsonResponseException) {
            ytLog.error("Search failure: ${e.message} - ${e.details.message}")
            return null
        }

        val results = response.items.map { it.id.videoId }
        addToCache(query,results)
        return results
    }
}
