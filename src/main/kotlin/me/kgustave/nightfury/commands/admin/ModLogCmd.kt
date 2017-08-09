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
package me.kgustave.nightfury.commands.admin

import me.kgustave.kjdautils.utils.findTextChannels
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.utils.multipleTextChannelsFound
import me.kgustave.nightfury.utils.noMatch

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

@MustHaveArguments
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
                return event.replyError(multipleTextChannelsFound(event.args, this))
            return@with this[0]
        }

        event.manager.setModLog(channel)
        event.replySuccess("Moderation log was set to ${channel.asMention}!")
    }
}