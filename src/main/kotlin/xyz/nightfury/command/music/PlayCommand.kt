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

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.experimental.async
import xyz.nightfury.NightFury
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.music.MemberTrack
import xyz.nightfury.music.MusicManager
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.jda.editMessage
import xyz.nightfury.util.formattedInfo

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class PlayCommand(manager: MusicManager): MusicCommand(manager) {
    override val name = "Play"
    override val arguments = "[Query]"
    override val help = "Plays music a voice channel."

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        val member = ctx.member
        val voiceChannel = member.voiceChannel ?: return ctx.notInVoiceChannel()

        if(ctx.guild.isPlaying && !member.isInPlayingChannel) return ctx.notInPlayingChannel()

        val loading = async(ctx) {
            ctx.send("Loading...")
        }

        val item = try {
            loadTrack(member, query)
        } catch(e: FriendlyException) {
            val message = loading.await()
            return when(e.severity) {
                COMMON -> message.editMessage("An error occurred${e.message?.let { ": $it" } ?: ""}.").queue()
                else -> message.editMessage("An error occurred.").queue()
            }
        }

        when(item) {
            null -> return ctx.replyWarning(xyz.nightfury.util.noMatch("results", query))

            is AudioTrack -> {
                val info = item.info.formattedInfo
                val position = manager.addTrack(voiceChannel, MemberTrack(member, item))
                loading.await().editMessage {
                    append(NightFury.SUCCESS)
                    if(position < 1) {
                        append(" Now playing $info.")
                    } else {
                        append(" Added $info at position $position in the queue.")
                    }
                }.await()
            }

            is AudioPlaylist -> {
                val tracks = item.tracks
                manager.addTracks(voiceChannel, tracks.map { MemberTrack(member, it) })
                loading.await().editMessage {
                    append(NightFury.SUCCESS)
                    if(manager[ctx.guild]!!.size + 1 == tracks.size) {
                        append(" Now playing `${tracks.size}` tracks from playlist **${item.name}**.")
                    } else {
                        append(" Added `${tracks.size}` tracks from **${item.name}**.")
                    }
                }.await()
            }

            // This shouldn't happen, but...
            else -> loading.await().editMessage("The loaded item is unsupported by this player.").queue()
        }
    }
}
