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
package xyz.nightfury.commands.admin

import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.extensions.findTextChannels
import xyz.nightfury.extensions.multipleTextChannels
import xyz.nightfury.extensions.noMatch
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.NoBaseExecutionCommand
import xyz.nightfury.db.SQLModeratorLog

/**
 * @author Kaidan Gustave
 */
class ModLogCmd : NoBaseExecutionCommand()
{
    init {
        this.name = "ModLog"
        this.help = "Manages the server's moderation log."
        this.category = Category.ADMIN
        this.guildOnly = true
        this.children = arrayOf(ModLogSetCmd())
    }
}

@MustHaveArguments("Specify a text channel to use as the server's moderation log.")
private class ModLogSetCmd : Command()
{
    init {
        this.name = "Set"
        this.fullname = "ModLog Set"
        this.arguments = "[Channel]"
        this.help = "Sets a channel as the server's moderation log."
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val channel = with(event.guild.findTextChannels(event.args))
        {
            if(this.isEmpty())
                return event.replyError(noMatch("channels", event.args))
            if(this.size>1)
                return event.replyError(this.multipleTextChannels(event.args))
            return@with this[0]
        }

        SQLModeratorLog.setChannel(channel)
        event.replySuccess("Moderation log was set to ${channel.asMention}!")
    }
}