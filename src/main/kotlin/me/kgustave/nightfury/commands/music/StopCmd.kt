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
package me.kgustave.nightfury.commands.music

import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.music.MusicManager

/**
 * @author Kaidan Gustave
 */
class StopCmd(musicManager: MusicManager) : MusicCmd(musicManager)
{
    init {
        this.name = "Stop"
        this.help = "Stops playing music in the server!"
    }

    override fun execute(event: CommandEvent)
    {
        if(!event.guild.isPlaying)
            return event.replyError("I am not playing music at this time!")

        if(event.selfMember.voiceChannel != null && event.selfMember.voiceChannel != event.member.voiceChannel)
            return event.replyError("You are not in the same voice channel as me!")

        musicManager.stopPlaying(event.guild)
        event.replySuccess("Stopped playing!")
    }
}