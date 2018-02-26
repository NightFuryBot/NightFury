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
package xyz.nightfury.ndb.entities

import xyz.nightfury.ndb.starboard.StarboardSettingsHandler

/**
 * @author Kaidan Gustave
 */
data class StarboardSettings(
    val guildId: Long,
    val channelId: Long,
    var threshold: Int = DEFAULT_THRESHOLD,
    var maxAge: Int = DEFAULT_MAX_AGE
) {
    companion object {
        const val DEFAULT_THRESHOLD = 5
        const val DEFAULT_MAX_AGE = 72 // hours
    }

    fun update() {
        StarboardSettingsHandler.updateSettings(this)
    }
}