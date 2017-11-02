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
package xyz.nightfury.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel

/**
 * @author Kaidan Gustave
 */
class MusicQueue(val voiceChannel: VoiceChannel, val audioPlayer: AudioPlayer, firstTrack: MemberTrack): AudioSendHandler,
MutableList<MemberTrack> by ArrayList()
{
    private lateinit var lastFrame: AudioFrame
    private val skipping = ArrayList<Long>()

    var currentTrack: MemberTrack
        private set

    var isDead = false
        private set

    val skips: Int
        get()
        {
            val currentIds = voiceChannel.members.map { it.user.idLong }
            skipping.forEach { if(!currentIds.contains(it)) skipping.remove(it) }
            return skipping.size
        }

    val totalToSkip : Int
        get()
        {
            val totalMembers = listeners.size
            return if(totalMembers % 2 == 0) totalMembers / 2 else (totalMembers / 2) + 1
        }

    val listeners : List<Member>
        get() = voiceChannel.members.filter { !it.user.isBot }

    init
    {
        voiceChannel.guild.audioManager.sendingHandler = this
        currentTrack = firstTrack
        audioPlayer.playTrack(currentTrack.originalTrack)
    }

    fun next()
    {
        if(isNotEmpty())
        {
            if(currentTrack.state != AudioTrackState.FINISHED)
                currentTrack.stop()
            currentTrack = removeAt(0)
            audioPlayer.playTrack(currentTrack.originalTrack)
            skipping.clear()
        }
        else dispose()
    }

    fun queue(track: MemberTrack): Int
    {
        add(track)
        return indexOf(track) + 1
    }

    // Credit to jagrosh for the original shuffle code
    fun shuffle(userId: Long): Int
    {
        val indexList = ArrayList<Int>()

        @Suppress("LoopToCallChain")
        for(i in indices)
        {
            if(this[i].member.user.idLong == userId)
                indexList += i
        }

        for(i in indexList.indices)
        {
            val first = indexList[i]
            val second = indexList[(Math.random()*indexList.size).toInt()]
            val temp = this[first]
            this[first] = this[second]
            this[second] = temp
        }

        return indexList.size
    }

    fun isSkipping(member: Member): Boolean = skipping.contains(member.user.idLong)

    fun voteToSkip(member: Member): Int
    {
        skipping.add(member.user.idLong)
        return skips
    }

    fun skip(): MemberTrack
    {
        val skippedTrack = currentTrack
        currentTrack.stop()
        next()
        return skippedTrack
    }

    override fun canProvide(): Boolean
    {
        lastFrame = audioPlayer.provide() ?: return false
        return true
    }

    override fun provide20MsAudio(): ByteArray = lastFrame.data

    override fun isOpus() = true

    fun dispose()
    {
        // Destroy
        // Close Connection
        // Die
        // Repeat...
        audioPlayer.destroy()
        MusicManager.threadpool.submit { voiceChannel.guild.audioManager.closeAudioConnection() }
        isDead = true
    }
}