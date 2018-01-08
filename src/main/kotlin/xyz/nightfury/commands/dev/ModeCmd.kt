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
package xyz.nightfury.commands.dev

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.extensions.niceName
import xyz.nightfury.listeners.CommandListener

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a mode to set to.")
class ModeCmd : Command()
{
    init {
        this.name = "Mode"
        this.arguments = "[Standard, Idle, Debug]"
        this.help = "Sets the bots mode."
        this.guildOnly = false
        this.devOnly = true
        this.category = Category.SHENGAERO
    }

    override fun execute(event: CommandEvent)
    {
        event.client.mode = CommandListener.Mode.typeOf(event.args)
                ?: return event.replyWarning("Invalid mode!")
        event.replySuccess("Set mode to `${event.client.mode.niceName}`!")
    }
}