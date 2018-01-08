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

/**
 * @author Kaidan Gustave
 */
object StarboardHandler {
    val starboards: MutableMap<Long, Starboard> = HashMap()

    fun createStarboard(starboard: TextChannel) {
        Settings.createSettingsFor(starboard)

        getStarboard(starboard.guild)
    }

    fun hasStarboard(guild: Guild): Boolean = Settings.hasSettingsFor(guild)

    fun removeStarboard(guild: Guild) {
        synchronized(starboards) { starboards.remove(guild.idLong) }

        if(Settings.hasSettingsFor(guild))
            Settings.deleteSettingsFor(guild)
    }

    fun getStarboard(guild: Guild): Starboard? {
        if(!Settings.hasSettingsFor(guild))
            return null
        return synchronized(starboards) {
            starboards[guild.idLong] ?: Starboard(guild).also {
                starboards[guild.idLong] = it
            }
        }
    }
}