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
package xyz.nightfury.commands.dev

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.db.SQLJoinWhitelist
import xyz.nightfury.music.MusicManager
import xyz.nightfury.db.SQLMusicWhitelist

/**
 * @author Kaidan Gustave
 */
class WhitelistCmd(private val musicManager: MusicManager) : Command()
{
    init {
        this.name = "Whitelist"
        this.help = "Whitelists a server by whitelist type."
        this.category = Category.SHENGAERO
        this.devOnly = true
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val type = try {
            Type.valueOf(event.args)
        } catch (e: IllegalArgumentException) {
            return event.replyError("Invalid whitelist type!")
        }

        when(type) {
            Type.MUSIC -> {
                SQLMusicWhitelist.apply {
                    if(isGuild(event.guild)) {
                        removeGuild(event.guild)
                        if(musicManager.isPlaying(event.guild))
                            musicManager.stopPlaying(event.guild)
                        event.replySuccess("Removed music whitelisting from **${event.guild.name}**!")
                    } else {
                        addGuild(event.guild)
                        event.replySuccess("Added music whitelisting to **${event.guild.name}**!")
                    }
                }
            }
            Type.JOIN -> {
                SQLJoinWhitelist.apply {
                    if(isGuild(event.guild)) {
                        removeGuild(event.guild)
                        event.replySuccess("Removed join whitelisting from **${event.guild.name}**!")
                    } else {
                        addGuild(event.guild)
                        event.replySuccess("Added join whitelisting to **${event.guild.name}**!")
                    }
                }
            }
        }
    }

    enum class Type { MUSIC, JOIN }
}
