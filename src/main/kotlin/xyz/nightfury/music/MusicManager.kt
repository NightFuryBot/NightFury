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
package xyz.nightfury.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.*
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceSuppressEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.nightfury.extensions.createLogger
import xyz.nightfury.extensions.formatTrackTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author Kaidan Gustave
 */
class MusicManager : AudioEventListener, EventListener, AudioPlayerManager by DefaultAudioPlayerManager() {
    companion object {
        internal val LOG = createLogger(MusicManager::class)

        internal val threadpool: ExecutorService by lazy {
            Executors.newSingleThreadExecutor {
                Thread(it).apply {
                    name = "AudioCloseThread"
                    isDaemon = true
                }
            }
        }

        private fun logTrackInfo(track: AudioTrack) =
                "Title: ${track.info.title} | Length: ${formatTrackTime(track.duration)} | State: ${track.state}"
    }

    private val queueMap: MutableMap<Long, MusicQueue> = HashMap()

    init {
        registerSourceManager(YoutubeAudioSourceManager())
        threadpool // Call Threadpool here to initialize it lazily
    }

    fun isPlaying(guild: Guild): Boolean {
        return synchronized(queueMap) {
            queueMap.containsKey(guild.idLong)
        }
    }

    fun stopPlaying(guild: Guild) {
        synchronized(queueMap) {
            val musicQueue = queueMap[guild.idLong] ?: return@synchronized
            musicQueue.dispose()
            queueMap.remove(musicQueue.voiceChannel.guild.idLong)
        }
    }

    fun addTrack(voiceChannel: VoiceChannel, track: MemberTrack): Int {
        if(!isPlaying(voiceChannel.guild)) {
            setupPlayer(voiceChannel, track)
            return 0
        } else {
            return synchronized(queueMap) {
                val queue = queueMap[voiceChannel.guild.idLong] ?: return -1
                queue.queue(track)
            }
        }
    }

    fun addTracks(voiceChannel: VoiceChannel, tracks: List<MemberTrack>) {
        if(!isPlaying(voiceChannel.guild))
            setupPlayer(voiceChannel, tracks[0])
        val queue = getQueue(voiceChannel.guild) ?: return
        for(i in 1 until tracks.size)
            queue.queue(tracks[i])
    }

    fun getQueue(guild: Guild): MusicQueue? {
        return synchronized(queueMap) {
            queueMap[guild.idLong]
        }
    }

    override fun onEvent(event: Event?) {
        when(event) {
            // Dispose on shutdown
            is ShutdownEvent -> {
                synchronized(queueMap) {
                    queueMap.forEach { _, u -> u.dispose() }
                }

                // This threadpool is used exclusively for closing
                // audio connections. As intended, it should run any
                // remaining processes on shutdown to prevent leaks.
                threadpool.shutdownNow().forEach { it.run() }

                shutdown()
            }

            // Dispose if we leave a guild for whatever reason
            is GuildLeaveEvent -> removeGuild(event.guild.idLong)

            // Dispose if certain events are fired
            is GuildVoiceLeaveEvent -> if(event.isSelf) removeGuild(event.guild.idLong)
            is GuildVoiceMuteEvent -> if(event.isSelf && event.isMuted) removeGuild(event.guild.idLong)
            is GuildVoiceSuppressEvent -> if(event.isSelf && event.isSuppressed) removeGuild(event.guild.idLong)
        }
    }

    override fun onEvent(event: AudioEvent?) {
        when(event) {
            is TrackStartEvent     -> LOG.debug("Track Started | Title: ${event.track.info.title}")
            is TrackEndEvent       -> {
                when(event.endReason)
                {
                    null           -> return
                    FINISHED       -> onTrackFinished(event)
                    LOAD_FAILED    -> LOG.debug("Track Load Failed | ${logTrackInfo(event.track)}")
                    STOPPED        -> LOG.debug("Track Stopped | ${logTrackInfo(event.track)}")
                    REPLACED       -> LOG.debug("Track Replaced | ${logTrackInfo(event.track)}")
                    CLEANUP        -> LOG.debug("Track Cleanup | ${logTrackInfo(event.track)}")
                }
            }
            is TrackExceptionEvent -> LOG.error("Track Exception | ${logTrackInfo(event.track)}", event.exception)
            is TrackStuckEvent     -> LOG.debug("Track Stuck | ${logTrackInfo(event.track)} | ${event.thresholdMs}ms")
        }
    }

    private fun setupPlayer(voiceChannel: VoiceChannel, firstTrack: MemberTrack) {
        synchronized(queueMap) {
            if(queueMap.containsKey(voiceChannel.guild.idLong))
                throw IllegalArgumentException("Attempted to join a VoiceChannel on a Guild already being handled!")

            voiceChannel.guild.audioManager.openAudioConnection(voiceChannel)

            val player = createPlayer()
            player.addListener(this)
            queueMap[voiceChannel.guild.idLong] = MusicQueue(voiceChannel, player, firstTrack)
        }
    }

    private fun removeGuild(guildId: Long) = synchronized(queueMap) {
        if(queueMap.contains(guildId))
            queueMap.remove(guildId)?.dispose()
    }

    private fun onTrackFinished(event: TrackEndEvent) {
        LOG.debug("Track Finished | ${logTrackInfo(event.track)}")

        val guildQueue: MusicQueue = synchronized(queueMap) {
            queueMap[((event.track ?: return).userData as Member).guild.idLong] ?: return
        }

        guildQueue.next()

        if(guildQueue.isDead) {
            synchronized(queueMap) {
                queueMap.remove(guildQueue.voiceChannel.guild.idLong)
            }
        }
    }

    private inline val <reified E: GenericGuildVoiceEvent> E.isSelf : Boolean
        get() = member == guild.selfMember
}
