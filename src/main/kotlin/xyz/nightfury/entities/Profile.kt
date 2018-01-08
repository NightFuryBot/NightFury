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
package xyz.nightfury.entities

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.db.SQLProfiles
import xyz.nightfury.db.get
import xyz.nightfury.extensions.*
import xyz.nightfury.resources.Emojis
import xyz.nightfury.extensions.timeZone
import java.awt.Color
import java.sql.ResultSet
import java.sql.Date
import java.time.Instant
import java.time.OffsetDateTime
import java.util.TimeZone

/**
 * @author Kaidan Gustave
 */
class Profile(val user: User,
              timeZoneId: String? = null,
              github: String? = null,
              website: String? = null,
              twitch: String? = null,
              title: String? = null,
              about: String? = null,
              birthday: Date? = null) {
    var timeZone: TimeZone? = timeZone(timeZoneId)
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }

    val flag: Emojis.Flag?
        get() = Emojis.Flag.of(timeZone)

    var timeZoneId: String?
        get() = timeZone?.id
        set(value) {
            timeZone = timeZone(value)
        }

    var github: String? = github
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }

    var website: String? = website
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }

    var twitch: String? = twitch
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }

    var title: String? = title
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }

    var about: String? = about
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }

    var birthday: OffsetDateTime? = birthday?.toOffsetDateTime()
        set(value) {
            field = value
            SQLProfiles.updateProfile(this)
        }


    // can construct from a result set.
    // should have the cursor already placed over the row for the profile.
    constructor(user: User, set: ResultSet): this(user,
                                                  set["TIME_ZONE_ID"], set["GITHUB"],
                                                  set["WEBSITE"], set["TWITCH"],
                                                  set["TITLE"], set["ABOUT"],
                                                  set["BIRTHDAY"])

    fun toEmbed(color: Color? = null): Message = message {
        append { "Profile for ${user.formattedName(boldName = true)}" }
        embed {
            this@Profile.title?.let { this@embed.title = it }
            this@Profile.about?.let { this@embed.append(it) }

            this@embed.color = color
            this@embed.thumbnail = user.effectiveAvatarUrl

            this@Profile.timeZone?.let { timeZone ->
                this@embed.field {
                    this@field.name = "TimeZone"

                    flag?.let { this@field.append("${it.emoji} ") }

                    this@field.append(timeZone.id)

                    this@field.inline = true
                }

                this@embed.footer {
                    this@footer.value = "Time"
                    this@footer.icon = clockIconUrl
                }

                this@embed.time { OffsetDateTime.ofInstant(Instant.now(), timeZone.toZoneId()) }
            }

            this@Profile.birthday?.let { birthday ->
                this@embed.field {
                    this@field.name = "Birthday"

                    this@field.append("${Emojis.CAKE} ${birthday.month.niceName} ${birthday.dayOfMonth}")

                    this@field.inline = true
                }
            }

            if(github != null || website != null || twitch != null) {
                field {
                    this@field.name = "Links"

                    github?.let {
                        this@field.append(Emojis.GITHUB)
                        this@field.append(" **GitHub** - **[")
                        this@field.append(parseGithub(it))
                        this@field.append("](")
                        this@field.append(it)
                        this@field.append(")**\n")
                    }

                    website?.let { website ->
                        this@field.append(Emojis.GLOBE_WITH_MERIDIANS)
                        this@field.append(" **Website** - **")
                        this@field.append(website)
                        this@field.append("**\n")
                    }

                    twitch?.let { twitch ->
                        this@field.append(Emojis.TWITCH)
                        this@field.append(" **Twitch** - **[")
                        this@field.append(parseTwitch(twitch))
                        this@field.append("](")
                        this@field.append(twitch)
                        this@field.append(")**\n")
                    }

                    this@field.inline = false
                }
            }
        }
    }

    companion object {
        val clockIconUrl: String = "https://cdn.pixabay.com/photo/2013/07/13/13/24/clock-160966_960_720.png"

        val githubUrlRegex: Regex = Regex("https\\:\\/\\/github\\.com\\/(\\S+)\\/?")
        val twitchUrlRegex: Regex = Regex("https\\:\\/\\/twitch\\.tv\\/(\\S+)\\/?")

        fun parseGithub(githubUrl: String): String? =
            githubUrlRegex.matchEntire(githubUrl)?.groupValues?.takeIf { it.size > 1 }?.get(1)?.let {
                if(it.endsWith('/')) it.substring(0, it.length-1) else it
            }

        fun parseTwitch(twitchUrl: String): String? =
            twitchUrlRegex.matchEntire(twitchUrl)?.groupValues?.takeIf { it.size > 1 }?.get(1)?.let {
                if(it.endsWith('/')) it.substring(0, it.length-1) else it
            }
    }
}