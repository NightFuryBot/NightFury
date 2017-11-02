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
@file:Suppress("Unused")
package xyz.nightfury.entities

import xyz.nightfury.extensions.embed
import xyz.nightfury.extensions.formattedName
import xyz.nightfury.db.Database
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color

class StarboardListener(private val manager: Database): EventListener
{
    val starboards: MutableMap<Long, Starboard> = HashMap()

    fun addStarboard(textChannel: TextChannel) {
        synchronized(starboards) {
            starboards.put(textChannel.guild.idLong, Starboard(textChannel, manager))
        }
    }

    fun removeStarboard(guild: Guild) {
        synchronized(starboards) {
            starboards.remove(guild.idLong)
        }
    }

    fun getStarboard(guild: Guild) = synchronized(starboards) { starboards[guild.idLong] }

    override fun onEvent(event: Event?) {}
}

class Starboard(val channel: TextChannel, private val manager: Database)
{
    private val map: MutableMap<Long, StarMessage> = HashMap()

    fun addStar(user: User, starred: Message, messageReaction: MessageReaction) {
        // Message is from starboard, so we don't do anything
        if(starred.channel == channel)
            return
        synchronized(map) {
            // If one isn't present we'll create one.
            if(map.contains(starred.idLong))
                return createStarboardEntry(user, starred, messageReaction)

            val starMessage = map[starred.idLong] ?: return // TODO
            starMessage.addStar(user, messageReaction)
        }
    }

    fun removeStar(user: User, messageId: Long, userRemovedSelf: Boolean = true) {
        synchronized(map) {
            val starMessage = map[messageId] ?: return
            starMessage.removeStar(user, userRemovedSelf)
        }
    }

    // We pass the user and the reaction because we'll be recursing back up.
    fun createStarboardEntry(user: User, starred: Message, messageReaction: MessageReaction) {
        channel.sendMessage(MessageBuilder().append {
            "\u2B50 **1** <#${starred.channel.idLong}> (ID: ${starred.idLong})"
        }.buildAsStarEntry(starred)) then { this ?: return@then
            synchronized(map) {
                map.put(starred.idLong, StarMessage(this@Starboard,starred, this))
                // Only recurse if this has been fully successful and has finished.
                addStar(user, starred, messageReaction)
            }
        }
    }

    fun deletedMessage(messageId: Long) {
        map.remove(messageId)?.delete()?.queue()
    }

    companion object {
        internal val LOG: Logger = LoggerFactory.getLogger(Starboard::class.java)
    }
}

class StarMessage(val starboard: Starboard, val starred: Message, val starboardEntry: Message): Message by starred
{
    val starReactions: MutableList<StarReaction> = ArrayList()

    val count: Int
        get() = starReactions.size

    val starType: String
        get() = when {
            count < 6 -> "\u2B50"
            count < 11 -> "\uD83C\uDF1F"
            count < 21 -> "\uD83D\uDCAB"
            else -> "\u2728"
        }

    init {
        starred.reactions.forEach { reaction ->
            reaction.users.queue(
                { it.forEach { user -> addStar(user, reaction) } },
                { Starboard.LOG.error("An error occurred while creating a StarMessage", it) }
            )
        }
    }

    fun addStar(user: User, reaction: MessageReaction) {
        // If there is no star, we add it, otherwise we do nothing.
        starReactions.firstOrNull { it.user == user }
        ?: starReactions.add(StarReaction(user, reaction, this))
    }

    private fun updateMessage() {
        if(!starboardEntry.textChannel.canTalk() || !starboardEntry.guild.selfMember
            .hasPermission(starboardEntry.textChannel, Permission.MESSAGE_EMBED_LINKS))
            return

        starboardEntry.editMessage(MessageBuilder().buildAsStarEntry(this)).queue()
    }

    fun isStarring(user: User): Boolean = starReactions.any { it.user == user }

    fun removeStar(user: User, userRemovedSelf: Boolean) {
        if(!userRemovedSelf) {
            starReactions.firstOrNull { it.user == user }
                ?.reaction?.removeReaction(user)?.queue({}, {})
        }
        starReactions.removeIf { it.user == user }
    }

    fun deleted() {
        starboardEntry.delete().queue()
    }

    override fun delete(): AuditableRestAction<Void> {
        starboardEntry.delete().queue({},{})
        return starred.delete()
    }

    override fun toString() = "$starType **$count** <#${channel.idLong}> (ID: $idLong)"
}

data class StarReaction(val user: User, val reaction: MessageReaction, val message: StarMessage)

fun starColor(stars: Int): Color {
    if(stars>=10)
        return Color(255, 255, 0)
    return Color(255, 255, (25.44*(10 - stars)).toInt())
}

inline val MessageReaction.isStarReaction: Boolean
    inline get() = when(emote.name) {
        "\u2B50" -> true
        "\uD83C\uDF1F" -> true
        "\uD83D\uDCAB" -> true
        else -> emote.name.contains("star", true)
    }

fun MessageBuilder.buildAsStarEntry(message: StarMessage): Message {
    append { message.toString() }
    return buildAsStarEntry(message.starred)
}

fun MessageBuilder.buildAsStarEntry(message: Message): Message {
    embed {
        author {
            icon = message.author.effectiveAvatarUrl
            value = message.author.formattedName(false)
        }
        if(message.rawContent.isNotEmpty())
            description { message.rawContent }
        if(message.attachments.isNotEmpty())
            image { message.attachments[0].url }
        if(message.embeds.isNotEmpty())
            image { message.embeds[0].url }
        color { Color.YELLOW }
        time { message.creationTime }
    }
    return build()
}