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
package me.kgustave.nightfury.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;

import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author Kaidan Gustave
 */
public class GoogleAPI {
    
    private static final String URL_FORMAT = "https://www.google.com/search?q=%s&num=10";
    private static final SimpleLog LOG = SimpleLog.getLog("Google");
    
    private final HashMap<String,Pair<List<String>, OffsetDateTime>> cache;
    private final String encoding;
    
    public GoogleAPI() {
        cache = new HashMap<>();
        encoding = "UTF-8";
    }
    
    public List<String> search(String query) {
        synchronized(cache) {
            List<String> result = cache.get(query.toLowerCase())==null ? null : cache.get(query.toLowerCase()).getKey();
            if(result!=null)
                return result;
        }
        String request;
        try {
            request = String.format(URL_FORMAT, URLEncoder.encode(query, encoding));
        } catch (UnsupportedEncodingException e) {
            LOG.fatal("Failed to encode with the provided encoding "+encoding+" "+e);
            return null;
        }
        List<String> result = new ArrayList<>();
        try {
            Jsoup.connect(request).userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)").timeout(7500).get().select("a[href]").stream()
            .map((link) -> link.attr("href"))
            .filter((temp) -> (temp.startsWith("/url?q=")))
            .forEach((temp) -> {
                try {
                    String res = URLDecoder.decode(temp.substring(7,temp.indexOf("&sa=")), encoding);
                    if(!res.equals("/settings/ads/preferences?hl=en"))
                        result.add(res);
                } catch (UnsupportedEncodingException e) {}
            });
            synchronized(cache) {
                cache.put(query.toLowerCase(), Pair.of(result,OffsetDateTime.now()));
            }
        } catch (IOException e) {
            LOG.fatal("Search failed: "+e);
            return null;
        }
        return result;
    }
    
    public void clearCache()
    {
        synchronized(cache)
        {
            List<String> deleteList = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();
            cache.keySet().stream()
            .filter((key) -> now.isAfter(cache.get(key).getValue().plusHours(6)))
            .forEach((truequery) -> deleteList.add(truequery));
            deleteList.forEach((str) -> {
                cache.remove(str);
            });
        }
    }
    
}
