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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.nightfury.entities.starboard

// We'll probably end up doing changing this one day, but for now some notes on
// why we handle starboard WAY differently then say, RoleMe roles, or ModLog channels
// is because internally it's pretty strict.
//
// Basic concept:
//
// If the guild has a starboard, we will generate a Starboard object and cache it.
// This is effective immediately upon the first "star reaction" being added to a
// message on a guild where we have database settings for the starboard.

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import xyz.nightfury.ndb.entities.StarboardSettings
import xyz.nightfury.util.db.hasStarboard
import xyz.nightfury.util.db.starboardSettings

/**
 * @author Kaidan Gustave
 */
object StarboardManager {
    val starboards: MutableMap<Long, Starboard> = HashMap()

    fun createStarboard(starboard: TextChannel): Starboard {
        val guild = starboard.guild
        if(!guild.hasStarboard) {
            guild.starboardSettings = StarboardSettings(guild.idLong, starboard.idLong)
        }

        return requireNotNull(getStarboard(guild)) {
            "Created starboard settings for Guild (ID: ${guild.idLong}) but could not " +
            "immediately create Starboard instance!"
        }
    }

    fun removeStarboard(guild: Guild) {
        starboards.remove(guild.idLong)

        if(guild.hasStarboard) {
            guild.starboardSettings = null
        }
    }

    fun getStarboard(guild: Guild): Starboard? {
        val settings = guild.starboardSettings
        if(settings === null) {
            if(guild.hasStarboard) {
                guild.starboardSettings = null
            }
            return null
        }
        return starboards.computeIfAbsent(guild.idLong) { Starboard(guild, settings) }
    }
}
