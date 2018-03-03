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
@file:Suppress("unused")
package xyz.nightfury.command.administrator

import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.EmptyCommand
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.listeners.EventWaiter
import xyz.nightfury.ndb.entities.StarboardSettings
import xyz.nightfury.util.db.*
import xyz.nightfury.util.jda.findTextChannels
import xyz.nightfury.util.multipleTextChannels
import xyz.nightfury.util.niceName
import xyz.nightfury.util.noMatch

/**
 * @author Kaidan Gustave
 */
class LogCommand(waiter: EventWaiter): EmptyCommand(AdministratorGroup) {
    override val name = "Log"
    override val help = "Manage various types of server logs."
    override val children = arrayOf(
        LogConfigureCommand(waiter),
        LogSetCommand(),
        LogRemoveCommand()
    )

    private inner class LogConfigureCommand(private val waiter: EventWaiter): Command(this@LogCommand) {
        override val name = "Configure"
        override val help = "Configures various log settings for the server."

        override suspend fun execute(ctx: CommandContext) {
            ctx.replyWarning("This feature has not been implemented yet!")
        }
    }

    @MustHaveArguments
    private inner class LogSetCommand : Command(this@LogCommand) {
        override val name = "Set"
        override val arguments = "[Type] [Channel]"
        override val help = "Sets the specified type of log for the server."

        override suspend fun execute(ctx: CommandContext) {
            val type = Type.fromArgs(ctx.args) ?: return ctx.replyError {
                "**Invalid type!**\n" +
                "For a list of all available log types, use `${ctx.client.prefix}${parent!!.name} types`!"
            }

            val args = type.trimArgs(ctx.args)
            val channels = ctx.guild.findTextChannels(args)

            val found = when {
                channels.isEmpty() -> return ctx.replyError(noMatch("text channels", args))
                channels.size > 1 -> return ctx.replyError(channels.multipleTextChannels(args))
                else -> channels[0]
            }

            val currentLog = when(type) {
                Type.MOD_LOG -> ctx.guild.modLog
                Type.STARBOARD -> ctx.guild.starboardChannel
            }

            if(currentLog == found) return ctx.replyError {
                "**${currentLog.name}** is already the ${type.niceName} for this server!"
            }

            when(type) {
                Type.MOD_LOG -> {
                    ctx.guild.modLog = found
                    ctx.replySuccess("Successfully set ${found.asMention} as this server's moderation log!")
                }
                Type.STARBOARD -> {
                    val settings = ctx.guild.starboardSettings?.also { it.channelId = found.idLong }
                                   ?: StarboardSettings(found.guild.idLong, found.idLong)
                    ctx.guild.starboardSettings = settings
                    ctx.replySuccess("Successfully set ${found.asMention} as this server's starboard!")
                }
            }
        }
    }

    @MustHaveArguments
    private inner class LogRemoveCommand : Command(this@LogCommand) {
        override val name = "Remove"
        override val arguments = "[Type]"
        override val help = "Removes the specified type of log from the server."

        override suspend fun execute(ctx: CommandContext) {
            val type = Type.fromArgs(ctx.args) ?: return ctx.replyError {
                "**Invalid type!**\n" +
                "For a list of all available log types, use `${ctx.client.prefix}${parent!!.name} types`!"
            }

            when(type) {
                Type.MOD_LOG -> {
                    if(!ctx.guild.hasModLog) return ctx.replyError {
                        "This server has no moderation log to remove!"
                    }

                    ctx.guild.modLog = null
                    ctx.replySuccess("Successfully removed this server's moderation log!")
                }

                Type.STARBOARD -> {
                    if(!ctx.guild.hasStarboard) return ctx.replyError {
                        "This server has no starboard to remove!"
                    }

                    ctx.guild.starboardSettings = null
                    ctx.replySuccess("Successfully removed this server's starboard!")
                }
            }
        }
    }

    private enum class Type(vararg val names: String) {
        MOD_LOG("moderation log", "moderation", "mod log", "mod"),
        STARBOARD("starboard", "star");

        fun trimArgs(args: String): String {
            return names.firstOrNull {
                args.startsWith(it, true)
            }?.let {
                args.substring(it.length).trim()
            } ?: args
        }

        companion object {
            fun fromArgs(args: String): Type? {
                return values().firstOrNull {
                    it.names.any {
                        args.startsWith(it, true)
                    }
                }
            }
        }
    }
}
