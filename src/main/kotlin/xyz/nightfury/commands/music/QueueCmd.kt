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
package xyz.nightfury.commands.music

import com.jagrosh.jdautilities.menu.Paginator
import com.jagrosh.jdautilities.waiter.EventWaiter
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.extensions.*
import xyz.nightfury.music.MusicManager
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
class QueueCmd(waiter: EventWaiter, musicManager: MusicManager): MusicCmd(musicManager)
{
    val pBuilder: Paginator.Builder = Paginator.Builder()
            .waitOnSinglePage { false }
            .setFinalAction   { it.clearReactions().queue({},{}) }
            .showPageNumbers  { true }
            .itemsPerPage     { 8 }
            .waiter           { waiter }

    init {
        this.name = "Queue"
        this.help = "Shows the full ordered queue of songs."
        this.cooldown = 30
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = botPermissions + arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        if(!musicManager.isPlaying(event.guild))
            return event.replyError("Currently not playing in a voice channel!")
        val queue = musicManager.getQueue(event.guild)
                ?: return event.replyError("An unexpected error occurred!")

        if(queue.isEmpty())
            return event.reply("There are no currently queued tracks!")

        pBuilder.clearItems()
        pBuilder.add {
            val currTrack = queue.currentTrack
            "`->` [${currTrack.member.asMention}] - ${currTrack.info.formattedInfo}"
        }
        queue.forEachIndexed { index, track ->
            pBuilder.add { "`${index+1}` [${track.member.asMention}] - ${track.info.formattedInfo}" }
        }
        pBuilder.color { event.selfMember.color }
        pBuilder.user { event.author }
        pBuilder.displayIn { event.channel }
    }

    // TODO Personal paginator implementation after JDA-Utilities 2.0
}