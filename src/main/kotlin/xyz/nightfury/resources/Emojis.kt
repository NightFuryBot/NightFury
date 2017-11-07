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
package xyz.nightfury.resources

import xyz.nightfury.extensions.niceName
import java.time.temporal.TemporalAccessor
import java.util.*

/**
 * @author Kaidan Gustave
 */
object Emojis {
    const val RED_TICK   = "<:xmark:314349398824058880>"
    const val GREEN_TICK = "<:check:314349398811475968>"

    // Holder for flag emojis.
    // The first is the actual emoji.
    // The second is for the country name that is the first part
    // of a ZoneInfo#id.
    // So for "Chicago, Illinois" the ZoneInfo would be:
    // ZoneInfo.getTimeZone("America/Chicago")
    // Of which the country is "America" and the Flag would
    // be Flag.US
    enum class Flag(val emoji: String, country: String? = null) {
        USA("\uD83C\uDDFA\uD83C\uDDF8", "America"),
        CANADA("\uD83C\uDDE8\uD83C\uDDE6"),
        AFRICA("\uD83C\uDDE6\uD83C\uDDEB"),
        GERMANY(TODO("Add Germany")),
        ENGLAND(TODO("Add England")),
        RUSSIA(TODO("Add Russia"))
        // TODO Add more countries
        ;

        val country: String = country ?: niceName

        override fun toString(): String = emoji

        companion object {
            @[JvmStatic Suppress("UNUSED_PARAMETER")]
            fun of(accessor: TemporalAccessor?): Flag? =
                TODO("Support for TemporalAccessor is not supported yet")

            @JvmStatic
            fun of(zone: TimeZone?, ignoreCase: Boolean = true): Flag? {
                val countryId = (zone ?: return null).id.run {
                    substring(0, this.indexOf('/').takeIf { it != -1 } ?: return null)
                }

                return values().find { countryId.equals(it.country, ignoreCase) }
            }
        }
    }
}