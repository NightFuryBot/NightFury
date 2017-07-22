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
package me.kgustave.nightfury.commands

import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.NightFury
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game

/**
 * @author Kaidan Gustave
 */
class ShutdownCmd : Command()
{
    init {
        this.name = "shutdown"
        this.help = "shuts down NightFury"
        this.devOnly = true
        this.category = Category.OWNER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        event.replyWarning("Shutting Down...")
        event.jda.presence.status = OnlineStatus.DO_NOT_DISTURB
        event.jda.presence.game = Game.of("Shutting Down...")
        NightFury.LOG.info("Shutting Down...")
        synchronized(this) {
            try { Thread.sleep(2500) } catch (ignored: InterruptedException) {}
        }
        event.jda.shutdown(true)
        NightFury.shutdown(1)
    }
}