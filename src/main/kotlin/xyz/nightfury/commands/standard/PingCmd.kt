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
package xyz.nightfury.commands.standard

import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.doc.Documentation
import java.time.temporal.ChronoUnit

/**
 * @author Kaidan Gustave
 */
@Documentation(
    name = ["Ping", "Pong", "Pang", "Pyng", "Pung", "Peng", "Png"],
    description = "Gets the bot's REST latency (in milliseconds)."
)
class PingCmd : Command()
{
    init {
        this.name = "Ping"
        this.aliases = arrayOf("pong", "pang", "pyng", "pung", "peng", "png")
        this.help = "Tests the bot's latency."
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        event.reply("Ping...") {
            it.editMessage("Ping: ${event.message.creationTime.until(it.creationTime, ChronoUnit.MILLIS)}ms").queue()
        }
    }
}