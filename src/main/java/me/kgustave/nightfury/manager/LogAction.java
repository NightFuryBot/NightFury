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
package me.kgustave.nightfury.manager;

/**
 *
 * @author Kaidan Gustave
 */
public enum LogAction {
    BAN("banned","\uD83D\uDD28"), UNBAN("unbanned",""),
    KICK("kicked","\uD83D\uDC62"), MUTE("muted","\uD83D\uDD07"),
    UNMUTE("unmuted","\uD83D\uDD08"), CLEAN("deleted %d messages in", "");
    
    public final String act;
    public final String emote;
    
    LogAction(String act, String emote) {
        this.act = act;
        this.emote = emote;
    }
}
