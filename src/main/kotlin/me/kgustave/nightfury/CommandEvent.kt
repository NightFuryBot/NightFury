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
package me.kgustave.nightfury

import me.kgustave.nightfury.db.DatabaseManager
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.function.Consumer

/**
 * @author Kaidan Gustave
 */
@Suppress("unused")
class CommandEvent internal constructor(val event: MessageReceivedEvent, args: String, val client: Client, val prefixUsed: String)
{
    val jda = event.jda!!
    val author = event.author!!
    val member = event.member
    val guild = event.guild
    val channel = event.channel!!
    val textChannel = event.textChannel
    val privateChannel = event.privateChannel
    val group = event.group
    val channelType = event.channelType!!
    val message = event.message!!
    val messageId = event.messageId!!
    val messageIdLong = event.messageIdLong
    val responseNumber = event.responseNumber

    fun isFromType(type: ChannelType) = channelType == type

    var args: String = args
        internal set(value) {field = value}
    val isDev: Boolean = author.idLong == client.devId
    val selfMember: Member
        get() = guild.selfMember
    val selfUser: SelfUser = jda.selfUser

    val manager: DatabaseManager
        get() {return client.manager}

    fun reply(string: String) = sendMessage(string, channel)
    fun reply(string: String, success: (Message) -> Unit) = reply(string, Consumer(success))
    fun reply(string: String, success: Consumer<Message>) = sendMessage(string, channel, success)
    fun reply(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        reply(string, Consumer(success), Consumer(failure))
    }
    fun reply(string: String, success: Consumer<Message>, failure: Consumer<Throwable>) {
        sendMessage(string, channel, success, failure)
    }

    fun reply(embed: MessageEmbed) = sendMessage(embed, channel)
    fun reply(embed: MessageEmbed, success: (Message) -> Unit) = reply(embed, Consumer(success))
    fun reply(embed: MessageEmbed, success: Consumer<Message>) = sendMessage(embed, channel, success)
    fun reply(embed: MessageEmbed, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        reply(embed, Consumer(success), Consumer(failure))
    }
    fun reply(embed: MessageEmbed, success: Consumer<Message>, failure: Consumer<Throwable>) {
        sendMessage(embed, channel, success, failure)
    }

    fun reply(message: Message) = sendMessage(message, channel)
    fun reply(message: Message, success: (Message) -> Unit) = reply(message, Consumer(success))
    fun reply(message: Message, success: Consumer<Message>) = sendMessage(message, channel, success)
    fun reply(message: Message, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        reply(message, Consumer(success), Consumer(failure))
    }
    fun reply(message: Message, success: Consumer<Message>, failure: Consumer<Throwable>) {
        sendMessage(message, channel, success, failure)
    }

    fun replyInDm(string: String) {
        author.openPrivateChannel().queue({ sendMessage(string, it) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }
    fun replyInDm(string: String, success: (Message) -> Unit) {
        replyInDm(string, Consumer(success))
    }
    fun replyInDm(string: String, success: Consumer<Message>) {
        author.openPrivateChannel().queue({ sendMessage(string, it, success) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }
    fun replyInDm(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        replyInDm(string, Consumer(success), Consumer(failure))
    }
    fun replyInDm(string: String, success: Consumer<Message>, failure: Consumer<Throwable>) {
        author.openPrivateChannel().queue({ sendMessage(string, it, success, failure) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }

    fun replyInDm(embed: MessageEmbed) {
        author.openPrivateChannel().queue({ sendMessage(embed, it) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }
    fun replyInDm(embed: MessageEmbed, success: (Message) -> Unit) {
        replyInDm(embed, Consumer(success))
    }
    fun replyInDm(embed: MessageEmbed, success: Consumer<Message>) {
        author.openPrivateChannel().queue({ sendMessage(embed, it, success) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }
    fun replyInDm(embed: MessageEmbed, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        replyInDm(embed, Consumer(success), Consumer(failure))
    }
    fun replyInDm(embed: MessageEmbed, success: Consumer<Message>, failure: Consumer<Throwable>) {
        author.openPrivateChannel().queue({ sendMessage(embed, it, success, failure) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }

    fun replyInDm(message: Message) {
        author.openPrivateChannel().queue({ sendMessage(message, it) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }
    fun replyInDm(message: Message, success: (Message) -> Unit) {
        replyInDm(message, Consumer(success))
    }
    fun replyInDm(message: Message, success: Consumer<Message>) {
        author.openPrivateChannel().queue({ sendMessage(message, it, success) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }
    fun replyInDm(message: Message, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        replyInDm(message, Consumer(success), Consumer(failure))
    }
    fun replyInDm(message: Message, success: Consumer<Message>, failure: Consumer<Throwable>) {
        author.openPrivateChannel().queue({ sendMessage(message, it, success, failure) },
                { channel.sendMessage(blockingDM.format(client.warning)).queue() })
    }

    fun replySuccess(string: String) = reply("${client.success} $string")
    fun replySuccess(string: String, success: (Message) -> Unit) = replySuccess(string, Consumer(success))
    fun replySuccess(string: String, success: Consumer<Message>) = reply("${client.success} $string", success)
    fun replySuccess(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        replySuccess(string, Consumer(success), Consumer(failure))
    }
    fun replySuccess(string: String, success: Consumer<Message>, failure: Consumer<Throwable>) {
        reply("${client.success} $string", success, failure)
    }

    fun replyWarning(string: String) = reply("${client.warning} $string")
    fun replyWarning(string: String, success: (Message) -> Unit) = replyWarning(string, Consumer(success))
    fun replyWarning(string: String, success: Consumer<Message>) = reply("${client.warning} $string", success)
    fun replyWarning(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        replyWarning(string, Consumer(success), Consumer(failure))
    }
    fun replyWarning(string: String, success: Consumer<Message>, failure: Consumer<Throwable>) {
        reply("${client.warning} $string", success, failure)
    }

    fun replyError(string: String) = reply("${client.error} $string")
    fun replyError(string: String, success: (Message) -> Unit) = replyError(string, Consumer(success))
    fun replyError(string: String, success: Consumer<Message>) = reply("${client.error} $string", success)
    fun replyError(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        replyError(string, Consumer(success), Consumer(failure))
    }
    fun replyError(string: String, success: Consumer<Message>, failure: Consumer<Throwable>) {
        reply("${client.error} $string", success, failure)
    }

    fun sendMessage(string: String, channel: MessageChannel) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            val msgs = processMessage(string)
            if(msgs.size==1) {
                channel.sendMessage(msgs[0]).queue({ linkMessage(it) }, {})
            } else {
                msgs.forEach { msg -> channel.sendMessage(msg).queue({ linkMessage(it) }, {}) }
            }
        }
    }
    fun sendMessage(string: String, channel: MessageChannel, success: Consumer<Message>) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            val msgs = processMessage(string)
            if(msgs.size==1) {
                channel.sendMessage(msgs[0]).queue({ linkMessage(it); success.accept(it) }, {})
            } else {
                msgs.forEachIndexed { index, msg ->
                    if(index + 1 == msgs.size)
                        channel.sendMessage(msg).queue({ linkMessage(it); success.accept(it) }, {})
                    else
                        channel.sendMessage(msg).queue({ linkMessage(it) }, {})
                }
            }
        }
    }
    fun sendMessage(string: String, channel: MessageChannel, success: Consumer<Message>, failure: Consumer<Throwable>) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            val msgs = processMessage(string)
            if(msgs.size==1) {
                channel.sendMessage(msgs[0]).queue(
                        { linkMessage(it); success.accept(it) }, { failure.accept(it) })
            } else {
                msgs.forEachIndexed { index, msg ->
                    if(index + 1 == msgs.size)
                        channel.sendMessage(msg).queue(
                                { linkMessage(it); success.accept(it) }, { failure.accept(it) })
                    else
                        channel.sendMessage(msg).queue({ linkMessage(it) }, {})
                }
            }
        }
    }

    fun sendMessage(embed: MessageEmbed, channel: MessageChannel) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            channel.sendMessage(embed).queue({ linkMessage(it) }, {})
        }
    }
    fun sendMessage(embed: MessageEmbed, channel: MessageChannel, success: Consumer<Message>) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            channel.sendMessage(embed).queue({ linkMessage(it); success.accept(it) }, {})
        }
    }
    fun sendMessage(embed: MessageEmbed, channel: MessageChannel, success: Consumer<Message>, failure: Consumer<Throwable>) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            channel.sendMessage(embed).queue(
                    { linkMessage(it); success.accept(it) },
                    { failure.accept(it)}
            )
        }
    }

    fun sendMessage(message: Message, channel: MessageChannel) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            channel.sendMessage(message).queue({ linkMessage(it) }, {})
        }
    }
    fun sendMessage(message: Message, channel: MessageChannel, success: Consumer<Message>) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            channel.sendMessage(message).queue({ linkMessage(it); success.accept(it) }, {})
        }
    }
    fun sendMessage(message: Message, channel: MessageChannel, success: Consumer<Message>, failure: Consumer<Throwable>) {
        if(channel is TextChannel && !channel.canTalk())
            return
        else {
            channel.sendMessage(message).queue(
                    { linkMessage(it); success.accept(it) },{ failure.accept(it)})
        }
    }

    fun reactSuccess() = react(client.success)
    fun reactWarning() = react(client.warning)
    fun reactError() = react(client.error)
    fun react(string: String) {
        if(string.matches(emoteRegex)) {
            val emote = jda.getEmoteById(string.replace(emoteRegex, "$1"))
            if(emote!=null) addReaction(emote)
        }
        else addReaction(string)
    }
    fun addReaction(emote: String) {
        if(event.isFromType(ChannelType.TEXT)) {
            if(selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION))
                message.addReaction(emote).queue()
        } else message.addReaction(emote).queue()
    }
    fun addReaction(emote: Emote) {
        if(event.isFromType(ChannelType.TEXT)) {
            if(selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION))
                message.addReaction(emote).queue()
        } else message.addReaction(emote).queue()
    }

    fun linkMessage(message: Message) = client.linkIds(messageIdLong, message)

    companion object
    {
        private val blockingDM : String = "%s I could not complete the command because you are blocking direct messages!"

        private val emoteRegex = Regex("<:\\S{2,32}:(\\d+)>")

        fun processMessage(input: String) : ArrayList<String>
        {
            val msgs = ArrayList<String>()
            var toSend = input.replace("@everyone", "@\u0435veryone")
                    .replace("@here", "@h\u0435re").trim()
            while(toSend.length > 2000)
            {
                val leeway = 2000 - (toSend.length % 2000)
                var index = toSend.lastIndexOf("\n", 2000)
                if(index < leeway)
                    index = toSend.lastIndexOf(" ", 2000)
                if(index < leeway)
                    index = 2000
                val temp = toSend.substring(0, index).trim()
                if(temp != "")
                    msgs.add(temp)
                toSend = toSend.substring(index).trim()
            }
            if(toSend != "")
                msgs.add(toSend)
            return msgs
        }
    }
}