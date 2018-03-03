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
package xyz.nightfury.command.administrator

import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.EmptyCommand
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.listeners.EventWaiter
import xyz.nightfury.util.commandArgs
import xyz.nightfury.util.db.*
import xyz.nightfury.util.menus.Paginator

/**
 * @author Kaidan Gustave
 */
class CustomCmdCommand(waiter: EventWaiter): EmptyCommand(AdministratorGroup) {
    companion object {
        private const val NAME_MAX_LENGTH = 50
        private const val CONTENT_MAX_LENGTH = 1900
    }

    override val name = "CustomCmd"
    override val help = "Manage the server's custom commands."
    override val aliases = arrayOf("CustomCommand", "CC")
    override val children = arrayOf(
        CustomCmdAddCommand(),
        CustomCmdImportCommand(),
        CustomCmdListCommand(waiter),
        CustomCmdRemoveCommand()
    )

    @MustHaveArguments("Specify arguments in the format `%arguments`.")
    private inner class CustomCmdAddCommand : Command(this@CustomCmdCommand) {
        override val name = "Add"
        override val arguments = "[Command Name] [Command Content]"
        override val help = "Adds a custom command."
        override val cooldown = 30
        override val cooldownScope = CooldownScope.GUILD

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)

            when {
                parts[0].length > NAME_MAX_LENGTH -> return ctx.replyError {
                    "Custom Command names cannot exceed 50 characters in length!"
                }
                ctx.client.commands[parts[0]] !== null -> return ctx.replyError {
                    "Custom Commands may not have names that match standard command names!"
                }
                parts.size == 1 -> return ctx.replyError {
                    "You must specify content when creating a Custom Command!"
                }
                parts[1].length > CONTENT_MAX_LENGTH -> return ctx.replyError {
                    "Custom Command content cannot exceed 1900 characters in length!"
                }
            }

            val name = parts[0]
            val content = parts[1]

            if(ctx.guild.getCustomCommand(name) !== null) return ctx.replyError {
                "Custom Command named \"$name\" already exists!"
            }

            ctx.guild.setCustomCommand(name, content)
            ctx.replySuccess("Successfully created Custom Command \"**$name**\"!")
            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments("Specify the name of the tag to import.")
    private inner class CustomCmdImportCommand : Command(this@CustomCmdCommand) {
        override val name = "Import"
        override val arguments = "[Tag Name]"
        override val help = "Imports a tag as a custom command."
        override val cooldown = 30
        override val cooldownScope = CooldownScope.GUILD

        override suspend fun execute(ctx: CommandContext) {
            val name = ctx.args

            if(ctx.guild.getCustomCommand(name) !== null) return ctx.replyError {
                "Custom Command named \"$name\" already exists!"
            }

            if(ctx.guild.isTag(name) || ctx.jda.isTag(name)) {
                val tag = checkNotNull(ctx.guild.getTagByName(name)) {
                    "Expected non-null tag from Guild (ID: ${ctx.guild.idLong}) with name $name"
                }

                ctx.guild.setCustomCommand(tag.name, tag.content)
                ctx.replySuccess("Successfully created Custom Command \"**$name**\"!")
            } else {
                ctx.replyError("Tag named \"$name\" does not exist!")
            }
        }
    }

    private inner class CustomCmdListCommand(waiter: EventWaiter): Command(this@CustomCmdCommand) {
        override val name = "List"
        override val help = "Gets a list of all the available custom commands."
        override val cooldown = 20
        override val cooldownScope = CooldownScope.USER_GUILD

        private val builder = Paginator.Builder {
            waiter           { waiter }
            timeout          { delay { 20 } }
            waitOnSinglePage { false }
            showPageNumbers  { true }
            numberItems      { true }
        }

        override suspend fun execute(ctx: CommandContext) {
            val commands = ctx.guild.customCommands

            if(commands.isEmpty()) {
                return ctx.replyError("There are no custom commands on this server!")
            }
            builder.clearItems()
            val paginator = Paginator(builder) {
                text        { _,_ -> "Custom Commands on ${ctx.guild.name}" }
                items       { addAll(commands.map { it.first }) }
                finalAction { ctx.linkMessage(it) }
                user        { ctx.author }
            }
            paginator.displayIn(ctx.channel)
        }
    }

    @MustHaveArguments("Specify a custom command to remove.")
    private inner class CustomCmdRemoveCommand : Command(this@CustomCmdCommand) {
        override val name = "Remove"
        override val arguments = "[Command Name]"
        override val help = "Removes a custom command."

        override suspend fun execute(ctx: CommandContext) {
            val name = ctx.args

            if(ctx.guild.getCustomCommand(name) === null) return ctx.replyError {
                "Custom Command named \"$name\" does not exist!"
            }

            ctx.guild.removeCustomCommand(name)
            ctx.replySuccess("Successfully created Custom Command \"**$name**\"!")
        }
    }
}
