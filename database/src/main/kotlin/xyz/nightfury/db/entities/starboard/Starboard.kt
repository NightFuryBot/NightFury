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
package xyz.nightfury.db.entities.starboard

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.entities.starboard.IStarboard
import xyz.nightfury.util.createLogger
import java.time.OffsetDateTime

/**
 * @author Kaidan Gustave
 */
class Starboard(val guild: Guild): MutableMap<Long, StarMessage> by HashMap(), IStarboard<StarMessage> {
    override var channel: TextChannel?
        get()      = Settings.getChannel(guild)
        set(value) = if(value != null) Settings.setChannel(value) else Settings.deleteSettingsFor(guild)

    override var threshold: Int
        get()      = Settings.getThresholdFor(guild) ?: IStarboard.DEFAULT_THRESHOLD
        set(value) = Settings.setThresholdFor(guild, value)

    override var maxAge: Int
        get()      = Settings.getMaxAgeFor(guild) ?: IStarboard.DEFAULT_MAX_AGE
        set(value) = Settings.setMaxAgeFor(guild, value)

    override fun addStar(user: User, starred: Message) {
        // Message is from starboard, so we don't do anything
        if(starred.channel == channel)
            return
        // Message is older than allowed
        if(starred.creationTime.plusHours(maxAge.toLong()).isBefore(OffsetDateTime.now()))
            return
        (this[starred.idLong] ?: StarMessage(this, starred).also { this[starred.idLong] = it }).addStar(user)
    }

    override fun deletedMessage(messageId: Long) {
        remove(messageId)?.delete()
    }

    companion object {
        internal val LOG = createLogger(Starboard::class)
    }
}
