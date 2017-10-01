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
package me.kgustave.nightfury.listeners

import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.extensions.embed
import me.kgustave.nightfury.extensions.formattedName
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageReaction
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.EventListener
import java.awt.Color
import java.time.OffsetDateTime
import java.util.*

/**
 * @author Kaidan Gustave
 */
class StarboardListener(val manager: DatabaseManager) : EventListener
{
    companion object
    {
        private val keyFormat = "U:%d|C:%d|M:%d"
    }

    private val map : MutableMap<String, StarMessage> = HashMap()

    override fun onEvent(event: Event?)
    {
        when(event)
        {
            is GuildMessageReactionAddEvent ->
            {
                // No bots
                if(event.user.isBot)
                    return
                // Guild requires a starboard
                if(!manager.hasStarboard(event.guild))
                    return
                if(event.reaction.isStarReaction)
                    addStarboardReaction(event)
            }

            is GuildMessageReactionRemoveEvent ->
            {
                // Guild requires a starboard
                if(!manager.hasStarboard(event.guild))
                    return
                if(event.reaction.isStarReaction)
                    removeStarboardReaction(event)
            }
        }
    }

    private fun addStarboardReaction(event: GuildMessageReactionAddEvent)
    {
        val key = keyFormat.format(event.user.idLong, event.channel.idLong, event.messageIdLong)

        synchronized(map)
        {
            // No key
            if(!map.contains(key))
            {
                val starboard = manager.getStarboard(event.guild)?:return

                if(!starboard.canTalk() || !event.guild.selfMember
                        .hasPermission(starboard, Permission.MESSAGE_EMBED_LINKS))
                    return

                event.channel.getMessageById(event.messageIdLong).queue({ starred ->

                    val builder = MessageBuilder()
                    builder.append { "\u2B50 **1** ${event.channel.asMention} (ID: ${starred.idLong})" }

                    builder.embed {
                        author {
                            icon = starred.author.effectiveAvatarUrl
                            value = starred.author.formattedName(false)
                        }
                        if(starred.rawContent.isNotEmpty())
                            description { starred.rawContent }
                        if(starred.attachments.isNotEmpty())
                            image { starred.attachments[0].url }
                        if(starred.embeds.isNotEmpty())
                            image { starred.embeds[0].url }
                        color { Color.YELLOW }
                        time { starred.creationTime }
                    }

                    starboard.sendMessage(builder.build()).queue {
                        map.put(key, StarMessage(event.messageIdLong, event.channel.idLong, it, 1))
                    }
                })
            }
            else
            {
                var starMessage = map[key]?:return

                if(starMessage.isExpired)
                {
                    map.remove(key)
                    return
                }

                starMessage++
                map.put(key, starMessage)
            }
        }
    }

    private fun removeStarboardReaction(event: GuildMessageReactionRemoveEvent)
    {
        val key = keyFormat.format(event.user.idLong, event.channel.idLong, event.messageIdLong)
        synchronized(map)
        {
            var starMessage = map[key]

            if(starMessage != null)
            {
                if(!check(starMessage, key))
                    return
                starMessage--
                map.put(key, starMessage)
            }
        }
    }

    private inline val MessageReaction.isStarReaction: Boolean
        inline get() = when(emote.name)
        {
            "\u2B50" -> true
            "\uD83C\uDF1F" -> true
            "\uD83D\uDCAB" -> true
            else -> emote.name.contains("star", true)
        }

    private fun check(starMessage: StarMessage, key: String) = synchronized(map)
    {
        when
        {
            starMessage.isExpired -> {
                map.remove(key)
                false
            }

            !starMessage.boardMessage.textChannel.canTalk() -> {
                map.remove(key)
                false
            }

            !starMessage.boardMessage.guild.selfMember.hasPermission(starMessage.boardMessage.textChannel,
                    Permission.MESSAGE_EMBED_LINKS) -> {
                map.remove(key)
                false
            }
            else -> true
        }
    }

    private class StarMessage(
            val messageId: Long, val channelId: Long,
            val boardMessage: Message, var count: Int,
            val timeStarted: OffsetDateTime = OffsetDateTime.now(),
            val endTime: OffsetDateTime = timeStarted.plusMinutes(10))
    {
        companion object
        {
            private val builder: MessageBuilder = MessageBuilder()
        }

        val isExpired: Boolean
            get() = OffsetDateTime.now().isAfter(endTime) || count == 0

        val starType: String
            get() = when
            {
                count == 0 -> "<:nostar:267793455311224843>"
                count < 6 -> "\u2B50"
                count < 11 -> "\uD83C\uDF1F"
                count < 21 -> "\uD83D\uDCAB"
                else -> "\u2728"
            }

        private fun updateMessage()
        {
            if(!boardMessage.textChannel.canTalk() || !boardMessage.guild.selfMember
                    .hasPermission(boardMessage.textChannel, Permission.MESSAGE_EMBED_LINKS))
                return
            if(count == 0)
                return boardMessage.delete().queue()
            builder.clear()
            builder.append { "$starType **$count** <#$channelId> (ID: $messageId)" }
            val embed = boardMessage.embeds[0]
            builder.embed {
                author {
                    icon = embed.author.iconUrl
                    value = embed.author.name
                }
                description { embed.description }
                image { embed.image.url }
                color { starColor(count) }
                time { embed.timestamp }
            }
            boardMessage.editMessage(builder.build()).queue()
        }

        operator fun inc(): StarMessage
        {
            if(!isExpired)
            {
                count += 1
                updateMessage()
            }
            return this
        }

        operator fun dec(): StarMessage
        {
            if(!isExpired)
            {
                count -= 1
                updateMessage()
            }
            return this
        }

        override fun equals(other: Any?) = when(other)
        {
            is Message -> other.idLong == messageId
            is StarMessage -> other.messageId == messageId && other.channelId == channelId
                    && other.boardMessage == boardMessage
            else -> false
        }

        override fun hashCode(): Int {
            var result = messageId.hashCode()
            result = 31 * result + channelId.hashCode()
            result = 31 * result + boardMessage.hashCode()
            return result
        }
    }
}

fun starColor(stars: Int): Color
{
    if(stars>=10)
        return Color(255, 255, 0)
    return Color(255, 255, (25.44*(10 - stars)).toInt())
}