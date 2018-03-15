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
package xyz.nightfury.command.moderator

import net.dv8tion.jda.core.Permission
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.db.modLog
import xyz.nightfury.util.db.mutedRole
import xyz.nightfury.util.db.settings
import xyz.nightfury.util.jda.embed

/**
 * @author Kaidan Gustave
 */
class SettingsCommand : Command(ModeratorGroup) {
    override val name = "Settings"
    override val aliases = arrayOf("Config", "Configurations")
    override val arguments by lazy { children.joinToString("|", "[", "]", 4, "...") { it.name } }
    override val help = "Manage server configurations and settings for the bot."
    override val botPermissions = arrayOf(
        Permission.MESSAGE_EMBED_LINKS
    )

    override suspend fun execute(ctx: CommandContext) {
        val guild = ctx.guild
        val settings = guild.settings
        val mutedRole = guild.mutedRole
        val modLog = guild.modLog
        embed {
            color { ctx.selfMember.color }
        }
    }
}
