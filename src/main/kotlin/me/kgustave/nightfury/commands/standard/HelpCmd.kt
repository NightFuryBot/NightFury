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
package me.kgustave.nightfury.commands.standard

import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import net.dv8tion.jda.core.entities.ChannelType

/**
 * @author Kaidan Gustave
 */
class HelpCmd : Command() {

    init {
        this.name = "help"
        this.help = "gets a list of commands"
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val commands = event.client.commands
        val b = StringBuilder()

        b.append("**Available Commands in ${
            if(event.isFromType(ChannelType.TEXT)) "<#${event.channel.id}>" else "Direct Messages"
        }:**\n\n")
        var cat : Category? = null
        for(c in commands) {
            if(cat!=c.category) {
                if(!c.category!!.test(event))
                    continue
                cat = c.category
                if(cat!=null)
                    b.append("\n__${cat.title}__\n\n")
            }
            b.append("`").append(event.client.prefix).append(c.name)
                    .append(if(c.arguments.toString().isNotEmpty()) " ${c.arguments}" else "")
                    .append("` ").append(c.help).append("\n")
        }
        event.jda.retrieveUserById(event.client.ownerID).queue({
            b.append("\nFor additional help contact**")
                    .append(it.name).append("**#").append(it.discriminator)
                    .append(" or join ").append(event.client.server)
            if(event.isFromType(ChannelType.TEXT))
                event.reactSuccess()
            event.replyInDm(b.toString())
        })
    }
}