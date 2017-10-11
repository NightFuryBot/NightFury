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

import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.annotations.APICache
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.api.GoogleImageAPI
import me.kgustave.nightfury.extensions.embed
import me.kgustave.nightfury.extensions.message
import me.kgustave.nightfury.resources.Algorithms
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType

/**
 * @author Kaidan Gustave
 */
@[APICache MustHaveArguments("Specify what to search for.")]
class ImageCmd(private val api: GoogleImageAPI) : Command()
{
    init {
        this.name = "Image"
        this.arguments = "[Query]"
        this.guildOnly = false
        this.cooldown = 60
        this.cooldownScope = CooldownScope.USER
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        event.channel.sendTyping().queue {
            val results = api.search(query)
            when {
                results == null -> event.replyError("An unexpected error occurred while searching!")
                results.isEmpty() -> event.replyError("No results were found for \"**$query**\"!")
                else -> {
                    event.reply(message {
                        append { "${event.client.success} ${event.author.asMention}" }
                        embed  {
                            if(event.isFromType(ChannelType.TEXT))
                                color { event.member.color }
                            image { Algorithms.selectResultURL(query, results) }
                        }
                    })
                }
            }
            event.invokeCooldown()
        }
    }

    @[APICache Suppress("unused")]
    fun clearCache() = api.clearCache()

}