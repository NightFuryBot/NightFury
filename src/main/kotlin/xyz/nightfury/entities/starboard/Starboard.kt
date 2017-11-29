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
package xyz.nightfury.entities.starboard

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

/**
 * @author Kaidan Gustave
 */
class Starboard(val guild: Guild,
                private val map: MutableMap<Long, StarMessage> = HashMap()): MutableMap<Long, StarMessage> by map {
    var channel: TextChannel?
        get()      = Settings.getChannel(guild)
        set(value) = if(value != null) Settings.setChannel(value) else Settings.deleteSettingsFor(guild)

    var threshold: Int
        get()      = Settings.getThresholdFor(guild) ?: DEFAULT_THRESHOLD
        set(value) = Settings.setThresholdFor(guild, value)

    var maxAge: Int
        get()      = Settings.getMaxAgeFor(guild) ?: DEFAULT_MAX_AGE
        set(value) = Settings.setMaxAgeFor(guild, value)

    fun addStar(user: User, starred: Message) {
        // Message is from starboard, so we don't do anything
        if(starred.channel == channel)
            return
        // Message is older than allowed
        if(starred.creationTime.plusHours(maxAge.toLong()).isBefore(OffsetDateTime.now()))
            return
        synchronized(map) {
            map[starred.idLong] ?: StarMessage(this, starred).also { map[starred.idLong] = it }
        }.addStar(user)
    }

    fun deletedMessage(messageId: Long) {
        map.remove(messageId)?.delete()
    }

    companion object {
        const val DEFAULT_THRESHOLD = 5
        const val DEFAULT_MAX_AGE = 72
        internal val LOG: Logger = LoggerFactory.getLogger(Starboard::class.java)
    }
}