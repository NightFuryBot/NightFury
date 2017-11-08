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
package xyz.nightfury.commands.standard

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Member
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLProfiles
import xyz.nightfury.entities.Profile
import xyz.nightfury.extensions.*
import xyz.nightfury.resources.Arguments
import java.time.*

/**
 * @author Kaidan Gustave
 */
class ProfileCmd : Command() {
    init {
        this.name = "Profile"
        this.aliases = arrayOf("p")
        this.arguments = "<User>"
        this.help = "Displays a user's NightFury Profile."
        this.guildOnly = false
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
        this.botPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS)
        this.children = arrayOf(
            ProfileCreateCmd(),
            ProfileSetCmd()
        )
    }

    override fun execute(event: CommandEvent) {
        val query = event.args

        val temp : Member? = if(event.isFromType(ChannelType.TEXT)) {
            if(query.isEmpty())
                event.member
            else {
                val found = event.guild.findMembers(query)
                when {
                    found.isEmpty() -> null
                    found.size>1 -> return event.replyError(found.multipleMembers(query))
                    else -> found[0]
                }
            }
        } else null

        val profile : Profile = when {
            query.isEmpty() -> SQLProfiles.getProfile(event.author)
                               ?: return event.replyError("**You have not created a profile!**\n" +
                                                          "Use `Profile Create` to create a profile!")
            temp != null -> SQLProfiles.getProfile(temp.user)
                          ?: return event.replyError("${temp.user.formattedName(true)} has not created a profile!")
            else -> {
                val found =  event.jda.findUsers(query)
                when {
                    found.isEmpty() -> return event.replyError(noMatch("users", query))
                    found.size>1    -> return event.replyError(found.multipleUsers(query))
                    else            -> SQLProfiles.getProfile(found[0])
                                       ?: return event.replyError("${found[0].formattedName(true)} " +
                                                                  "has not created a profile!")
                }
            }
        }

        event.reply(profile.toEmbed(color = event.guild?.getMember(profile.user)?.color))

        event.invokeCooldown()
    }
}

private class ProfileCreateCmd : Command() {
    init {
        this.name = "Create"
        this.fullname = "Profile Create"
        this.help = "Creates a NightFury Profile for you."
        this.guildOnly = false
        this.botPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        if(SQLProfiles.getProfile(event.author) != null)
            return event.replyWarning("You have already set up a NightFury Profile!")

        SQLProfiles.createProfile(event.author)
        event.replySuccess("Successfully created a NightFury Profile! Use `Profile Set` to customize it!")
    }
}

@MustHaveArguments
private class ProfileSetCmd : Command() {
    init {
        this.name = "Set"
        this.fullname = "Profile Set"
        this.help = "Sets a Profile field."
        this.guildOnly = false
        this.botPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Arguments.commandArgs, 2)

        val profile = SQLProfiles.getProfile(event.author)
                      ?: return event.replyError("**You have not created a NightFury Profile yet!**\n" +
                                                 "Use `Profile Create` to set one up, then try again!")

        val profCat = ProfileCategory.values().find {
            it.catName.any { it == parts[0] }
        }
                      ?: return event.replyError("**\"${parts[0]}\" is not a profile category!**\n" +
                                                 "For a full list of profile categories, use `Profile Help`")

        if(parts.size < 2) {
            // Note to self, it might be worth adding a check here that
            // tells a user that they need to specify arguments if the
            // category is already null.
            when(profCat) {
                ProfileCategory.TITLE -> profile.title = null
                ProfileCategory.TIME_ZONE -> profile.timeZone = null
                ProfileCategory.ABOUT -> profile.about = null
                ProfileCategory.BIRTHDAY -> profile.birthday = null
                ProfileCategory.TWITCH -> profile.twitch = null
                ProfileCategory.GITHUB -> profile.github = null
                ProfileCategory.WEBSITE -> profile.website = null
            }
            event.replySuccess("Successfully removed **${profCat.niceName}**!")
        } else {
            val value = parts[1]
            when(profCat) {
                ProfileCategory.TIME_ZONE -> {
                    profile.timeZone = timeZone(value)
                                       ?: return event.replyError("**The specified value `$value` is not a valid timezone!**\n" +
                                                                  "For a list of valid timezones use `Profile TimeZones`")
                }

                ProfileCategory.ABOUT -> {
                    if(value.length > 1800) {
                        return event.replyError("The value to set was too long! " +
                                                "The maximum length of **${profCat.niceName}** is 1800 characters!")
                    }

                    profile.about = value
                }

                ProfileCategory.GITHUB -> {
                    Profile.parseGithub(value)
                    ?: return event.replyError("Invalid GitHub URL specified! " +
                                               "You must provide a valid GitHub User URL!")
                    profile.github = value
                }

                ProfileCategory.BIRTHDAY -> {
                    val birthdayMatch = yyyyMMddRegex.matchEntire(value)
                                   ?: return event.replyError("The date specified was an invalid format! " +
                                                              "You must provide a date matching `yyyy/MM/dd` " +
                                                              "or `yyyy-MM-dd`!")
                    try {
                        val groups = birthdayMatch.groupValues
                        val yearInt = groups[1].toInt()
                        // lets be real, no 87 year old is using discord
                        if(Year.now().value < yearInt || yearInt < 1930) {
                            return event.replyError("`$yearInt` is not a valid year!")
                        }

                        val year = Year.of(yearInt)

                        val monthInt = groups[2].let {
                            if(it.startsWith("0")) it[1].toString().toInt() else it.toInt()
                        }.takeIf { it <= 12 }
                                       ?: return event.replyError("`${groups[2]}` is not a valid month!")

                        val month = Month.of(monthInt)
                        val monthDay = groups[3].let {
                            if(it.startsWith("0")) it[1].toString().toInt() else it.toInt()
                        }.run {
                            try {
                                MonthDay.of(month, this)
                            } catch(e: DateTimeException) { null }
                        }
                                       ?: return event.replyError("`${groups[3]}` is not a valid " +
                                                                  "day for the month of `${month.niceName}`!")

                        if(!year.isValidMonthDay(monthDay)) {
                            return event.replyError("The specified day and month is not valid for the year `${year.value}`!")
                        }

                        profile.birthday = year.atMonthDay(monthDay).atStartOfDay().atOffset(ZoneOffset.UTC)
                    } catch(e: Exception) {
                        return event.replyError("An error occurred while parsing the arguments provided!")
                    }
                }

                ProfileCategory.TITLE -> {
                    if(value.length > 50) {
                        return event.replyError("The value to set was too long! " +
                                                "The maximum length of **${profCat.niceName}** is 50 characters!")
                    }

                    profile.title = value
                }

                ProfileCategory.TWITCH -> {
                    Profile.parseTwitch(value)
                    ?: return event.replyError("Invalid Twitch URL specified! " +
                                               "You must provide a valid Twitch User URL!")
                    profile.twitch = value
                }

                ProfileCategory.WEBSITE -> {
                    TODO("Not yet implemented")
                }
            }
            event.replySuccess("Successfully set **${profCat.niceName}**!")
        }
    }

    companion object {
        val yyyyMMddRegex: Regex = Regex("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})")
    }
}

enum class ProfileCategory(vararg val catName: String) {
    TIME_ZONE("timezone", "tz", "location"),
    GITHUB("github"),
    WEBSITE("website", "site"),
    TWITCH("twitch"),
    TITLE("title", "titlecard"),
    BIRTHDAY("birthday"),
    ABOUT("about", "information", "info")
}