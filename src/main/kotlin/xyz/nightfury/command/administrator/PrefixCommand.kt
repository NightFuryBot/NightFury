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
import xyz.nightfury.util.db.addPrefix
import xyz.nightfury.util.db.hasPrefix
import xyz.nightfury.util.db.prefixes
import xyz.nightfury.util.db.removePrefix
import xyz.nightfury.util.menus.Paginator

/**
 * @author Kaidan Gustave
 */
class PrefixCommand(waiter: EventWaiter) : EmptyCommand(AdministratorGroup) {
    override val name = "Prefix"
    override val help = "Manage the bot's custom prefixes for the server."
    override val children = arrayOf(
        PrefixAddCommand(),
        PrefixListCommand(waiter),
        PrefixRemoveCommand()
    )

    @MustHaveArguments
    private inner class PrefixAddCommand : Command(this@PrefixCommand) {
        override val name = "Add"
        override val arguments = "[Prefix]"
        override val help = "Adds a custom prefix to the bot for the server."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            when {
                args.equals(ctx.client.prefix, true) -> return ctx.replyError {
                    "`$args` cannot be added as a prefix because it is the default prefix!"
                }

                args.length > 50 -> return ctx.replyError {
                    "`$args` cannot be added as a prefix because it is the default prefix!"
                }

                ctx.guild.hasPrefix(args) -> return ctx.replyError {
                    "`$args` cannot be added as a prefix because it is already a prefix!"
                }

                else -> {
                    ctx.guild.addPrefix(args)
                    ctx.replySuccess("`$args` was added as a prefix!")
                }
            }
        }
    }

    @MustHaveArguments
    private inner class PrefixRemoveCommand : Command(this@PrefixCommand) {
        override val name = "Remove"
        override val arguments = "[Prefix]"
        override val help = "Removes a custom prefix from the bot for the server."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            when {
                args.equals(ctx.client.prefix, true) -> return ctx.replyError {
                    "`$args` cannot be removed as a prefix because it is the default prefix!"
                }

                !ctx.guild.hasPrefix(args) -> return ctx.replyError {
                    "`$args` cannot be removed as a prefix because it is not a prefix!"
                }

                else -> {
                    ctx.guild.removePrefix(args)
                    ctx.replySuccess("`$args` was removed as a prefix!")
                }
            }
        }
    }

    private inner class PrefixListCommand(waiter: EventWaiter): Command(this@PrefixCommand) {
        override val name = "List"
        override val help = "Gets a list of the server's custom prefixes."
        override val cooldown = 10
        override val cooldownScope = CooldownScope.USER_GUILD
        override val defaultLevel = Level.STANDARD

        private val builder = Paginator.Builder {
            waiter           { waiter }
            waitOnSinglePage { false }
            showPageNumbers  { true }
            itemsPerPage     { 10 }
            numberItems      { true }
            text { p, t -> "Server Prefixes${if(t > 1) " [`$p/$t`]" else ""}" }
        }

        override suspend fun execute(ctx: CommandContext) {
            val prefixes = ctx.guild.prefixes
            builder.clearItems()
            val paginator = Paginator(builder) {
                items { addAll(prefixes) }
                finalAction { ctx.linkMessage(it) }
                user  { ctx.author }
            }
            paginator.displayIn(ctx.channel)
        }
    }
}
