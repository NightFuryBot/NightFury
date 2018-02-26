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
package xyz.nightfury.commands.standard

import xyz.nightfury.entities.menus.Paginator
import xyz.nightfury.entities.menus.EventWaiter
import xyz.nightfury.annotations.AutoInvokeCooldown
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.entities.then
import net.dv8tion.jda.core.Permission
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.SQLLimits
import xyz.nightfury.db.SQLRoleMe
import xyz.nightfury.util.ext.*
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@MustHaveArguments("Specify a RoleMe role to give or remove!")
class RoleMeCmd(waiter: EventWaiter) : Command() {
    init {
        this.name = "RoleMe"
        this.arguments = "[Role]"
        this.help = "Give yourself or remove a RoleMe role."
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

    override fun execute(event: CommandEvent) {
        val query = event.args
        val allRoleMes = SQLRoleMe.getRoles(event.guild)
        if(allRoleMes.isEmpty())
            return event.replyError("**No RoleMe roles on this server!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        val roles = event.guild.findRoles(query)
        if(roles.isEmpty())
            return event.replyError(noMatch("roles", query))
        val roleMes = roles.stream().filter { SQLRoleMe.isRole(it) }.toList()
        if(roleMes.isEmpty() && roles.isNotEmpty())
            return event.replyError("**${roles[0].name} is not a RoleMe role!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        if(roleMes.size>1)
            return event.replyError(roleMes multipleRoles query)
        val requested = roleMes[0]
        if(!event.selfMember.canInteract(requested))
            event.replyError("**Cannot interact with requested role!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        else if(!event.member.roles.contains(requested)) {
            if(event.hasRoleMeLimit) {
                if(event.roleMeLimit<=event.member.roles.stream().filter { SQLRoleMe.isRole(it) }.count())
                    return event.replyError("More RoleMe roles cannot be added because you are at the limit set by the server!")
            }

            event.member.giveRole(requested) then {
                event.replySuccess("Successfully gave the role **${requested.name}**!")
                event.invokeCooldown()
            } catch {
                event.replyError("An error occurred while giving the role **${requested.name}**!")
            }
        } else {
            event.member.removeRole(requested) then {
                event.replySuccess("Successfully removed the role **${requested.name}**!")
                event.invokeCooldown()
            } catch {
                event.replyError("An error occurred while removing the role **${requested.name}**!")
            }
        }
    }
}

@MustHaveArguments("Specify a role to add to RoleMe!")
private class RoleMeAddCmd : Command() {
    init {
        this.name = "Add"
        this.fullname = "RoleMe Add"
        this.arguments = "[Role]"
        this.help = "Adds a RoleMe role for the server."
        this.documentation =
            "Roles added will be available to all members on the server via the `RoleMe` " +
            "command, except in cases that NightFury cannot give roles to or remove roles from " +
            "the member using the command!\n\n" +

            "**NightFury is not responsible for any dangerous permissions given with these," +
            "nor the consequences of the aforementioned!**"

        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent) {
        val query = event.args
        val found = event.guild.findRoles(query)
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(found multipleRoles query)
        val requested = found[0]
        if(SQLRoleMe.isRole(requested))
            return event.replyError("The role **${requested.name}** is already a RoleMe role!")
        SQLRoleMe.addRole(requested)
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
private class RoleMeRemoveCmd : Command() {
    init {
        this.name = "Remove"
        this.fullname = "RoleMe Remove"
        this.arguments = "[Role]"
        this.help = "Removes a RoleMe role for the server."
        this.documentation =
            "**Using this will not remove the RoleMe role from members who previously had it!**"
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent) {
        val query = event.args
        val found = event.guild.findRoles(query).stream()
                .filter { SQLRoleMe.isRole(it) }.toList()
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(found multipleRoles query)
        SQLRoleMe.deleteRole(found[0])
        event.replySuccess("The role **${found[0].name}** was removed from RoleMe!")
    }
}

@AutoInvokeCooldown
private class RoleMeListCmd(waiter: EventWaiter) : Command() {
    init {
        this.name = "List"
        this.fullname = "RoleMe List"
        this.help = "Gets a full list of RoleMe roles on this server."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
    }

    val builder : Paginator.Builder = Paginator.Builder()
            .timeout          { delay { 20 } }
            .showPageNumbers  { true }
            .numberItems      { true }
            .waitOnSinglePage { false }
            .waiter           { waiter }

    override fun execute(event: CommandEvent) {
        val rolemes = SQLRoleMe.getRoles(event.guild).map { it.name }
        if(rolemes.isEmpty())
            return event.replyError("**No RoleMe roles on this server!**\n" +
                    SEE_HELP.format(event.client.prefix, "RoleMe"))
        with(builder) {
            text        { _,_ -> "RoleMe Roles On ${event.guild.name}" }
            items       { addAll(rolemes) }
            finalAction { event.linkMessage(it) }
            user        { event.author }
            displayIn   { event.channel }
        }
    }
}

@MustHaveArguments("Try specifying a limit in the form of a number.")
private class RoleMeLimitCmd : Command() {
    init {
        this.name = "Limit"
        this.fullname = "RoleMe Limit"
        this.arguments = "[Number]"
        this.help = "Sets the limit of RoleMe roles a user can have on the server."
        this.documentation =
            "Note that providing `0` as the limit will set no limit for the server."
        this.cooldown = 20
        this.cooldownScope = CooldownScope.USER_GUILD
        this.category = Category.ADMIN
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        if(!(event.args matches Regex("\\d+")))
            return event.replyError(INVALID_ARGS_ERROR.format("Try specifying a limit in the form of a number."))
        val limit = event.args.toInt()
        event.roleMeLimit = limit
        if(limit==0) event.replySuccess("RoleMe limit was removed!")
        else         event.replySuccess("RoleMe limit was set to `$limit`!")
    }
}

var CommandEvent.roleMeLimit : Int
    set(value) = if(value == 0) SQLLimits.removeLimit(guild, "RoleMe") else SQLLimits.setLimit(guild, "RoleMe", value)
    get() = SQLLimits.getLimit(guild, "RoleMe")

val CommandEvent.hasRoleMeLimit : Boolean
    // FIXME Should have Guild parameter option
    get() = SQLLimits.hasLimit(guild.idLong, "RoleMe")
