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
import net.dv8tion.jda.core.Permission.*
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.listeners.EventWaiter
import xyz.nightfury.listeners.ModLog
import xyz.nightfury.util.db.hasMutedRole
import xyz.nightfury.util.db.mutedRole
import xyz.nightfury.util.ext.*
import xyz.nightfury.util.menus.Paginator
import xyz.nightfury.util.parseModeratorArgument
import java.awt.Color

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class MuteCommand(waiter: EventWaiter): Command(ModeratorGroup) {
    override val name = "Mute"
    override val arguments = "[@User] <Reason>"
    override val help = "Mutes a user on this server."
    override val botPermissions = arrayOf(
        MANAGE_ROLES,
        MANAGE_PERMISSIONS
    )
    override val children = arrayOf(
        MuteListCommand(waiter),
        MuteRoleCommand(),
        MuteSetupCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val mutedRole = ctx.guild.mutedRole ?: return ctx.replyError {
            "This server has no muted role!"
        }

        if(!ctx.selfMember.canInteract(mutedRole)) {
            return ctx.replyError {
                "The mute command cannot be used because I cannot " +
                "interact with this server's muted role!"
            }
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
            ctx.selfUser == target -> return ctx.replyError {
                "I cannot mute myself!"
            }

            ctx.author == target -> return ctx.replyError {
                "You cannot mute yourself!"
            }

            ctx.guild.owner.user == target -> return ctx.replyError {
                "You cannot mute ${target.formattedName(true)} because they are the owner of the server!"
            }

            !ctx.selfMember.canInteract(member) -> return ctx.replyError {
                "I cannot mute ${target.formattedName(true)}!"
            }

            !ctx.member.canInteract(member) -> return ctx.replyError {
                "You cannot mute ${target.formattedName(true)}!"
            }

            mutedRole in member.roles -> return ctx.replyError {
                "${target.name} is already muted."
            }
        }

        try {
            member.giveRole(mutedRole).await()
        } catch(t: Throwable) {
            return ctx.replyError("An error occurred while muting ${target.formattedName(true)}")
        }

        ctx.replySuccess("${target.formattedName(true)} was muted.")

        launch(ctx) {
            ModLog.newMute(ctx.member, target, reason)
        }
    }

    private inner class MuteListCommand(waiter: EventWaiter): Command(this@MuteCommand) {
        override val name = "List"
        override val help = "Gets a list of muted members on this server."
        override val botPermissions = arrayOf(
            MESSAGE_EMBED_LINKS,
            MESSAGE_MANAGE,
            MESSAGE_ADD_REACTION,
            MANAGE_ROLES,
            MANAGE_PERMISSIONS
        )

        private val builder = Paginator.Builder {
            waiter { waiter }
            timeout { delay { 20 } }
            waitOnSinglePage { false }
            showPageNumbers  { true }
            numberItems { true }
            text { p, t -> "Server Mutes${if(t > 0) " [`$p/$t`]" else ""}" }
        }

        override suspend fun execute(ctx: CommandContext) {
            val mutedRole = ctx.guild.mutedRole ?: return ctx.replyError {
                "This server has no muted role!"
            }

            val members = ctx.guild.getMembersWithRoles(mutedRole)

            if(members.isEmpty()) {
                return ctx.replySuccess("There are no members on this server who are muted!")
            }

            builder.clearItems()
            val paginator = Paginator(builder) {
                members.forEach { add { "${it.user.formattedName(true)} (ID: ${it.user.idLong})" } }
                user { ctx.author }
                finalAction {
                    if(ctx.selfMember.hasPermission(ctx.textChannel, MESSAGE_MANAGE)) {
                        it.clearReactions()
                    }
                    ctx.linkMessage(it)
                }
            }

            paginator.displayIn(ctx.channel)
        }
    }

    @MustHaveArguments
    private inner class MuteRoleCommand : Command(this@MuteCommand) {
        override val name = "Role"
        override val arguments = "[Role]"
        override val help = "Sets the mute role for the server."
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(
            MANAGE_ROLES,
            MANAGE_PERMISSIONS
        )

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val guild = ctx.guild
            val found = guild.findRoles(query)
            val target = when {
                found.isEmpty() -> return ctx.replyError(noMatch("roles", query))
                found.size > 1 -> return ctx.replyError(found.multipleRoles(query))
                else -> found[0]
            }
            guild.mutedRole = target
            guild.refreshMutedRole(target)
            ctx.replySuccess("Successfully set this server's muted role as ${target.name}!")
        }
    }

    private inner class MuteSetupCommand : Command(this@MuteCommand) {
        override val name = "Setup"
        override val arguments = "<Name of New Role>"
        override val help = "Creates a new mute role for the server."
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(
            MANAGE_ROLES,
            MANAGE_PERMISSIONS
        )

        override suspend fun execute(ctx: CommandContext) {
            val guild = ctx.guild
            if(guild.hasMutedRole) return ctx.replyError {
                "**This server already has a muted role!**\n" +
                "To change the muted role for the server, use `${ctx.client.prefix}Mute Role`."
            }

            val role = try {
                guild.controller.createRole {
                    name { if(ctx.args.isEmpty()) "Muted" else ctx.args }
                    color { Color.BLACK }
                }.await()
            } catch(ignored: Throwable) {
                return ctx.replyError("Failed to create new muted role for an unknown reason.")
            }

            guild.mutedRole = role
            guild.refreshMutedRole(role)
            ctx.replySuccess("Successfully created muted role: **${role.name}**!")
        }
    }
}
