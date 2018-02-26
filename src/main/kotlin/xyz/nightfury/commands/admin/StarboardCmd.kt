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
package xyz.nightfury.commands.admin

import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.util.ext.findTextChannels
import xyz.nightfury.util.ext.multipleTextChannels
import xyz.nightfury.util.ext.noMatch
import net.dv8tion.jda.core.Permission
import xyz.nightfury.*
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.entities.starboard.StarboardHandler

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class StarboardCmd : NoBaseExecutionCommand() {
    init {
        name = "Starboard"
        arguments = "[Function]"
        help = "Manages the server's Starboard."
        category = Category.ADMIN
        guildOnly = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        children = arrayOf(
            StarboardMaxAgeCmd(),
            StarboardSetCmd(),
            StarboardThresholdCmd()
        )
    }
}

@MustHaveArguments("Please specify a channel to use as the server's Starboard!")
private class StarboardSetCmd : Command() {
    init {
        name = "Set"
        fullname = "Starboard Set"
        arguments = "[Channel]"
        help = "Sets the server's Starboard."
        category = Category.ADMIN
        guildOnly = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val tcs = event.guild.findTextChannels(event.args)

        if(tcs.isEmpty())
            return event.replyError(noMatch("TextChannels", event.args))
        if(tcs.size > 1)
            return event.replyError(tcs.multipleTextChannels(event.args))

        // Delete prior to creation of new settings
        if(StarboardHandler.hasStarboard(event.guild))
            StarboardHandler.removeStarboard(event.guild)

        StarboardHandler.createStarboard(tcs[0])

        event.replySuccess("Successfully set ${tcs[0].asMention} as this server's Starboard!")
    }
}

@MustHaveArguments("Specify a number of stars to get an entry on the starboard!")
private class StarboardThresholdCmd : Command() {
    init {
        name = "Threshold"
        fullname = "Starboard Threshold"
        arguments = "[3-10]"
        help = "Sets the number of stars required to get an entry in the Starboard."
        category = Category.ADMIN
        guildOnly = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val args = try { event.args.toInt() } catch(e: Exception) {
            return event.replyError(INVALID_ARGS_ERROR.format(
                "Specify a number of of stars to get an entry on the starboard!"))
        }

        if(args > 10 || args < 3) {
            return event.replyError("Invalid number of stars! Must be a number 3 - 10")
        }

        val starboard = StarboardHandler.getStarboard(event.guild)
        ?: return event.replyError("There is no starboard for this server. Use `Starboard Set` to set the server's starboard!")

        starboard.threshold = args

        event.replySuccess("Successfully set the server's starboard threshold to `$args` stars")
    }
}

@MustHaveArguments("Specify a number of hours a message can be put on the Starboard after it's created!")
private class StarboardMaxAgeCmd : Command() {
    init {
        name = "MaxAge"
        fullname = "Starboard MaxAge"
        arguments = "[3 - 168]"
        help = "Sets the number of hours a message can be put on the Starboard after it's created."
        category = Category.ADMIN
        guildOnly = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val args = try { event.args.toInt() } catch(e: Exception) {
            return event.replyError(INVALID_ARGS_ERROR.format(
                "Specify a number of of hours a message can be put on the Starboard after it's created!"
            ))
        }

        if(args > 10 || args < 3) {
            return event.replyError("Invalid number of stars! Must be a number 3 - 168")
        }

        val starboard = StarboardHandler.getStarboard(event.guild)
                        ?: return event.replyError("There is no starboard for this server. Use `Starboard Set` to set the server's starboard!")

        starboard.maxAge = args

        event.replySuccess("Successfully set the server's starboard max age to `$args` hours")
    }
}
