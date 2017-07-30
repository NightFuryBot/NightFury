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

import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.commands.standard.globalTags
import me.kgustave.nightfury.commands.standard.localTags
import me.kgustave.nightfury.extensions.waiting.paginator

/**
 * @author Kaidan Gustave
 */
class CustomCommandCmd(waiter: EventWaiter) : NoBaseExecutionCommand()
{
    init {
        this.name = "ccommand"
        this.aliases = arrayOf("cc", "customcommand")
        this.arguments = Argument("[add|remove|list|import]")
        this.help = "manage the server's available commands"
        this.guildOnly = true
        this.category = Category.ADMIN
        this.children = arrayOf(
                CustomCommandAddCmd(),
                CustomCommandImportCmd(),
                CustomCommandListCmd(waiter),
                CustomCommandRemoveCmd()
        )
    }
}

private class CustomCommandAddCmd : Command()
{
    init {
        this.name = "add"
        this.fullname = "ccommand add"
        this.arguments = Argument("[command name] [command content]")
        this.help = "adds a custom command"
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length>50)
            return event.replyError("**Custom Command names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(event.client.getCommandByName(parts[0])!=null)
                return event.replyError("**Illegal Custom Command Name!**\n" +
                        "Custom Commands may not have names that match standard command names!")
        else parts[0]

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a Custom Command!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Custom Command content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        with(event.client.manager.customCommands)
        {
            if(getContentFor(name, event.guild).isNotEmpty())
                return event.replyError("Custom Command named \"$name\" already exists!")
            else {
                add(name, content, event.guild)
                event.replySuccess("Successfully created Custom Command \"**$name**\"!")
                invokeCooldown(event)
            }
        }
    }
}

private class CustomCommandRemoveCmd : Command()
{
    init {
        this.name = "remove"
        this.fullname = "ccommand remove"
        this.arguments = Argument("[command name]")
        this.help = "removes a custom command"
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val name = event.args.split(Regex("\\s+"))[0]
        with(event.client.manager.customCommands) {
            if(getContentFor(name, event.guild).isNotEmpty()) {
                remove(name, event.guild)
                event.replySuccess("Successfully removed Custom Command \"**$name**\"!")
            } else {
                event.replyError("There is no custom command named \"**$name\" on this server!")
            }
        }
    }
}

private class CustomCommandImportCmd : Command()
{
    init {
        this.name = "import"
        this.fullname = "ccommand import"
        this.arguments = Argument("[tag name]")
        this.help = "imports a tag as a custom command"
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val name = event.args
        with(event.client.manager)
        {
            if(!event.localTags.isTag(name, event.guild)) {
                if(!event.globalTags.isTag(name)) {
                    event.replyError("Tag named \"$name\" does not exist!")
                } else {
                    val cmdName = event.globalTags.getOriginalName(name)
                    if(event.client.getCommandByName(cmdName)!=null)
                        return event.replyError("**Illegal Custom Command Name!**\n" +
                                "Custom Commands may not have names that match standard command names!")
                    val cmdCont = event.globalTags.getTagContent(name)
                    customCommands.add(cmdName, cmdCont, event.guild)
                    event.replySuccess("Successfully imported global tag \"$cmdName\" as a Custom Command!")
                    invokeCooldown(event)
                }
            } else {
                val cmdName = event.localTags.getOriginalName(name, event.guild)
                if(event.client.getCommandByName(cmdName)!=null)
                    return event.replyError("**Illegal Custom Command Name!**\n" +
                            "Custom Commands may not have names that match standard command names!")
                val cmdCont = event.localTags.getTagContent(name, event.guild)
                customCommands.add(cmdName, cmdCont, event.guild)
                event.replySuccess("Successfully imported local tag \"$cmdName\" as a Custom Command!")
                invokeCooldown(event)
            }
        }
    }
}

@AutoInvokeCooldown
private class CustomCommandListCmd(val waiter: EventWaiter) : Command()
{
    init {
        this.name = "list"
        this.fullname = "ccommand list"
        this.help = "gets a list of all the available custom commands"
        this.category = Category.ADMIN
        this.guildOnly = true
        this.cooldown = 20
        this.cooldownScope = CooldownScope.USER_GUILD
    }

    override fun execute(event: CommandEvent)
    {
        val ccs = event.client.manager.customCommands.getAll(event.guild)
        if(ccs.isEmpty())
            return event.replyError("There are no custom commands on this server!")
        paginator(waiter, event.channel)
        {
            text { "Custom Commands on ${event.guild.name}" }
            timeout { 20 }
            items { addAll(ccs) }
            finalAction { it.editMessage(it).queue(); event.linkMessage(it) }
            showPageNumbers  { true }
            useNumberedItems { true }
            waitOnSinglePage { false }
        }
    }
}