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
package xyz.nightfury.command.music

import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.music.MusicManager
import xyz.nightfury.util.doesNotMatch

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class RemoveCommand(manager: MusicManager) : MusicCommand(manager) {
    companion object {
        private val NUMERIC_REGEX = Regex("\\d{1,5}")
    }

    override val name = "Remove"
    override val arguments = "[Queue Position]"
    override val help = "Removes a queued track."

    override suspend fun execute(ctx: CommandContext) {
        val member = ctx.member
        if(!ctx.isPlaying) return ctx.notPlaying()
        if(!member.isInPlayingChannel) return ctx.notInPlayingChannel()

        val queue = checkNotNull(manager[ctx.guild]) {
            "Expected non-null guild queue for Guild (ID: ${ctx.guild.idLong})"
        }

        val args = ctx.args

        if(args doesNotMatch NUMERIC_REGEX) return ctx.invalidArgs()

        val position = args.toInt().takeIf { 0 < it && it <= queue.size }?.minus(1) ?: return ctx.replyError {
            "**Invalid position**\n" +
            "Track number must be between 1 and ${queue.size}!"
        }

        val atPosition = queue[position]

        val track = if(Level.MODERATOR.test(ctx) || member == atPosition.member) {
            queue.removeAt(position)
        } else return ctx.replyError {
            "The track position ${position + 1} cannot be removed " +
            "because you do not have permission to remove it."
        }

        ctx.replySuccess("Removed **${track.info.title}** at position ${position + 1}")
    }
}
