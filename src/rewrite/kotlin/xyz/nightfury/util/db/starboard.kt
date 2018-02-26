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
@file:Suppress("Unused")
package xyz.nightfury.util.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import xyz.nightfury.entities.starboard.Star
import xyz.nightfury.entities.starboard.Starboard
import xyz.nightfury.entities.starboard.StarboardManager as Manager
import xyz.nightfury.ndb.entities.StarboardSettings
import xyz.nightfury.ndb.starboard.StarboardEntriesHandler as Entries
import xyz.nightfury.ndb.starboard.StarboardSettingsHandler as Settings

inline val <reified G: Guild> G.starboard: Starboard? inline get() {
    return Manager.getStarboard(this)
}
inline val <reified G: Guild> G.hasStarboard: Boolean inline get() {
    return Settings.hasSettings(idLong) && Settings.hasChannel(idLong)
}
inline var <reified G: Guild> G.starboardSettings: StarboardSettings?
    inline get() = Settings.getSettings(idLong)
    inline set(value) {
        if(value !== null) {
            if(Settings.hasSettings(idLong))
                value.update()
            else Settings.createSettings(value.guildId, value.channelId)
        } else Settings.removeSettings(idLong)
    }
inline var <reified G: Guild> G.starboardChannel: TextChannel?
    inline get() = getTextChannelById(Settings.getChannel(idLong))
    inline set(value) {
        requireNotNull(value) { "Cannot set starboardChannel to null, use starboardSettings instead!" }
        Settings.setChannel(idLong, value!!.idLong)
    }

inline val <reified M: Message> M.stars: List<Star> inline get() {
    requireNotNull(guild) { "Cannot get stars for a Message with null guild!" }
    return Entries.getStars(idLong, guild.idLong).map { Star(this, it.second) }
}

inline val <reified M: Message> M.starCount: Int inline get() {
    requireNotNull(guild) { "Cannot get stars for a Message with null guild!" }
    return Entries.getStarCount(idLong, guild.idLong)
}

inline fun <reified G: Guild> G.createStarboard(channel: TextChannel): Starboard {
    return Manager.createStarboard(channel)
}

inline fun <reified G: Guild> G.removeStarboard() {
    Manager.removeStarboard(this)
}
