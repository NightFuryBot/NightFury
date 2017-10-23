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

        if(!channel.canTalk()) return event.replyError("I cannot speak in the channel you specified!")

        event.manager.setWelcome(channel, args[1])
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
        if(!event.manager.hasWelcome(event.guild))
            return event.replyError("I cannot disable welcomes because it is not enabled for this server!")
        event.manager.resetWelcome(event.guild)
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
        val message = event.manager.getWelcomeMessage(event.guild)
                ?:return event.replyError("This server does not have a welcome message!")
        event.replySuccess("**Welcome message for ${event.guild.name}:** ```\n$message```")
    }
}