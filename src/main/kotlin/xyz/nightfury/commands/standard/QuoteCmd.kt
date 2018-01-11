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
package xyz.nightfury.commands.standard

import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.entities.embed
import xyz.nightfury.resources.Arguments
import xyz.nightfury.extensions.formattedName
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import xyz.nightfury.annotations.HasDocumentation

@HasDocumentation
@MustHaveArguments("Please specify a message ID to quote!")
class QuoteCmd : Command() {
    init {
        this.name = "Quote"
        this.arguments = "<Channel ID> [Message ID]"
        this.help = "Quotes a message by ID."
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val split = event.args.split(Arguments.commandArgs)
        val channel: TextChannel
        val message: Message

        when(split.size) {
            1 -> {
                channel = event.textChannel
                message = try {
                    channel.getMessageById(split[0].toLong()).complete()
                } catch (e: NumberFormatException) {
                    return event.replyError(INVALID_ARGS_ERROR.format("\"${split[0]}\" is not a valid message ID"))
                } catch (e : Exception) {
                    null
                } ?:return event.replyError("Could not find a message with ID: ${split[0]}!")
            }

            2 -> {
                channel = try {
                    event.guild.getTextChannelById(split[0].toLong())
                            ?:return event.replyError("Could not find a channel with ID: ${split[0]}!")
                } catch (e: NumberFormatException) {
                    return event.replyError(INVALID_ARGS_ERROR.format("\"${split[0]}\" is not a valid channel ID"))
                }

                message = try {
                    channel.getMessageById(split[1].toLong()).complete()
                } catch (e: NumberFormatException) {
                    return event.replyError(INVALID_ARGS_ERROR.format("\"${split[1]}\" is not a valid message ID"))
                } catch (e : Exception) {
                    null
                } ?:return event.replyError("Could not find a message with ID: ${split[1]}!")
            }

            else -> return event.replyError("**Too Many Arguments!**\n" +
                                            "Provide either a channel ID and message ID or just a message ID!")
        }

        event.reply(embed {
            author {
                value = message.author.formattedName(false)
                url   = message.author.effectiveAvatarUrl
                icon  = message.author.effectiveAvatarUrl
            }

            description {  message.contentRaw  }
            time        { message.creationTime }
            color       { message.member?.color }
        })

        event.invokeCooldown()
    }
}
