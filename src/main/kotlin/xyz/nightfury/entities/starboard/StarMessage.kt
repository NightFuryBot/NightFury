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
package xyz.nightfury.entities.starboard

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.extensions.rebuild
import xyz.nightfury.extensions.send
import xyz.nightfury.resources.Emojis

/**
 * @author Kaidan Gustave
 */
data class StarMessage(val starboard: Starboard, val starred: Message) {
    val starReactions: MutableList<StarReaction>
        get() = Entries.getStars(this)

    var entryIsCreated: Boolean = false
    lateinit var entry: Message
        private set

    val count: Int
        get() = Entries.getStarCount(this)
    val starType: Emojis.Star
        get() = Emojis.Star.forCount(count)

    // Takes a user and adds them as a star to the message
    // If there is no star, we add it, otherwise we do nothing.
    fun addStar(user: User) {
        starReactions.firstOrNull { it.userId == user.idLong } ?: Entries.addStar(starred, user)

        if(entryIsCreated)
            updateEntry()
        else if(starboard.threshold <= count)
            createEntry()
    }

    fun createEntry() {
        val board = starboard.channel ?: return
        if(board.canTalk() && board.guild.selfMember.hasPermission(board, Permission.MESSAGE_EMBED_LINKS)) {
            board.send { generateStarEntry(this@StarMessage) } then {
                entry = it ?: return@then
                entryIsCreated = true
                Entries.setEntry(this)
            }
        }
    }

    // Updates the starboard entry.
    // This should be called when a new star is added or removed.
    private fun updateEntry() {
        if(!entryIsCreated) // entry isn't created yet
            return
        if(count == 0)
            return starboard.deletedMessage(starred.idLong) // delete if no reactions left
        if(!entry.textChannel.canTalk())
            return
        if(!entry.guild.selfMember.hasPermission(entry.textChannel, Permission.MESSAGE_EMBED_LINKS))
            return
        entry.rebuild { generateStarEntry(this@StarMessage) }
    }

    fun isStarring(user: User): Boolean = Entries.isStarring(starred, user)

    fun removeStar(user: User) {
        Entries.removeEntry(user, this)
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
        Entries.removeAllEntries(this)
    }

    override fun toString() = "${starType.emoji} **$count** <#${starred.channel.idLong}> (ID: ${starred.idLong})"
}