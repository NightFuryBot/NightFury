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
import com.jagrosh.jdautilities.utils.FinderUtil
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
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
class RoleMeCmd(waiter: EventWaiter) : Command()
{
    init {
        this.name = "roleme"
        this.arguments = "[role]"
        this.help = "give yourself or remove a RoleMe role"
        this.cooldown = 10
        this.guildOnly = true
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.children = arrayOf(
                RoleMeAddCmd(),
                RoleMeListCmd(waiter),
                RoleMeRemoveCmd()
        )
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        if(query.isEmpty())
            return event.replyError(Command.TOO_FEW_ARGS_HELP.format(event.prefixUsed, name))
        val allRolemes = event.manager.getRoleMes(event.guild)
        if(allRolemes.isEmpty())
            return event.replyError("**No RoleMe roles on this server!**\n${SEE_HELP.format(event.prefixUsed, name)}")
        val roles = FinderUtil.findRoles(query, event.guild)
        if(roles.isEmpty())
            return event.replyError(noMatch("roles", query))
        val rolemes = roles.stream().filter { event.manager.isRoleMe(it) }.toList()
        if(rolemes.isEmpty() && roles.isNotEmpty())
            return event.replyError("**${roles[0].name} is not a RoleMe role!**\n" +
                    SEE_HELP.format(event.prefixUsed, name))
        if(rolemes.size>1)
            return event.replyError(multipleRolesFound(query, rolemes))
        val requested = rolemes[0]
        if(!event.selfMember.canInteract(requested))
            event.replyError("**Cannot interact with requested role!**\n" +
                    SEE_HELP.format(event.prefixUsed, name))
        else if(!event.member.roles.contains(requested)) {
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
}

private class RoleMeAddCmd : Command()
{
    init {
        this.name = "add"
        this.fullname = "roleme add"
        this.arguments = "[role]"
        this.help = "adds a RoleMe role for the server"
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        if(query.isEmpty())
            return event.replyError(Command.TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val found = FinderUtil.findRoles(query, event.guild)
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

private class RoleMeRemoveCmd : Command()
{
    init {
        this.name = "remove"
        this.fullname = "roleme remove"
        this.arguments = "[role]"
        this.help = "removes a RoleMe role for the server"
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        if(query.isEmpty())
            return event.replyError(Command.TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val found = FinderUtil.findRoles(query, event.guild).stream()
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
        this.name = "list"
        this.fullname = "roleme list"
        this.help = "gets a full list of all the roleme roles on this server"
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        val rolemes = event.manager.getRoleMes(event.guild).map { it.name }
        if(rolemes.isEmpty())
            return event.replyError("**No RoleMe roles on this server!**\n${SEE_HELP.format(event.prefixUsed, name)}")
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