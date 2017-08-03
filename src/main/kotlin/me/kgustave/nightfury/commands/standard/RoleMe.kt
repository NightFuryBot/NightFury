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

import club.minnced.kjda.promise
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.findRoles
import me.kgustave.nightfury.extensions.giveRole
import me.kgustave.nightfury.extensions.removeRole
import me.kgustave.nightfury.extensions.waiting.paginator
import me.kgustave.nightfury.utils.multipleRolesFound
import me.kgustave.nightfury.utils.noMatch
import net.dv8tion.jda.core.Permission
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class RoleMeCmd(waiter: EventWaiter) : Command()
{
    init {
        this.name = "RoleMe"
        this.arguments = "[Role]"
        this.help = "Give yourself or remove a RoleMe role."
        this.helpBiConsumer = Command.standardSubHelp(
                "RoleMe roles are self-give/remove roles for normal members of a server.\n" +
                        "Simply using `RoleMe [Role]` will give the user the `[Role]` requested or remove " +
                        "it if they already possess the `[Role]`, so long as it is a registered RoleMe role.\n\n" +

                        "RoleMe roles must be registered by an Administrator using the `Add` sub-command.\n" +
                        "Conversely, registered RoleMe roles can be unregistered by an Administrator using " +
                        "the `Remove` sub-command.\n" +
                        "For those looking to limit the number of RoleMe roles a user can have, the `Limit` " +
                        "sub-command provides a means of doing this.",
                true
        )
        this.cooldown = 10
        this.guildOnly = true
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.children = arrayOf(
                RoleMeListCmd(waiter),

                RoleMeAddCmd(),
                RoleMeLimitCmd(),
                RoleMeRemoveCmd()
        )
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val allRoleMes = event.manager.getRoleMes(event.guild)
        if(allRoleMes.isEmpty())
            return event.replyError("**No RoleMe roles on this server!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        val roles = event.guild.findRoles(query)
        if(roles.isEmpty())
            return event.replyError(noMatch("roles", query))
        val roleMes = roles.stream().filter { event.manager.isRoleMe(it) }.toList()
        if(roleMes.isEmpty() && roles.isNotEmpty())
            return event.replyError("**${roles[0].name} is not a RoleMe role!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        if(roleMes.size>1)
            return event.replyError(multipleRolesFound(query, roleMes))
        val requested = roleMes[0]
        if(!event.selfMember.canInteract(requested))
            event.replyError("**Cannot interact with requested role!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        else if(!event.member.roles.contains(requested)) {
            if(event.hasRoleMeLimit()) {
                if(event.getRoleMeLimit()<=event.member.roles.filter { event.manager.isRoleMe(it) }.size)
                    return event.replyError("More RoleMe roles cannot be added because you are at the limit set by the server!")
            }
            event.member.giveRole(requested).promise() then {
                event.replySuccess("Successfully gave the role **${requested.name}**!")
                event.invokeCooldown()
            } catch {
                event.replyError("An error occurred while giving the role **${requested.name}**!")
            }
        } else {
            event.member.removeRole(requested).promise() then {
                event.replySuccess("Successfully removed the role **${requested.name}**!")
                event.invokeCooldown()
            } catch {
                event.replyError("An error occurred while removing the role **${requested.name}**!")
            }
        }
    }

    fun CommandEvent.hasRoleMeLimit() : Boolean = this.manager.hasLimit(this.guild, this@RoleMeCmd.name)

    fun CommandEvent.getRoleMeLimit() : Int = this.manager.getLimit(this.guild, this@RoleMeCmd.name)
}

@MustHaveArguments
private class RoleMeAddCmd : Command()
{
    init {
        this.name = "Add"
        this.fullname = "RoleMe Add"
        this.arguments = "[Role]"
        this.help = "Adds a RoleMe role for the server."
        this.helpBiConsumer = Command.standardSubHelp(
                "Roles added will be available to all members on the server via the `RoleMe` " +
                        "command, except in cases that NightFury cannot give roles to or remove roles from " +
                        "the member using the command!\n\n" +

                        "**NightFury is not responsible for any dangerous permissions given with these," +
                        "nor the consequences of the aforementioned!**",
                true
        )
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val found = event.guild.findRoles(query)
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(multipleRolesFound(query, found))
        val requested = found[0]
        if(event.manager.isRoleMe(requested))
            return event.replyError("The role **${requested.name}** is already a RoleMe role!")
        event.manager.addRoleMe(requested)
        if(event.selfMember.canInteract(requested))
            event.replySuccess("The role **${requested.name}** was added as RoleMe!")
        else
            event.replyWarning("The role **${requested.name}** was added as RoleMe!\n" +
                    "Please be aware that due to role hierarchy positioning, I will not be able to give this role to members!\n" +
                    "To fix this, make sure my I have a role higher than `${requested.name}` on the roles list.")
        event.invokeCooldown()
    }

}

@MustHaveArguments
private class RoleMeRemoveCmd : Command()
{
    init {
        this.name = "Remove"
        this.fullname = "RoleMe Remove"
        this.arguments = "[Role]"
        this.help = "Removes a RoleMe role for the server."
        this.helpBiConsumer = Command.standardSubHelp(
                "**Using this will not remove the RoleMe role from members who previously had it!**",
                true
        )
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val found = event.guild.findRoles(query).stream()
                .filter { event.manager.isRoleMe(it) }.toList()
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(multipleRolesFound(query, found))
        event.manager.removeRoleMe(found[0])
        event.replySuccess("The role **${found[0].name}** was removed from RoleMe!")
    }
}

@AutoInvokeCooldown
private class RoleMeListCmd(val waiter: EventWaiter) : Command()
{
    init {
        this.name = "List"
        this.fullname = "RoleMe List"
        this.help = "Gets a full list of RoleMe roles on this server."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        val rolemes = event.manager.getRoleMes(event.guild).map { it.name }
        if(rolemes.isEmpty())
            return event.replyError("**No RoleMe roles on this server!**\n" +
                    SEE_HELP.format(event.client.prefix, "RoleMe"))
        paginator(waiter, event.channel)
        {
            text             { "RoleMe Roles On ${event.guild.name}" }
            timeout          { 20 }
            items            { addAll(rolemes) }
            finalAction      { event.linkMessage(it) }
            showPageNumbers  { true }
            useNumberedItems { true }
            waitOnSinglePage { false }
        }
    }
}

@MustHaveArguments("Try specifying a limit in the form of a number.")
private class RoleMeLimitCmd : Command()
{
    init {
        this.name = "Limit"
        this.fullname = "RoleMe Limit"
        this.arguments = "[Number]"
        this.help = "Sets the limit of RoleMe roles a user can have on the server."
        this.helpBiConsumer = Command.standardSubHelp(
                "Note that providing `0` as the limit will set no limit for the server.",
                true
        )
        this.cooldown = 20
        this.cooldownScope = CooldownScope.USER_GUILD
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(!event.args.matches(Regex("\\d+")))
            return event.replyError(INVALID_ARGS_ERROR.format("Try specifying a limit in the form of a number."))
        val limit = event.args.toInt()
        if(limit==0) {
            event.removeRoleMeLimit()
            event.replySuccess("RoleMe limit was removed!")
        } else {
            event.setRoleMeLimit(limit)
            event.replySuccess("RoleMe limit was set to `$limit`!")
        }
    }

    fun CommandEvent.setRoleMeLimit(limit: Int) = this.manager.setLimit(this.guild, "RoleMe", limit)

    fun CommandEvent.removeRoleMeLimit() = this.manager.removeLimit(this.guild, "RoleMe")
}