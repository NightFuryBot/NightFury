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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.util.ext.edit
import xyz.nightfury.util.ext.formattedInfo
import xyz.nightfury.util.ext.noMatch
import xyz.nightfury.music.MemberTrack
import xyz.nightfury.music.MusicManager
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.VoiceChannel

/**
 * @author Kaidan Gustave
 */
abstract class MusicCmd(protected val musicManager: MusicManager): Command() {
    init {
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.VOICE_SPEAK, Permission.VOICE_CONNECT)
        this.category = Category.MUSIC
    }

    // Extensions for commands
    protected val Guild.isPlaying: Boolean
        get() = musicManager.isPlaying(this)

    protected val Member.voiceChannel: VoiceChannel?
        get() = voiceState.channel

    protected val Member.isInProperVoice: Boolean
        get() = musicManager.getQueue(guild)?.run {
            !isDead || this@isInProperVoice.voiceState.channel?.equals(this.voiceChannel) ?: true
        } ?: true

    inner class SearchResultHandler(
        val event: CommandEvent, val message: Message, val query: String, var ytSearch: Boolean = false
    ): AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack)
        {
            val position = event.member.voiceState.channel?.let {
                musicManager.addTrack(it, MemberTrack(event.member, track))
            }!! // TODO Find a safer way

            showLoaded(track, position)
        }

        override fun noMatches()
        {
            if(ytSearch)
                message.edit { "${event.client.warning} ${noMatch("results", query)}" }
            else
                rerun("ytsearch:$query")
        }

        override fun loadFailed(exception: FriendlyException)
        {
            message.edit {
                "${event.client.error} An error occurred${if(exception.severity == FriendlyException.Severity.COMMON)
                    ": ${exception.localizedMessage}" else "."}"
            }
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if(playlist.tracks.size == 1 || playlist.isSearchResult || playlist.selectedTrack != null)
            {
                trackLoaded(playlist.selectedTrack ?: playlist.tracks[0])
            }
            else
            {
                event.member.voiceState.channel?.let {
                    musicManager.addTracks(it, playlist.tracks.map { MemberTrack(event.member, it) })
                }
                showLoaded(playlist, musicManager.getQueue(event.guild)!!.size + 1 == playlist.tracks.size)
            }
        }

        fun showLoaded(track: AudioTrack, position: Int)
        {
            message.edit {
                if(position < 1)
                    "${event.client.success} Now playing ${track.info.formattedInfo}."
                else
                    "${event.client.success} Added ${track.info.formattedInfo} at position $position in the queue."
            }
        }

        fun showLoaded(playlist: AudioPlaylist, fromEmptyQueue: Boolean)
        {
            message.edit {
                if(fromEmptyQueue)
                    "${event.client.success} Now playing `${playlist.tracks.size}` tracks from playlist **${playlist.name}**."
                else
                    "${event.client.success} Added `${playlist.tracks.size}` tracks from **${playlist.name}**."
            }
        }

        @Suppress("HasPlatformType")
        fun rerun(newQuery: String = query) = musicManager.loadItemOrdered(event.guild, newQuery, apply { ytSearch = true })
    }
}
