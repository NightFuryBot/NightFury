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

import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.db.isMusic

/**
 * @author Kaidan Gustave
 */
object MusicGroup : Command.Group("Music") {
    override val defaultLevel = Command.Level.STANDARD
    override val guildOnly = true
    override val devOnly = false
    override fun check(ctx: CommandContext): Boolean = ctx.isDev || ctx.guild.isMusic
}
