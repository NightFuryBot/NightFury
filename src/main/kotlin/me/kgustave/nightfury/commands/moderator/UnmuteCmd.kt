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

import club.minnced.kjda.promise
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.removeRole
import me.kgustave.nightfury.extensions.formattedName
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Mention a user or provide a user ID to unmute.")
class UnmuteCmd : Command()
{
    init {
        this.name = "Unmute"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Unmutes a user."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.MODERATOR
    }

    override fun execute(event: CommandEvent)
    {
        val mutedRole = event.client.manager.getMutedRole(event.guild)
                ?:return event.replyError("**Muted role has not been setup!**\n" +
                "Try using `${event.prefixUsed}mute setup` to create a new mute role, or `${event.prefixUsed}mute set` to " +
                "register an existing one!")

        val parsed = event.modSearch()?:return

        val id = parsed.first
        val reason = parsed.second

        val target = event.guild.getMemberById(id)?:return event.replyError("Could not find a member matching \"${event.args}\"!")

        // Error Responses
        val error = when
        {
            !target.roles.contains(mutedRole)
                    -> "I cannot unmute ${target.user.formattedName(true)} because they are not muted!"
            !event.selfMember.canInteract(mutedRole)
                    -> "I cannot remove **${mutedRole.name}** from ${target.user.formattedName(true)}" +
                    "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.removeRole(mutedRole).apply { if(reason!=null) reason(reason) }.promise() then {
            if(reason != null) event.client.logger.newUnmute(event.member, target.user, reason)
            else               event.client.logger.newUnmute(event.member, target.user)
            event.replySuccess("${target.user.formattedName(true)} was unmuted!")
        } catch {
            event.replyError("Unmuting ${target.user.formattedName(true)} failed for an unexpected reason!")
        }
    }
}