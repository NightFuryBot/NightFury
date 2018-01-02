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

import xyz.nightfury.*
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLWelcomes
import xyz.nightfury.extensions.findTextChannels
import xyz.nightfury.extensions.multipleTextChannels
import xyz.nightfury.extensions.noMatch

/**
 * @author Kaidan Gustave
 */
class WelcomeCmd : NoBaseExecutionCommand()
{
    init {
        this.name = "Welcome"
        this.arguments = "[Function]"
        this.help = "Manages the server's welcome system."
        this.guildOnly = true
        this.category = Category.ADMIN
        this.children = arrayOf(
                WelcomeDisableCmd(),
                WelcomeSetCmd(),
                WelcomeRawCmd()
        )
    }
}

@MustHaveArguments
private class WelcomeSetCmd : Command()
{
    init {
        this.name = "Set"
        this.fullname = "Welcome Set"
        this.arguments = "[Channel] [Welcome Message]"
        this.help = "Enables and sets the server's welcome system."
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.guildOnly = true
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val args = event.args.split(Regex("\\s+"),2)
        if(args.size<2)
            return event.replyError(TOO_FEW_ARGS_ERROR.format("Please specify a channel and a welcome message!"))
        if(args[1].length>1900)
            return event.replyError("**Welcome message content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))
        val channel = with(event.guild findTextChannels args[0])
        {
            if(isEmpty())
                return event.replyError(noMatch("text channels", args[0]))
            if(size > 1)
                return event.replyError(this multipleTextChannels args[0])
            return@with this[0]
        }

        if(!channel.canTalk()) return event.replyError("I cannot set the welcome channel to ${channel.asMention} because I do not have the permission to send messages there!")

        SQLWelcomes.setWelcome(channel, args[1])
        event.replySuccess("Successfully set welcome message for this server!")
        event.invokeCooldown()
    }
}

private class WelcomeDisableCmd : Command()
{
    init {
        this.name = "Disable"
        this.fullname = "Welcome Disable"
        this.help = "Disables the server's welcome system."
        this.guildOnly = true
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        if(!SQLWelcomes.hasWelcome(event.guild))
            return event.replyError("I cannot disable welcomes because it is not enabled for this server!")
        SQLWelcomes.removeWelcome(event.guild)
        event.replySuccess("Successfully disabled welcomes for this server!")
    }
}

private class WelcomeRawCmd : Command()
{
    init {
        this.name = "Raw"
        this.fullname = "Welcome Raw"
        this.help = "Gets the raw value for the server's welcome message."
        this.guildOnly = true
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val message = SQLWelcomes.getMessage(event.guild)
                ?:return event.replyError("This server does not have a welcome message!")
        event.replySuccess("**Welcome message for ${event.guild.name}:** ```\n$message```")
    }
}