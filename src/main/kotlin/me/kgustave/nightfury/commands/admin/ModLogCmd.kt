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

import me.kgustave.nightfury.*
import me.kgustave.nightfury.extensions.Find
import me.kgustave.nightfury.utils.multipleTextChannelsFound
import me.kgustave.nightfury.utils.noMatch


/**
 * @author Kaidan Gustave
 */
class ModLogCmd : NoBaseExecutionCommand()
{
    init {
        this.name = "modlog"
        this.help = "manages the servers moderation log"
        this.category = Category.ADMIN
        this.guildOnly = true
        this.children = arrayOf(ModLogSetCmd())
    }
}

private class ModLogSetCmd : Command()
{
    init {
        this.name = "set"
        this.fullname = "modlog set"
        this.arguments = Argument("[channel]")
        this.help = "sets a channel as the server's moderation log"
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed,fullname))
        val channel = with(Find.textChannels(event.args,event.guild))
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