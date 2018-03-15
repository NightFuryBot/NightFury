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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.nightfury.entities.starboard

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.util.jda.embed
import xyz.nightfury.util.Emojis
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.db.starCount
import xyz.nightfury.util.db.stars
import xyz.nightfury.util.formattedName
import xyz.nightfury.util.jda.message
import java.awt.Color
import xyz.nightfury.ndb.starboard.StarboardEntriesHandler as StarEntries

/**
 * @author Kaidan Gustave
 */
data class StarMessage constructor(val starboard: Starboard, val starred: Message) {
    companion object {
        private val msgBuilder = MessageBuilder()
    }

    lateinit var entry: Message
        private set

    val guild: Guild get() = starboard.guild

    val entryIsCreated: Boolean get() = ::entry.isInitialized
    val starReactions: List<Star> get() = starred.stars
    val count: Int get() = starred.starCount
    val starType: Emojis.Star get() = Emojis.Star.forCount(count)

    // Takes a user and adds them as a star to the message
    // If there is no star, we add it, otherwise we do nothing.
    suspend fun addStar(user: User) {
        starReactions.firstOrNull { it.userId == user.idLong }
        ?: StarEntries.addStar(starred.idLong, starred.channel.idLong, user.idLong)

        if(entryIsCreated)
            updateEntry()
        else if(starboard.settings.threshold <= count)
            createEntry()
    }

    suspend fun createEntry() {
        val board = starboard.channel ?: return
        if(board.canTalk() && guild.selfMember.hasPermission(board, Permission.MESSAGE_EMBED_LINKS)) {
            msgBuilder.clear()
            val msg = message(msgBuilder) {
                append { this@StarMessage.toString() }
                setEmbed(createEmbed())
            }
            val entry = board.sendMessage(msg).await()
            this.entry = entry
            StarEntries.setEntry(entry.idLong, starred.idLong, guild.idLong)
        }
    }

    // Updates the starboard entry.
    // This should be called when a new star is added or removed.
    fun updateEntry() {
        if(!entryIsCreated) // entry isn't created yet
            return
        if(count == 0)
            return starboard.deletedMessage(starred.idLong) // delete if no reactions left
        if(!entry.textChannel.canTalk())
            return
        if(!entry.guild.selfMember.hasPermission(entry.textChannel, Permission.MESSAGE_EMBED_LINKS))
            return
        msgBuilder.clear()
        val msg = message(msgBuilder) {
            append { this@StarMessage.toString() }
            setEmbed(createEmbed())
        }
        entry.editMessage(msg).queue()
    }

    fun isStarring(user: User): Boolean {
        return StarEntries.isStarring(starred.idLong, user.idLong, guild.idLong)
    }

    fun removeStar(user: User) {
        StarEntries.removeEntry(user.idLong, starred.idLong, guild.idLong)
        // Cleanup if it hits zero
        if(count <= 0) {
            // We go back up to the starboard first to remove it from the map.
            starboard.deletedMessage(starred.idLong)
        } else {
            updateEntry()
        }
    }

    fun delete() {
        if(entryIsCreated)
            entry.delete().queue()
        StarEntries.removeAllEntries(starred.idLong, guild.idLong)
    }

    override fun toString(): String {
        return "${starType.emoji} **$count** <#${starred.channel.idLong}> (ID: ${starred.idLong})"
    }

    private fun createEmbed() = embed {
        author {
            icon = starred.author.effectiveAvatarUrl
            value = starred.author.formattedName(false)
        }
        if(starred.contentRaw.isNotEmpty())
            + starred.contentRaw
        if(starred.attachments.isNotEmpty())
            starred.attachments[0].takeIf { it.isImage }?.let { image { it.url } }
        // Image embeds take precedence over attachments
        if(starred.embeds.isNotEmpty()) image { starred.embeds[0].url }
        color { Color(255, 255, (25.44 * Math.min(count, 10)).toInt()) }
        time { starred.creationTime }
    }
}
