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
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLModeratorRole
import xyz.nightfury.extensions.doesNotMatch
import xyz.nightfury.extensions.isAdmin
import xyz.nightfury.music.MemberTrack
import xyz.nightfury.music.MusicManager

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a position in the queue to remove a track from!")
class RemoveCmd(musicManager: MusicManager): MusicCmd(musicManager) {

    init {
        this.name = "Remove"
        this.help = "Removes a song from the queue."
    }

    override fun execute(event: CommandEvent) {
        if(!musicManager.isPlaying(event.guild))
            return event.replyError("Currently not playing in a voice channel!")

        val queue = musicManager.getQueue(event.guild)
                    ?: return event.replyError("An unexpected error occurred!")

        val args = event.args
        if(args doesNotMatch Regex("\\d+"))
            return event.replyError(INVALID_ARGS_ERROR.format("Specify a position in the queue to remove a track from!"))

        val position = (
                args.toInt().takeIf { it > 0 && it <= queue.size }
                ?: return event.replyError("**Invalid position**\n" +
                                           "Track number must be between 0 and ${queue.size}!")
        ) - 1

        val atPosition = queue[position]

        val member = event.member

        val modRole = SQLModeratorRole.getRole(event.guild)

        // They are a moderator, admin, the server owner, or are the
        // member who queued up the track
        val track: MemberTrack = if((modRole != null && modRole in member.roles) or (member.isAdmin ||
                                                                                     member.isOwner ||
                                                                                     member == atPosition.member)) {

            queue.removeAt(position)
        } else {
            return event.replyError("The track position ${position + 1} cannot be removed " +
                                    "because you do not have permission to remove it.")
        }

        // They aren't able to remove the specified position
        event.replySuccess("Removed **${track.info.title}** at position ${position + 1}")
    }

}