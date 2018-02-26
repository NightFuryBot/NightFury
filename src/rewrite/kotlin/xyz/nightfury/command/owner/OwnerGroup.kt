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
package xyz.nightfury.command.owner

import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext

/**
 * @author Kaidan Gustave
 */
object OwnerGroup: Command.Group("Owner") {
    override val defaultLevel = Command.Level.SHENGAERO
    override val guildOnly = false
    override val devOnly = true

    override fun check(ctx: CommandContext): Boolean = ctx.isDev
}
