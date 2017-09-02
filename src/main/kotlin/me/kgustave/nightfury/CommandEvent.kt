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
@file:Suppress("unused", "HasPlatformType")
package me.kgustave.nightfury

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * @author Kaidan Gustave
 */
class CommandEvent internal constructor(val event: MessageReceivedEvent, args: String, val client: Client)
{
    val jda            = event.jda!!
    val author         = event.author!!
    val member         = event.member
    val guild          = event.guild
    val channel        = event.channel!!
    val textChannel    = event.textChannel
    val privateChannel = event.privateChannel
    val group          = event.group
    val channelType    = event.channelType!!
    val message        = event.message!!
    val messageId      = event.messageId!!
    val messageIdLong  = event.messageIdLong
    val responseNumber = event.responseNumber

    fun isFromType(type: ChannelType) = channelType == type

    var args: String = args
        internal set(value) {field = value}

    val isDev: Boolean = author.idLong == client.devId

    val selfMember: Member
        get() = guild.selfMember
    val selfUser: SelfUser = jda.selfUser

    val manager = client.manager

    fun reply(string: String) = sendMessage(string, channel)
    fun reply(string: String, success: (Message) -> Unit) = sendMessage(string, channel, success)
    fun reply(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) = sendMessage(string, channel, success, failure)

    fun reply(embed: MessageEmbed) = sendMessage(embed, channel)
    fun reply(embed: MessageEmbed, success: (Message) -> Unit) = sendMessage(embed, channel, success)
    fun reply(embed: MessageEmbed, success: (Message) -> Unit, failure: (Throwable) -> Unit) = sendMessage(embed, channel, success, failure)

    fun reply(message: Message) = sendMessage(message, channel)
    fun reply(message: Message, success: (Message) -> Unit) = sendMessage(message, channel, success)
    fun reply(message: Message, success: (Message) -> Unit, failure: (Throwable) -> Unit) = sendMessage(message, channel, success, failure)

    fun replyInDm(string: String) {
        author.openPrivateChannel().queue({ sendMessage(string, it) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }
    fun replyInDm(string: String, success: (Message) -> Unit) {
        author.openPrivateChannel().queue({ sendMessage(string, it, success) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }
    fun replyInDm(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        author.openPrivateChannel().queue({ sendMessage(string, it, success, failure) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }

    fun replyInDm(embed: MessageEmbed) {
        author.openPrivateChannel().queue({ sendMessage(embed, it) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }
    fun replyInDm(embed: MessageEmbed, success: (Message) -> Unit) {
        author.openPrivateChannel().queue({ sendMessage(embed, it, success) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }
    fun replyInDm(embed: MessageEmbed, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        author.openPrivateChannel().queue({ sendMessage(embed, it, success, failure) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }

    fun replyInDm(message: Message) {
        author.openPrivateChannel().queue({ sendMessage(message, it) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }
    fun replyInDm(message: Message, success: (Message) -> Unit) {
        author.openPrivateChannel().queue({ sendMessage(message, it, success) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }
    fun replyInDm(message: Message, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        author.openPrivateChannel().queue({ sendMessage(message, it, success, failure) },
                { sendMessage(blockingDM.format(client.warning), channel) })
    }

    fun replySuccess(string: String) = reply("${client.success} $string")
    fun replySuccess(string: String, success: (Message) -> Unit) = reply("${client.success} $string", success)
    fun replySuccess(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        reply("${client.success} $string", success, failure)
    }

    fun replyWarning(string: String) = reply("${client.warning} $string")
    fun replyWarning(string: String, success: (Message) -> Unit) = reply("${client.warning} $string", success)
    fun replyWarning(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        reply("${client.warning} $string", success, failure)
    }

    fun replyError(string: String) = reply("${client.error} $string")
    fun replyError(string: String, success: (Message) -> Unit) = reply("${client.error} $string", success)
    fun replyError(string: String, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        reply("${client.error} $string", success, failure)
    }

    fun sendMessage(string: String, channel: MessageChannel) {
        if(channel is TextChannel && !channel.canTalk())
            return
        val msgs = processMessage(string)
        if(msgs.size==1) channel.sendMessage(msgs[0]).queue({ linkMessage(it) }, {})
        else for(i in 0 until msgs.size){
            channel.sendMessage(msgs[i]).queue({ linkMessage(it) },{})

            if(i==maxMessages-1) break
        }
    }
    fun sendMessage(string: String, channel: MessageChannel, success: (Message) -> Unit) {
        if(channel is TextChannel && !channel.canTalk())
            return
        val msgs = processMessage(string)
        if(msgs.size==1) channel.sendMessage(msgs[0]).queue({ linkMessage(it); success(it) }, {})
        else for(i in 0 until msgs.size) {
            channel.sendMessage(msgs[i]).queue({ linkMessage(it); if(i == msgs.size-1 || i == maxMessages-1) success(it) },{})

            if(i == maxMessages-1) break
        }
    }
    fun sendMessage(string: String, channel: MessageChannel, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        if(channel is TextChannel && !channel.canTalk())
            return
        val msgs = processMessage(string)
        if(msgs.size==1) channel.sendMessage(msgs[0]).queue({ linkMessage(it); success(it) }, { failure(it) })
        else for(i in 0 until msgs.size) {
            channel.sendMessage(msgs[i]).queue({
                linkMessage(it)
                if(i == msgs.size-1 || i == maxMessages-1)
                    success(it)
            },{ if(i == msgs.size-1 || i == maxMessages-1) failure(it) })

            if(i == maxMessages-1) break
        }
    }

    fun sendMessage(embed: MessageEmbed, channel: MessageChannel) {
        if(channel is TextChannel && !channel.canTalk())
            return
        channel.sendMessage(embed).queue({ linkMessage(it) }, {})
    }
    fun sendMessage(embed: MessageEmbed, channel: MessageChannel, success: (Message) -> Unit) {
        if(channel is TextChannel && !channel.canTalk())
            return
        channel.sendMessage(embed).queue({ linkMessage(it); success(it) }, {})
    }
    fun sendMessage(embed: MessageEmbed, channel: MessageChannel, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        if(channel is TextChannel && !channel.canTalk())
            return
        channel.sendMessage(embed).queue({ linkMessage(it); success(it) }, { failure(it)})
    }

    fun sendMessage(message: Message, channel: MessageChannel) {
        if(channel is TextChannel && !channel.canTalk())
            return
        channel.sendMessage(message).queue({ linkMessage(it) }, {})
    }
    fun sendMessage(message: Message, channel: MessageChannel, success: (Message) -> Unit) {
        if(channel is TextChannel && !channel.canTalk())
            return
        channel.sendMessage(message).queue({ linkMessage(it); success(it) }, {})
    }
    fun sendMessage(message: Message, channel: MessageChannel, success: (Message) -> Unit, failure: (Throwable) -> Unit) {
        if(channel is TextChannel && !channel.canTalk())
            return
        channel.sendMessage(message).queue({ linkMessage(it); success(it) },{ failure(it)})
    }

    fun reactSuccess() = react(client.success)
    fun reactWarning() = react(client.warning)
    fun reactError() = react(client.error)
    fun react(string: String) {
        if(emoteRegex.matches(string)) {
            val emote = jda.getEmoteById(emoteRegex.replace(string, "$1"))
            if(emote!=null) addReaction(emote)
        }
        else addReaction(string)
    }
    fun addReaction(emote: String) {
        if(event.isFromType(ChannelType.TEXT)) {
            if(selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION))
                message.addReaction(emote).queue({},{})
        } else message.addReaction(emote).queue({},{})
    }
    fun addReaction(emote: Emote) {
        if(event.isFromType(ChannelType.TEXT)) {
            if(selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION))
                message.addReaction(emote).queue({},{})
        } else message.addReaction(emote).queue({},{})
    }

    fun linkMessage(message: Message) {
        if(message.isFromType(ChannelType.TEXT))
            client.linkIds(messageIdLong, message)
    }

    companion object
    {
        private val maxMessages = 3
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