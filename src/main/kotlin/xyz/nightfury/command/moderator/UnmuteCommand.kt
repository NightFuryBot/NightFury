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

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.listeners.ModLog
import xyz.nightfury.util.db.mutedRole
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.formattedName
import xyz.nightfury.util.jda.removeRole
import xyz.nightfury.util.parseModeratorArgument

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class UnmuteCommand : Command(ModeratorGroup) {
    override val name = "Unmute"
    override val arguments = "[@User] <Reason>"
    override val help = "Unmutes a user on this server."
    override val botPermissions = arrayOf(
        Permission.MANAGE_ROLES,
        Permission.MANAGE_PERMISSIONS
    )

    override suspend fun execute(ctx: CommandContext) {
        val mutedRole = ctx.guild.mutedRole ?: return ctx.replyError {
            "**This server has no muted role!**\n" +
            "Try using `${ctx.client.prefix}$name Role` to set this server's muted role."
        }

        if(!ctx.selfMember.canInteract(mutedRole)) return ctx.replyError {
            "The unmute command cannot be used because I cannot " +
            "interact with this server's muted role!"
        }

        val modArgs = parseModeratorArgument(ctx.args) ?: return ctx.invalidArgs()

        val targetId = modArgs.first
        val reason = modArgs.second

        val member = ctx.guild.getMemberById(targetId)

        if(member === null) {
            // Theoretically this should only happen if they use direct ID
            // as a reference. In the case they don't, well they know what
            // the fuck they are doing anyways.
            // Can't cover everything I assume.
            return ctx.replyError("Could not find a user with ID: $targetId")
        }

        val target = member.user

        // Error Responses
        when {
            // Both of these are theoretical
            ctx.selfUser == target || ctx.author == target -> return ctx.replyError("What....?")

            mutedRole !in member.roles -> return ctx.replyError {
                "${target.name} is not muted, and thus cannot be unmuted!"
            }

            !ctx.selfMember.canInteract(member) -> return ctx.replyError {
                "I cannot unmute ${target.formattedName(true)}!"
            }

            !ctx.member.canInteract(member) -> return ctx.replyError {
                "You cannot unmute ${target.formattedName(true)}!"
            }
        }

        try {
            member.removeRole(mutedRole).await()
        } catch(t: Throwable) {
            return ctx.replyError("An error occurred while unmuting ${target.formattedName(true)}")
        }

        ctx.replySuccess("${target.formattedName(true)} was unmuted.")

        launch(ctx) {
            ModLog.newUnmute(ctx.member, target, reason)
        }
    }
}
