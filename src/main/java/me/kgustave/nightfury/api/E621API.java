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

import org.json.JSONArray;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author Kaidan Gustave
 */
public class E621API {
    
    private static final SimpleLog LOG = SimpleLog.getLog("e621");
    
    public E621API() {}
    
    public JSONArray search(int limit, String... tags) {
        if(tags.length<1)
            throw new IllegalArgumentException("Must have one or more tags!");
        if(limit<1) 
            throw new IllegalArgumentException("Limit must be a non-zero positive number!");
        try {
            String str = tags[0];
            if(tags.length>1) {
                for(int i=1; i<tags.length; i++) {
                    str+="%20"+tags[i];
                }
            }
            return Unirest.get("https://e621.net/post/index.json?limit="+limit+(!str.isEmpty()? "&tags="+str : str)).asJson().getBody().getArray();
        } catch (UnirestException e) {
            LOG.fatal("Failed to search: "+e);
            return null;
        }
    }
    
    public JSONArray search(String... tags) {
        if(tags.length<1)
            throw new IllegalArgumentException();
        try {
            String str = tags[0];
            if(tags.length>1) {
                for(int i=1; i<tags.length; i++) {
                    str+="%20"+tags[i];
                }
            }
            return Unirest.get("https://e621.net/post/index.json"+(!str.isEmpty()? "?tags="+str : str)).asJson().getBody().getArray();
        } catch (UnirestException e) {
            LOG.warn("Failed to search: "+e);
            return null;
        }
    }
}
