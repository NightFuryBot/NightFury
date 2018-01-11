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
package xyz.nightfury.commands.moderator

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.entities.embed
import net.dv8tion.jda.core.Permission
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.*

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class SettingsCmd : Command()
{
    init {
        this.name = "Settings"
        this.help = "Get info on the server's settings."
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
            color { event.selfMember.color }
            field {
                this.name = "Prefixes"
                this.value = buildString {
                    this.append("`${event.client.prefix}`")
                    SQLPrefixes.getPrefixes(guild).forEach { this.append(", `$it`") }
                }
                this.inline = true
            }
            field {
                val modRole = SQLModeratorRole.getRole(guild)
                this.name = "Moderator Role"
                this.value = modRole?.name ?: "None"
                this.inline = true
            }
            field {
                val modLog = SQLModeratorLog.getChannel(guild)
                this.name = "Moderation Log"
                this.value = modLog?.name ?: "None"
                this.inline = true
            }
            field {
                val mutedRole = SQLMutedRole.getRole(guild)
                this.name = "Muted Role"
                this.value = mutedRole?.name ?: "None"
                this.inline = true
            }
            field {
                this.name = "Cases"
                this.value = "${SQLCases.getCases(event.guild).size} cases"
                this.inline = true
            }
        })
    }
}
