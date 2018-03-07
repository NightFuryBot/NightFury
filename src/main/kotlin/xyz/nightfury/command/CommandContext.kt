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
@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused")
package xyz.nightfury.command

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction
import xyz.nightfury.Client
import xyz.nightfury.NightFury
import xyz.nightfury.util.emoteRegex
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.jda.filterMassMentions
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @author Kaidan Gustave
 */
class CommandContext(
    val event: MessageReceivedEvent,
    val client: Client,
    var args: String,
    context: CoroutineContext
): CoroutineContext by context {
    val jda: JDA = event.jda
    val author: User = event.author
    val message: Message = event.message
    val channel: MessageChannel = event.channel
    val channelType: ChannelType = event.channelType
    val responseNumber: Long = event.responseNumber
    val messageIdLong: Long = event.messageIdLong
    val selfUser: SelfUser = jda.selfUser

    val guild: Guild get() = nonNullFromGuild(event.guild, "Guild")
    val member: Member get() = nonNullFromGuild(event.member, "Member")
    val textChannel: TextChannel get() = nonNullFromGuild(event.textChannel, "TextChannel")
    val selfMember: Member get() = nonNullFromGuild(event.guild?.selfMember, "SelfMember")
    val privateChannel: PrivateChannel get() = nonNullFromNonGuild(event.privateChannel, "PrivateChannel")

    val isGuild: Boolean get() = channelType.isGuild
    val isPrivate: Boolean get() = !isGuild
    val isDev: Boolean get() = author.idLong == NightFury.DEV_ID

    fun reply(text: String) {
        checkForTalking(channel)
        sendMessage(text, channel).queue(::linkMessage, {})
    }

    fun reply(embed: MessageEmbed) {
        checkForTalking(channel)
        channel.sendMessage(embed).queue(this::linkMessage, {})
    }

    fun reply(message: Message) {
        checkForTalking(channel)
        channel.sendMessage(message).queue(this::linkMessage, {})
    }

    suspend fun send(text: String): Message {
        checkForTalking(channel)
        return sendMessage(text, channel).await().also(::linkMessage)
    }

    suspend fun send(embed: MessageEmbed): Message {
        checkForTalking(channel)
        return channel.sendMessage(embed).await().also(::linkMessage)
    }

    suspend fun send(message: Message): Message {
        checkForTalking(channel)
        return channel.sendMessage(message).await().also(::linkMessage)
    }

    fun replyInDM(text: String) {
        if(channel is PrivateChannel) {
            return sendMessage(text, channel).queue()
        }
        author.openPrivateChannel().queue({ pc ->
            sendMessage(text, pc).queue()
        }, {
            if(isGuild && textChannel.canTalk()) {
                replyError(BLOCKING_ERROR)
            }
        })
    }

    fun replyInDM(embed: MessageEmbed) {
        if(channel is PrivateChannel) {
            return channel.sendMessage(embed).queue({}, {})
        }
        author.openPrivateChannel().queue({ pc ->
            pc.sendMessage(embed).queue({}, {})
        }, {
            if(isGuild && textChannel.canTalk()) {
                replyError(BLOCKING_ERROR)
            }
        })
    }

    fun replyInDM(message: Message) {
        if(channel is PrivateChannel) {
            return channel.sendMessage(message).queue({}, {})
        }
        author.openPrivateChannel().queue({ pc ->
            pc.sendMessage(message).queue({}, {})
        }, {
            if(isGuild && textChannel.canTalk()) {
                replyError(BLOCKING_ERROR)
            }
        })
    }

    fun replySuccess(text: String) = reply("${NightFury.SUCCESS} $text")
    fun replyWarning(text: String) = reply("${NightFury.WARNING} $text")
    fun replyError(text: String) = reply("${NightFury.ERROR} $text")

    suspend fun sendSuccess(text: String) = send("${NightFury.SUCCESS} $text")
    suspend fun sendWarning(text: String) = send("${NightFury.WARNING} $text")
    suspend fun sendError(text: String) = send("${NightFury.ERROR} $text")

    fun reactSuccess() = react(NightFury.SUCCESS)
    fun reactWarning() = react(NightFury.WARNING)
    fun reactError() = react(NightFury.ERROR)

    fun linkMessage(message: Message) = client.linkCall(messageIdLong, message)

    private fun react(string: String) {
        if(emoteRegex matches string) {
            jda.getEmoteById(emoteRegex.replace(string, "$1"))?.let(::addReaction)
        } else {
            addReaction(string)
        }
    }

    private fun addReaction(emote: String) {
        if(isGuild) {
            if(selfMember.hasPermission(textChannel, MESSAGE_ADD_REACTION)) {
                message.addReaction(emote).queue({},{})
            }
        } else {
            message.addReaction(emote).queue({},{})
        }
    }

    private fun addReaction(emote: Emote) {
        if(event.isFromType(ChannelType.TEXT)) {
            if(selfMember.hasPermission(textChannel, MESSAGE_ADD_REACTION)) {
                message.addReaction(emote).queue({},{})
            }
        } else {
            message.addReaction(emote).queue({},{})
        }
    }

    private fun sendMessage(text: String, channel: MessageChannel): RestAction<Message> {
        val parts = processMessage(text)
        if(parts.size == 1) {
            return channel.sendMessage(parts[0])
        }

        parts.forEachIndexed { i, s ->
            val action = channel.sendMessage(s)
            if(i == parts.size - 1 || i == MAX_MESSAGES - 1)
                return action
            else action.queue(::linkMessage, {})
        }

        throw IllegalStateException("Somehow iterated through all message parts " +
                                    "without returning final RestAction?!")
    }

    inline fun reply(block: () -> String) = reply(block())
    inline fun replySuccess(block: () -> String) = replySuccess(block())
    inline fun replyWarning(block: () -> String) = replyWarning(block())
    inline fun replyError(block: () -> String) = replyError(block())

    suspend inline fun send(block: () -> String) = send(block())
    suspend inline fun sendSuccess(block: () -> String) = sendSuccess(block())
    suspend inline fun sendWarning(block: () -> String) = sendWarning(block())
    suspend inline fun sendError(block: () -> String) = sendError(block())

    inline fun error(type: String, details: () -> String) = replyError("**$type!**\n${details()}")

    companion object {
        private fun checkForTalking(channel: MessageChannel) {
            check(channel !is TextChannel || channel.canTalk()) {
                "Cannot send a message to a TextChannel without being able to talk in it!"
            }
        }

        const val MAX_MESSAGES = 2

        const val BLOCKING_ERROR = "Help could not be sent to your DM because you are blocking me."

        private inline fun <reified T: Any> nonNullFromNonGuild(value: T?, name: String): T {
            return requireNotNull(value) { "Cannot get $name from a guild context!" }
        }

        private inline fun <reified T: Any> nonNullFromGuild(value: T?, name: String): T {
            return requireNotNull(value) { "Cannot get $name from a non-guild context!" }
        }

        fun processMessage(input: String): ArrayList<String> {
            val msgs = ArrayList<String>()
            var toSend = filterMassMentions(input)
            while(toSend.length > 2000) {
                val leeway = 2000 - (toSend.length % 2000)
                var index = toSend.lastIndexOf("\n", 2000)
                if(index < leeway)
                    index = toSend.lastIndexOf(" ", 2000)
                if(index < leeway)
                    index = 2000
                val temp = toSend.substring(0, index).trim()
                if(temp.isNotEmpty())
                    msgs.add(temp)
                toSend = toSend.substring(index).trim()
            }
            if(toSend.isNotEmpty())
                msgs.add(toSend)
            return msgs
        }
    }
}
