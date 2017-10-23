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
package me.kgustave.nightfury.commands.admin

import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.findTextChannels
import me.kgustave.nightfury.extensions.multipleTextChannels
import me.kgustave.nightfury.extensions.noMatch
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
@Suppress("unused")
class StarboardCmd : NoBaseExecutionCommand()
{
    init
    {
        name = "Starboard"
        arguments = "[Function]"
        help = "Manages the server's Starboard."
        category = Category.ADMIN
        guildOnly = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        children = arrayOf(StarboardSetCmd())
    }
}

@MustHaveArguments("Please specify a channel to use as the server's Starboard!")
private class StarboardSetCmd : Command()
{
    init
    {
        name = "Set"
        fullname = "Starboard Set"
        arguments = "[Channel]"
        help = "Sets the server's Starboard."
        cooldown = 15
        cooldownScope = CooldownScope.GUILD
        category = Category.ADMIN
        guildOnly = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val tcs = event.guild.findTextChannels(event.args)

        if(tcs.isEmpty())
            return event.replyError(noMatch("TextChannels", event.args))
        if(tcs.size > 1)
            return event.replyError(tcs.multipleTextChannels(event.args))

        //event.manager.setStarboard(tcs[0])

        event.replySuccess("Successfully set ${tcs[0].asMention} as this server's Starboard!")
    }
}