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

import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.findBannedUsers
import me.kgustave.nightfury.extensions.formattedName
import me.kgustave.nightfury.extensions.multipleUsers
import me.kgustave.nightfury.extensions.noMatch
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a banned user to unban!")
class UnbanCmd : Command()
{
    init {
        this.name = "Unban"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Unbans a user from the server."
        this.botPermissions = arrayOf(Permission.BAN_MEMBERS)
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val bans = event.guild findBannedUsers event.args
                ?:return event.replyError("An unexpected error occurred when retrieving banned members!")
        when
        {
            bans.isEmpty() -> event.replyError(noMatch("users", event.args))
            bans.size > 1  -> event.replyError(bans multipleUsers event.args)
            else -> {
                event.guild.controller.unban(bans[0])
                event.replySuccess("Successfully unbanned ${bans[0].formattedName(true)}")
            }
        }
    }
}