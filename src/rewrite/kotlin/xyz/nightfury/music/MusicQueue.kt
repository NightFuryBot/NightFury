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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package xyz.nightfury.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.nightfury.util.ext.modifyIf
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.HashSet

/**
 * @author Kaidan Gustave
 */
class MusicQueue(
    val channel: VoiceChannel,
    val player: AudioPlayer,
    initTrack: MemberTrack
): AudioSendHandler, List<MemberTrack>, Queue<MemberTrack>, AutoCloseable {
    @Volatile private lateinit var lastFrame: AudioFrame

    var isDead = false
        private set
    var currentTrack = initTrack
        private set

    private val queued = LinkedList<MemberTrack>()
    private val skipping = HashSet<Long>()

    init {
        with(channel.guild.audioManager) {
            openAudioConnection(channel)
            sendingHandler = this@MusicQueue
        }
        player.playTrack(currentTrack.originalTrack)
    }

    val listening: List<Member> get() {
        return channel.members.filter { !it.user.isBot }
    }

    val listeningCount: Int get() {
        return channel.members.count { !it.user.isBot }
    }

    val totalToSkip: Int get() {
        val totalMembers = listeningCount
        return (totalMembers / 2).modifyIf(totalMembers % 2 != 0) { it + 1 }
    }

    val skips: Int get() {
        val currentIds = channel.members.mapNotNull { it.user.takeIf { !it.isBot }?.idLong }
        skipping.removeIf { it !in currentIds }
        return skipping.size
    }

    var volume: Int get() = player.volume
        set(value) { player.volume = value }

    fun queue(track: MemberTrack): Int {
        add(track)
        return indexOf(track) + 1
    }

    fun shuffle(userId: Long): Int {

        // Credit to jagrosh for the original shuffle code

        val indexList = ArrayList<Int>()

        @Suppress("LoopToCallChain")
        for(i in indices) {
            if(this[i].member.user.idLong == userId) {
                indexList += i
            }
        }

        for(i in indexList.indices) {
            val first = indexList[i]
            val second = indexList[(Math.random() * indexList.size).toInt()]
            val temp = this[first]
            this[first] = this[second]
            this[second] = temp
        }

        return indexList.size
    }

    fun isSkipping(member: Member): Boolean = member.user.idLong in skipping

    fun voteToSkip(member: Member): Int {
        skipping.add(member.user.idLong)
        return skips
    }

    fun skip(): MemberTrack {
        val skippedTrack = currentTrack
        currentTrack.stop()
        poll()
        return skippedTrack
    }

    // Queue Implementations

    override fun element(): MemberTrack = queued.element()
    override fun peek(): MemberTrack? = queued.peek()

    override fun add(element: MemberTrack): Boolean = queued.add(element)
    override fun offer(e: MemberTrack?): Boolean = queued.offer(e)

    override fun remove(): MemberTrack {
        return poll() ?: throw NoSuchElementException("Could not remove current track because the MusicQueue is empty")
    }
    override fun poll(): MemberTrack? {
        if(isNotEmpty()) {
            val current = currentTrack
            if(current.state != AudioTrackState.FINISHED) {
                current.stop()
            }
            currentTrack = remove()
            player.playTrack(currentTrack.originalTrack)
            skipping.clear()
            return currentTrack
        } else close()
        return null
    }

    fun removeAt(index: Int): MemberTrack = queued.removeAt(index)
    operator fun set(index: Int, element: MemberTrack): MemberTrack = queued.set(index, element)

    // List Implementations

    override fun get(index: Int): MemberTrack = queued[index]
    override fun indexOf(element: MemberTrack): Int = queued.indexOf(element)
    override fun lastIndexOf(element: MemberTrack): Int = queued.lastIndexOf(element)
    override fun listIterator(): ListIterator<MemberTrack> = queued.listIterator()
    override fun listIterator(index: Int): ListIterator<MemberTrack> = queued.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<MemberTrack> = queued.subList(fromIndex, toIndex)

    // MutableCollection Implementations

    override val size: Int get() = queued.size

    override fun contains(element: MemberTrack): Boolean = queued.contains(element)
    override fun containsAll(elements: Collection<MemberTrack>): Boolean = queued.containsAll(elements)
    override fun isEmpty(): Boolean = queued.isEmpty()
    override fun iterator(): MutableIterator<MemberTrack> = queued.iterator()
    override fun addAll(elements: Collection<MemberTrack>): Boolean = queued.addAll(elements)
    override fun remove(element: MemberTrack): Boolean = queued.remove(element)
    override fun removeAll(elements: Collection<MemberTrack>): Boolean = queued.removeAll(elements)
    override fun retainAll(elements: Collection<MemberTrack>): Boolean = queued.retainAll(elements)
    override fun clear() {
        queued.clear()
        skipping.clear()
        close()
    }

    // AudioSendHandler Implementations

    override fun canProvide(): Boolean {
        lastFrame = player.provide() ?: return false
        return true
    }

    override fun provide20MsAudio(): ByteArray = lastFrame.data
    override fun isOpus(): Boolean = true

    // AutoCloseable Implementation

    override fun close() {
        player.destroy()

        // JDA Audio Connections MUST be closed on a separate thread
        launch(MusicManager.context) { channel.guild.audioManager.closeAudioConnection() }

        isDead = true
    }

    // Other Implementations

    override fun hashCode(): Int = channel.idLong.hashCode()

    override fun equals(other: Any?): Boolean {
        if(other !is MusicQueue)
            return false

        return channel == other.channel
    }

    override fun toString(): String {
        return "MusicQueue(VC: ${channel.idLong}, Queued: $size, Now Playing: ${currentTrack.identifier})"
    }
}
