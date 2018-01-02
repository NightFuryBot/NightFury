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

package xyz.nightfury.listeners

import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent
import net.dv8tion.jda.core.events.user.UserTypingEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * @author Kaidan Gustave
 */
class InvisibleTracker : EventListener {
    private val map : HashMap<Long, OffsetDateTime> = HashMap()

    override fun onEvent(event: Event?) {
        if(event is UserOnlineStatusUpdateEvent) {
            if(isInvisible(event.user)) {
                synchronized(map) {
                    map.remove(event.user.idLong)
                }
            }
        } else if(event is UserTypingEvent) {
            if(event.isPrivate)
                return
            if(event.member.onlineStatus == OnlineStatus.OFFLINE) {
                synchronized(map) {
                    map.put(event.user.idLong, OffsetDateTime.now())
                }
            }
        }
    }

    fun isInvisible(user: User): Boolean = synchronized(map) {
        if(map.contains(user.idLong)) {
            if(map[user.idLong]?.plusMinutes(5)?.isAfter(OffsetDateTime.now()) == true)
                true // Is invisible
            else {
                map.remove(user.idLong)
                false // Was invisible but past the 5 minute mark
            }
        }
        else false // Was not invisible
    }

    fun getLastTimeTyping(user: User): Long? {
        if(!isInvisible(user))
            return null
        synchronized(map) {
            return map[user.idLong]?.until(OffsetDateTime.now(), ChronoUnit.MINUTES)
        }
    }
}