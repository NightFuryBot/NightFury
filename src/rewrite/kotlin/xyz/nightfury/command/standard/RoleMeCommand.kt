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
package xyz.nightfury.command.standard

import net.dv8tion.jda.core.Permission
import xyz.nightfury.command.AutoCooldown
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.listeners.EventWaiter
import xyz.nightfury.util.db.*
import xyz.nightfury.util.ext.*
import xyz.nightfury.util.menus.Paginator

/**
 * @author Kaidan Gustave
 */
@AutoCooldown
@MustHaveArguments
class RoleMeCommand(waiter: EventWaiter): Command(StandardGroup) {
    override val name = "RoleMe"
    override val arguments = "[Role Name]"
    override val help = "Assigns you a RoleMe role by name."
    override val guildOnly = true
    override val cooldown = 5
    override val cooldownScope = CooldownScope.USER_GUILD
    override val botPermissions = arrayOf(Permission.MANAGE_ROLES)
    override val children = arrayOf(
        RoleMeAddCommand(),
        RoleMeLimitCommand(),
        RoleMeListCommand(waiter),
        RoleMeRemoveCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args

        val roleMeRoles = ctx.guild.roleMeRoles

        // We have a different error if there is no RoleMe roles at all
        if(roleMeRoles.isEmpty()) {
            return ctx.replyError("There are no RoleMe roles on this server!")
        }

        val found = ctx.guild.findRoles(query)

        val roleMeRolesFound = found.filter { it in roleMeRoles }

        val roleMeRole = when {
            roleMeRolesFound.isEmpty() -> return ctx.replyError(noMatch("RoleMe roles", query))
            roleMeRolesFound.size > 1 -> return ctx.replyError(roleMeRolesFound.multipleRoles(query))
            else -> roleMeRolesFound[0]
        }

        val member = ctx.member

        if(roleMeRole !in member.roles) {

            // Check for a limit
            val roleMeLimit = ctx.guild.getCommandLimit(this) ?: 0

            if(roleMeLimit > 0) {
                // The user is at the RoleMe limit
                if(roleMeLimit == roleMeRoles.count { it in member.roles }) {
                    return ctx.replyError("You are at the RoleMe role limit for this server.")
                }
            }

            member.giveRole(roleMeRole).await()
            ctx.replySuccess("Successfully gave the **${roleMeRole.name}** role!")
        } else {
            member.removeRole(roleMeRole).await()
            ctx.replySuccess("Successfully removed the **${roleMeRole.name}** role!")
        }
    }

    @MustHaveArguments
    private inner class RoleMeAddCommand : Command(this@RoleMeCommand) {
        override val name = "Add"
        override val arguments = "[Role Name]"
        override val help = "Adds a RoleMe role to the server."
        override val guildOnly = true
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val found = ctx.guild.findRoles(query)

            val role = when {
                found.isEmpty() -> return ctx.replyError(noMatch("roles", query))
                found.size > 1 -> return ctx.replyError(found.multipleRoles(query))
                else -> found[0]
            }

            if(role.isRoleMe) {
                return ctx.replyError("**${role.name}** is already a RoleMe role!")
            }

            role.isRoleMe = true
            ctx.replySuccess("Successfully added **${role.name}** as a RoleMe role!")
        }
    }

    @MustHaveArguments
    private inner class RoleMeRemoveCommand : Command(this@RoleMeCommand) {
        override val name = "Remove"
        override val arguments = "[Role Name]"
        override val help = "Removes a RoleMe role from the server."
        override val guildOnly = true
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val roleMeRoles = ctx.guild.roleMeRoles

            if(roleMeRoles.isEmpty()) {
                return ctx.replyError("There are no RoleMe roles on this server!")
            }

            val found = ctx.guild.findRoles(query).filter { it in roleMeRoles }

            val role = when {
                found.isEmpty() -> return ctx.replyError(noMatch("RoleMe roles", query))
                found.size > 1 -> return ctx.replyError(found.multipleRoles(query))
                else -> found[0]
            }

            // We don't need to check if it's not a RoleMe role because we already
            // filtered out all the non-RoleMe roles

            role.isRoleMe = false
            ctx.replySuccess("Successfully removed **${role.name}** from RoleMe roles!")
        }
    }

    private inner class RoleMeLimitCommand : Command(this@RoleMeCommand) {
        override val name = "Limit"
        override val arguments = "<Number>"
        override val help = "Sets the limit for RoleMe roles on the server."
        override val guildOnly = true
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            val currentLimit = ctx.guild.getCommandLimit(this)

            if(args.isEmpty()) {
                return if(currentLimit === null) {
                    ctx.replySuccess("This server does not have a RoleMe limit.")
                } else {
                    ctx.replySuccess("The RoleMe limit for this server is $currentLimit RoleMe roles.")
                }
            }

            val limit = try {
                args.toInt()
            } catch(e: NumberFormatException) {
                return ctx.replyError("\"$args\" is not a valid number!")
            }

            ctx.guild.setCommandLimit(this, limit.takeIf { it > 0 })
            if(limit > 0) {
                ctx.replySuccess("Removed the RoleMe limit for this server.")
            } else {
                ctx.replySuccess("Successfully set the RoleMe limit for this server to $limit RoleMe roles.")
            }
        }
    }

    @AutoCooldown
    private inner class RoleMeListCommand(waiter: EventWaiter): Command(this@RoleMeCommand) {
        override val name = "List"
        override val help = "Lists all the RoleMe roles on the server."
        override val guildOnly = true
        override val cooldown = 5
        override val cooldownScope = CooldownScope.USER_GUILD
        override val botPermissions = arrayOf(
            Permission.MANAGE_ROLES,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ADD_REACTION
        )

        private val builder = Paginator.Builder {
            waiter           { waiter }
            timeout          { delay { 20 } }
            showPageNumbers  { true }
            numberItems      { true }
            waitOnSinglePage { false }
        }

        override suspend fun execute(ctx: CommandContext) {
            val roleMeRoles = ctx.guild.roleMeRoles

            if(roleMeRoles.isEmpty()) {
                return ctx.replyError("There are no RoleMe roles on this server!")
            }

            builder.clearItems()

            val paginator = Paginator(builder) {
                text        { _,_ -> "RoleMe Roles On ${ctx.guild.name}" }
                items       { addAll(roleMeRoles.map { it.name }) }
                finalAction { ctx.linkMessage(it) }
                user        { ctx.author }
            }

            paginator.displayIn(ctx.channel)
            ctx.invokeCooldown()
        }
    }
}
