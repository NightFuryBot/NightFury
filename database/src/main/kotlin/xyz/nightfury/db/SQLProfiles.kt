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
package xyz.nightfury.db

import net.dv8tion.jda.core.entities.User
import xyz.nightfury.db.entities.Profile
import xyz.nightfury.extensions.toDate
import xyz.nightfury.resources.FixedSizeCache

/**
 * @author Kaidan Gustave
 */
object SQLProfiles : Table() {

    /*
     * schema PROFILES
     * col USER_ID BIGINT
     * col TIME_ZONE_ID VARCHAR(75) DEFAULT NULL
     * col GITHUB VARCHAR(100) DEFAULT NULL
     * col WEBSITE VARCHAR(100) DEFAULT NULL
     * col TWITCH VARCHAR(100) DEFAULT NULL
     * col TITLE VARCHAR(50) DEFAULT NULL
     * col ABOUT VARCHAR(1800) DEFAULT NULL
     * col BIRTHDAY DATE DEFAULT NULL
     */

    // we cache profiles because they auto-update when values are changed.
    private val profileCache: MutableMap<Long, Profile> = FixedSizeCache(size = 100)

    fun createProfile(user: User) {
        using(connection.prepareStatement("INSERT INTO PROFILES (USER_ID) VALUES(?)")) {
            this[1] = user.idLong
            execute()
        }

        // We cache the profile because it's brand new and general users will
        // start customizing directly after.
        synchronized(profileCache) { profileCache.put(user.idLong, Profile(user)) }
    }

    fun getProfile(user: User): Profile? {
        synchronized(profileCache) {
            if(profileCache.containsKey(user.idLong))
                return profileCache[user.idLong]
        }

        return using(connection.prepareStatement("SELECT * FROM PROFILES WHERE USER_ID = ?")) {
            this[1] = user.idLong

            using(executeQuery()) {
                if(next()) Profile(user, this) else null
            }
        }
    }

    fun updateProfile(profile: Profile) {
        using(connection.prepareStatement("UPDATE PROFILES SET " + "TIME_ZONE_ID = ?, " + "GITHUB = ?, " + "WEBSITE = ?, " + "TWITCH = ?, " + "TITLE = ?, " + "ABOUT = ?, " + "BIRTHDAY = ? " + "WHERE USER_ID = ?")) {
            this[1] = profile.timeZoneId
            this[2] = profile.github
            this[3] = profile.website
            this[4] = profile.twitch
            this[5] = profile.title
            this[6] = profile.about
            this[7] = profile.birthday?.toDate()
            this[8] = profile.user.idLong

            execute()
        }
    }
}
