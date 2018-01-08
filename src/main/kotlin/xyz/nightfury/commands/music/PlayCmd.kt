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

import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.music.MusicManager

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Try specifying the name of a song, or providing a YouTube link.")
class PlayCmd(musicManager: MusicManager) : MusicCmd(musicManager)
{
    init {
        this.name = "Play"
        this.help = "Plays a song based on user query."
    }

    override fun execute(event: CommandEvent) {
        val query = event.args
        if(query.toLowerCase().startsWith("ytsearch:") || query.toLowerCase().startsWith("scsearch:"))
            return event.replyError("Invalid query! The format specified is not allowed!")
        event.reply("Searching for tracks...") {
            musicManager.loadItemOrdered(event.guild, query, SearchResultHandler(event, it, query))
        }
    }
}