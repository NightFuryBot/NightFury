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
package me.kgustave.nightfury.commands.moderator

import club.minnced.kjda.builders.colorAwt
import club.minnced.kjda.builders.embed
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import net.dv8tion.jda.core.Permission


/**
 * @author Kaidan Gustave
 */
class SettingsCmd : Command()
{
    init {
        this.name = "settings"
        this.help = "manage and get info on the server's settings"
        this.aliases = arrayOf("config", "configurations")
        this.guildOnly = true
        this.category = Category.MODERATOR
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val guild = event.guild
        event.reply(embed {
            author {
                value = "Settings for ${guild.name} (ID: ${guild.id})"
                image = guild.iconUrl
            }
            colorAwt = event.selfMember.color
            field {
                this.name = "Prefixes"
                this.value = buildString {
                    this.append("`${event.client.prefix}`")
                    event.client.manager.getPrefixes(guild).forEach { this.append(", `$it`") }
                }
                this.inline = true
            }
            field {
                val modRole = event.client.manager.getModRole(guild)
                this.name = "Moderator Role"
                this.value = if(modRole!=null) modRole.name else "None"
                this.inline = true
            }
            field {
                val modLog = event.client.manager.getModLog(guild)
                this.name = "Moderator Log"
                this.value = if(modLog!=null) modLog.asMention else "None"
                this.inline = true
            }
            field {
                val mutedRole = event.client.manager.getMutedRole(guild)
                this.name = "Muted Role"
                this.value = if(mutedRole!=null) mutedRole.name else "None"
                this.inline = true
            }
            field {
                this.name = "Cases"
                this.value = "${event.client.manager.getCases(event.guild).size} cases"
                this.inline = true
            }
        })
    }
}