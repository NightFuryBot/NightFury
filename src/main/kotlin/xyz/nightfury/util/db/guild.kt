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

import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import xyz.nightfury.ndb.guilds.BlacklistHandler
import xyz.nightfury.ndb.guilds.JoinWhitelistHandler
import xyz.nightfury.ndb.guilds.MusicWhitelistHandler

inline var <reified G: Guild> G.isMusic: Boolean
    inline get() = MusicWhitelistHandler.isGuild(idLong)
    inline set(value) {
        if(value) MusicWhitelistHandler.addGuild(idLong)
        else MusicWhitelistHandler.removeGuild(idLong)
    }

inline var <reified G: Guild> G.isBlacklisted: Boolean
    inline get() = BlacklistHandler.isGuild(idLong)
    inline set(value) {
        if(value) BlacklistHandler.addGuild(idLong)
        else BlacklistHandler.removeGuild(idLong)
    }

inline var <reified G: Guild> G.isJoinWhitelisted: Boolean
    inline get() = JoinWhitelistHandler.isGuild(idLong)
    inline set(value) {
        if(value) JoinWhitelistHandler.addGuild(idLong)
        else JoinWhitelistHandler.removeGuild(idLong)
    }

inline val <reified J: JDA> J.musicGuilds: List<Guild> inline get() {
    return MusicWhitelistHandler.getGuilds().mapNotNull { getGuildById(it) }
}

inline val <reified J: JDA> J.blacklistedGuilds: List<Guild> inline get() {
    return BlacklistHandler.getGuilds().mapNotNull { getGuildById(it) }
}

inline val <reified J: JDA> J.joinWhitelistedGuilds: List<Guild> inline get() {
    return JoinWhitelistHandler.getGuilds().mapNotNull { getGuildById(it) }
}

inline val <reified S: ShardManager> S.musicGuilds: List<Guild> inline get() {
    return MusicWhitelistHandler.getGuilds().mapNotNull { getGuildById(it) }
}

inline val <reified S: ShardManager> S.blacklistedGuilds: List<Guild> inline get() {
    return BlacklistHandler.getGuilds().mapNotNull { getGuildById(it) }
}

inline val <reified S: ShardManager> S.joinWhitelistedGuilds: List<Guild> inline get() {
    return JoinWhitelistHandler.getGuilds().mapNotNull { getGuildById(it) }
}
