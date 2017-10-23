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

import com.jagrosh.jdautilities.menu.Paginator
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.commands.standard.globalTags
import me.kgustave.nightfury.commands.standard.localTags
import me.kgustave.nightfury.db.sql.SQLCustomCommands
import me.kgustave.nightfury.extensions.*

/**
 * @author Kaidan Gustave
 */
class CustomCommandCmd(waiter: EventWaiter) : NoBaseExecutionCommand()
{
    init {
        this.name = "CCommand"
        this.aliases = arrayOf("cc", "customcommand")
        this.arguments = "[Function]"
        this.help = "Manage the server's custom commands."
        this.helpBiConsumer = Command standardSubHelp
                        "Custom Commands are an easy way to create super simple" +
                        "commands for a server.\n" +
                        "In addition to this, custom commands use JagTag syntax in " +
                        "order to process syntax structures such as `{@user}` into a " +
                        "user mention. Listings and descriptions for all structures is " +
                        "available at ${NightFury.GITHUB}wiki"
        this.guildOnly = true
        this.category = Category.ADMIN
        this.children = arrayOf(
                CustomCommandListCmd(waiter),

                CustomCommandAddCmd(),
                CustomCommandImportCmd(),
                CustomCommandRemoveCmd()
        )
    }
}

@MustHaveArguments("Specify arguments in the format `[Command Name] [Command Content]`.")
private class CustomCommandAddCmd : Command()
{
    init {
        this.name = "Add"
        this.fullname = "CCommand Add"
        this.arguments = "[Command Name] [Command Content]"
        this.help = "Adds a custom command."
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex("\\s+"),2)

        val error = when
        {
            parts[0].length>50 ->
                "**Custom Command names cannot exceed 50 characters in length!**\n${SEE_HELP.format(event.client.prefix, fullname)}"
            event.client.commands[parts[0]]!=null ->
                "**Illegal Custom Command Name!**\nCustom Commands may not have names that match standard command names!"
            parts.size==1 ->
                "**You must specify content when creating a Custom Command!**\n${SEE_HELP.format(event.client.prefix, fullname)}"
            parts[1].length>1900 ->
                "**Custom Command content cannot exceed 1900 characters in length!**\n${SEE_HELP.format(event.client.prefix, fullname)}"
            else -> null
        }

        if(error!=null) return event.replyError(error)

        val name = parts[0]
        val content = parts[1]

        if(event.customCommands.getContentFor(name, event.guild).isNotEmpty())
            return event.replyError("Custom Command named \"$name\" already exists!")
        else {
            event.customCommands.add(name, content, event.guild)
            event.replySuccess("Successfully created Custom Command \"**$name**\"!")
            event.invokeCooldown()
        }
    }
}

@MustHaveArguments("Specify a custom command you own to remove.")
private class CustomCommandRemoveCmd : Command()
{
    init {
        this.name = "Remove"
        this.fullname = "CCommand Remove"
        this.arguments = "[Command Name]"
        this.help = "Removes a custom command."
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val name = event.args.split(Regex("\\s+"))[0]
        if(event.customCommands.getContentFor(name, event.guild).isNotEmpty()) {
            event.customCommands.remove(name, event.guild)
            event.replySuccess("Successfully removed Custom Command \"**$name**\"!")
        } else {
            event.replyError("There is no custom command named \"**$name\" on this server!")
        }
    }
}

@MustHaveArguments("Specify the name of the tag to import.")
private class CustomCommandImportCmd : Command()
{
    init {
        this.name = "Import"
        this.fullname = "CCommand Import"
        this.arguments = "[Tag Name]"
        this.help = "Imports a tag as a custom command."
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val name = event.args
        if(!event.localTags.isTag(name, event.guild)) {
            if(!event.globalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else {
                val cmdName = event.globalTags.getOriginalName(name)
                if(event.client.commands[cmdName]!=null)
                    return event.replyError("**Illegal Custom Command Name!**\n" +
                            "Custom Commands may not have names that match standard command names!")
                val cmdCont = event.globalTags.getTagContent(name)
                event.customCommands.add(cmdName, cmdCont, event.guild)
                event.replySuccess("Successfully imported global tag \"$cmdName\" as a Custom Command!")
                event.invokeCooldown()
            }
        } else {
            val cmdName = event.localTags.getOriginalName(name, event.guild)
            if(event.client.commands[cmdName]!=null)
                return event.replyError("**Illegal Custom Command Name!**\n" +
                        "Custom Commands may not have names that match standard command names!")
            val cmdCont = event.localTags.getTagContent(name, event.guild)
            event.customCommands.add(cmdName, cmdCont, event.guild)
            event.replySuccess("Successfully imported local tag \"$cmdName\" as a Custom Command!")
            event.invokeCooldown()
        }
    }
}

@AutoInvokeCooldown
private class CustomCommandListCmd(waiter: EventWaiter) : Command()
{
    init {
        this.name = "List"
        this.fullname = "CCommand List"
        this.help = "Gets a list of all the available custom commands."
        this.guildOnly = true
        this.cooldown = 20
        this.cooldownScope = CooldownScope.USER_GUILD
    }

    val builder : Paginator.Builder = Paginator.Builder()
            .waiter           { waiter }
            .timeout          { delay { 20 } }
            .waitOnSinglePage { false }
            .showPageNumbers  { true }
            .useNumberedItems { true }

    override fun execute(event: CommandEvent)
    {
        val ccs = event.customCommands.getAll(event.guild)
        if(ccs.isEmpty())
            return event.replyError("There are no custom commands on this server!")
        builder.clearItems()
        with(builder)
        {
            text        { _,_ -> "Custom Commands on ${event.guild.name}" }
            items       { addAll(ccs) }
            finalAction { event.linkMessage(it) }
            user        { event.author }
            displayIn   { event.channel }
        }
    }
}

private val CommandEvent.customCommands : SQLCustomCommands
    get() = this.manager.customCommands