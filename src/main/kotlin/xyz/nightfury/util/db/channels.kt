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
package xyz.nightfury.util.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import xyz.nightfury.ndb.WelcomesHandler
import xyz.nightfury.ndb.channels.AnnouncementChannelHandler
import xyz.nightfury.ndb.channels.IgnoredChannelsHandler
import xyz.nightfury.ndb.channels.ModLogHandler

inline val <reified G: Guild> G.ignoredChannels: List<TextChannel> inline get() {
    return IgnoredChannelsHandler.getChannels(idLong).mapNotNull { getTextChannelById(it) }
}
inline var <reified C: TextChannel> C.isIgnored: Boolean
    inline get() = IgnoredChannelsHandler.isChannel(guild.idLong, idLong)
    inline set(value) {
        if(value) IgnoredChannelsHandler.addChannel(guild.idLong, idLong)
        else IgnoredChannelsHandler.removeChannel(guild.idLong, idLong)
    }

inline var <reified G: Guild> G.modLog: TextChannel?
    inline get() = getTextChannelById(ModLogHandler.getChannel(idLong))
    inline set(value) {
        if(value !== null) ModLogHandler.setChannel(idLong, value.idLong)
        else ModLogHandler.removeChannel(idLong)
    }
inline val <reified G: Guild> G.hasModLog: Boolean inline get() {
    return ModLogHandler.hasChannel(idLong)
}

inline var <reified G: Guild> G.announcementChannel: TextChannel?
    inline get() = getTextChannelById(AnnouncementChannelHandler.getChannel(idLong))
    inline set(value) {
        if(value !== null) AnnouncementChannelHandler.setChannel(idLong, value.idLong)
        else AnnouncementChannelHandler.removeChannel(idLong)
    }
inline val <reified G: Guild> G.hasAnnouncementsChannel: Boolean inline get() {
    return AnnouncementChannelHandler.hasChannel(idLong)
}

inline val <reified G: Guild> G.welcomeChannel: TextChannel? inline get() {
    return getTextChannelById(WelcomesHandler.getChannel(idLong))
}
inline val <reified G: Guild> G.welcomeMessage: String? inline get() {
    return WelcomesHandler.getWelcome(idLong)
}
inline val <reified G: Guild> G.hasWelcome: Boolean inline get() {
    return WelcomesHandler.hasWelcome(idLong)
}
inline fun <reified G: Guild> G.setWelcome(channel: TextChannel, message: String) {
    require(this == channel.guild) {
        "Tried to set welcome for Guild (ID: $idLong) to a TextChannel from a different Guild!"
    }
    WelcomesHandler.setWelcome(idLong, channel.idLong, message)
}
inline fun <reified G: Guild> G.removeWelcome() {
    WelcomesHandler.removeWelcome(idLong)
}
