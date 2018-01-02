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
package xyz.nightfury.commands.music

import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.extensions.formattedInfo
import xyz.nightfury.extensions.formattedName
import xyz.nightfury.music.MusicManager

/**
 * @author Kaidan Gustave
 */
class SkipCmd(musicManager: MusicManager) : MusicCmd(musicManager)
{
    init {
        this.name = "Skip"
        this.help = "Votes to skip a song"
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
    }

    override fun execute(event: CommandEvent) {
        if(!event.member.isInProperVoice)
            return event.replyError("I am already in a voice channel!")
        val queue = musicManager.getQueue(event.guild)
            ?: return event.replyError("An unexpected error occurred!")

        if(queue.currentTrack.member == event.member)
        {
            val skippedTrack = queue.skip()
            return event.replySuccess("Skipped ${skippedTrack.info.formattedInfo}")
        }

        val totalToSkip = queue.totalToSkip
        if(totalToSkip == 1)
        {
            val skippedTrack = queue.skip()
            return event.replySuccess("Skipped ${skippedTrack.info.formattedInfo} " +
                    "(Queued by: ${skippedTrack.member.user.formattedName(true)})")
        }

        if(queue.isSkipping(event.member))
            return event.replyError("You have already voted to skip this song!")

        val skips = queue.voteToSkip(event.member)

        if(totalToSkip == skips)
        {
            val skippedTrack = queue.skip()
            event.replySuccess("Skipped ${skippedTrack.info.formattedInfo} " +
                    "(Queued by: ${skippedTrack.member.user.formattedName(true)})")
        }
        else
        {
            event.replySuccess("Voted to skip ${queue.currentTrack.info.formattedInfo} " +
                    "(`$skips/$totalToSkip` votes, `${totalToSkip - skips}` more needed to skip)")
        }
    }
}