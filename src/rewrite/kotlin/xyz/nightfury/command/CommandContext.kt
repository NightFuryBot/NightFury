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
@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
package xyz.nightfury.command

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import xyz.nightfury.Client
import xyz.nightfury.NightFury
import xyz.nightfury.entities.RestDeferred
import xyz.nightfury.util.emoteRegex
import xyz.nightfury.util.ext.modifyIf
import xyz.nightfury.util.filterMassMentions
import xyz.nightfury.util.requireState
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

    var isCooldownInvoked: Boolean = false

    fun send(text: String) = run {
        checkForTalking(channel)
        sendMessage(text, channel)
    }

    fun send(embed: MessageEmbed) = run {
        checkForTalking(channel)
        RestDeferred(channel.sendMessage(embed), this, success = this::linkMessage)
    }

    fun send(message: Message) = run {
        checkForTalking(channel)
        RestDeferred(channel.sendMessage(message), this, success = this::linkMessage)
    }

    fun reply(text: String) {
        checkForTalking(channel)
        replyMessage(text, channel)
    }

    fun reply(embed: MessageEmbed) {
        checkForTalking(channel)
        channel.sendMessage(embed).queue(this::linkMessage, {})
    }

    fun reply(message: Message) {
        checkForTalking(channel)
        channel.sendMessage(message).queue(this::linkMessage, {})
    }

    fun replyInDM(text: String) {
        if(channel is PrivateChannel) {
            return replyMessage(text, channel)
        }
        author.openPrivateChannel().queue({ pc ->
            replyMessage(text, pc)
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

    fun sendSuccess(text: String) = send("${NightFury.SUCCESS} $text")
    fun sendWarning(text: String) = send("${NightFury.WARNING} $text")
    fun sendError(text: String) = send("${NightFury.ERROR} $text")

    fun replySuccess(text: String) = reply("${NightFury.SUCCESS} $text")
    fun replyWarning(text: String) = reply("${NightFury.WARNING} $text")
    fun replyError(text: String) = reply("${NightFury.ERROR} $text")

    fun reactSuccess() = react(NightFury.SUCCESS)
    fun reactWarning() = react(NightFury.WARNING)
    fun reactError() = react(NightFury.ERROR)

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

    private fun sendMessage(text: String, channel: MessageChannel) = run {
        val parts = processMessage(text)
        if(parts.size == 1) {
            return@run RestDeferred(channel.sendMessage(parts[0]), this, success = this::linkMessage)
        }

        parts.forEachIndexed { i, s ->
            RestDeferred(channel.sendMessage(s), this, success = this::linkMessage).let {
                if(i == parts.size - 1 || i == MAX_MESSAGES - 1)
                    return@run it
            }
        }

        throw IllegalStateException("Somehow iterated through all message parts without returning final RestDeferred?!")
    }

    private fun replyMessage(text: String, channel: MessageChannel) {
        val parts = processMessage(text)
        if(parts.size == 1) {
            return channel.sendMessage(text).queue(this::linkMessage, {})
        }

        for((i, s) in parts.withIndex()) {
            channel.sendMessage(s).queue(this::linkMessage, {})
            if(i == MAX_MESSAGES - 1)
                break
        }
    }

    fun linkMessage(message: Message) = client.linkCall(messageIdLong, message)

    inline fun send(block: () -> String) = send(block())
    inline fun sendSuccess(block: () -> String) = sendSuccess(block())
    inline fun sendWarning(block: () -> String) = sendWarning(block())
    inline fun sendError(block: () -> String) = sendError(block())

    inline fun reply(block: () -> String) = reply(block())
    inline fun replySuccess(block: () -> String) = replySuccess(block())
    inline fun replyWarning(block: () -> String) = replyWarning(block())
    inline fun replyError(block: () -> String) = replyError(block())

    companion object {
        private fun checkForTalking(channel: MessageChannel) {
            requireState(channel !is TextChannel || channel.canTalk()) {
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
