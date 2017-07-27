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

import club.minnced.kjda.builders.colorAwt
import club.minnced.kjda.builders.embed
import me.kgustave.nightfury.*
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
class PrefixCmd : NoBaseExecutionCommand() {
    init {
        this.name = "prefix"
        this.arguments = Argument("[add|remove|list]")
        this.help = "manages the server's custom prefixes"
        this.guildOnly = true
        this.category = Category.ADMIN
        this.children = arrayOf(AddPrefixCmd(), ListPrefixCmd(), RemovePrefixCmd()
        )
    }
}

private class AddPrefixCmd : Command() {

    init {
        this.name = "add"
        this.fullname = "prefix add"
        this.arguments = Argument("<prefix>")
        this.help = "adds a custom prefix for the bot"
        this.guildOnly = true
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val args = event.args
        if(args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.client.prefix,"prefix $name"))
        else if(args.equals(event.client.prefix, true))
            return event.replyError("`$args` cannot be added as a prefix because it is the default prefix!")
        else if(event.client.manager.isPrefixFor(event.guild, args))
            return event.replyError("`$args` cannot be added as a prefix because it is already a prefix!")
        else if(args.length>50)
            return event.replyError("`$args` cannot be added as a prefix because it's longer than 50 characters!")
        else
        {
            event.client.manager.addPrefix(event.guild, args)
            event.replySuccess("`$args` was added as a prefix!")
        }
    }
}

private class RemovePrefixCmd : Command() {

    init {
        this.name = "remove"
        this.name = "prefix remove"
        this.arguments = Argument("<prefix>")
        this.help = "removes a custom prefix for the bot"
        this.guildOnly = true
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val args = event.args
        if(args.isEmpty())
            return event.replyError(Command.TOO_FEW_ARGS_HELP.format(event.client.prefix,"prefix $name"))
        else if(args.equals(event.client.prefix, true))
            return event.replyError("`$args` cannot be removed as a prefix because it is the default prefix!")
        else if(!event.client.manager.isPrefixFor(event.guild, args))
            return event.replyError("`$args` cannot be removed as a prefix because it is not a prefix!")
        else
        {
            event.client.manager.removePrefix(event.guild, args)
            event.replySuccess("`$args` was removed as a prefix!")
        }
    }
}

private class ListPrefixCmd: Command() {

    init {
        this.name = "list"
        this.fullname = "prefix list"
        this.help = "lists all custom prefixes for this server"
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val prefixes = event.client.manager.getPrefixes(event.guild)
        if(prefixes.isEmpty())
            return event.replyError("There are no custom prefixes available for this server!")
        else {
            event.reply(embed {
                title = "Custom prefixes for **${event.guild.name}"
                prefixes.forEach { prefix -> append("`$prefix`\n") }
                colorAwt = event.selfMember.color
            })
        }
    }
}