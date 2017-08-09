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
import me.kgustave.kjdautils.utils.findRoles
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.*
import me.kgustave.nightfury.utils.*
import net.dv8tion.jda.core.Permission
import java.awt.Color

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class MuteCmd : Command() {

    init {
        this.name = "Mute"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Mutes a user."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
        this.category = Category.MODERATOR
        this.children = arrayOf(SetupMuteCmd(), SetMutedRoleCmd())
    }

    override fun execute(event: CommandEvent)
    {
        val mutedRole = event.client.manager.getMutedRole(event.guild)
                ?:return event.replyError("**Muted role has not been setup!**\n" +
                "Try using `${event.prefixUsed}mute setup` to create a new mute role, or `${event.prefixUsed}mute set` to " +
                "register an existing one!")

        val args = event.args
        val targetId = TARGET_ID_REASON.matcher(args)
        val targetMention = TARGET_MENTION_REASON.matcher(args)

        val id : String =
                if(targetId.matches()) targetId.group(1).trim()
                else if(targetMention.matches()) targetMention.group(1).trim()
                else return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, name))
        val reason : String? =
                if(targetId.matches()) targetId.group(2)?.trim()
                else if(targetMention.matches()) targetMention.group(2)?.trim()
                else return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, name))

        val target = event.guild.getMemberById(id) ?: return event.replyError("Could not find a member matching \"$args\"!")

        // Error Responses
        val error = when
        {
            event.selfMember == target
                    -> "I cannot mute myself!"
            event.author == target
                    -> "You cannot mute yourself!"
            event.guild.owner.user == target
                    -> "You cannot mute ${target.user.formattedName(true)} because they are the owner of the server!"
            target.roles.contains(mutedRole)
                    -> "I cannot mute ${target.user.formattedName(true)} because they are already muted!"
            !event.selfMember.canInteract(mutedRole)
                    -> "I cannot give **${mutedRole.name}** to ${target.user.formattedName(true)}" +
                       "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.giveRole(mutedRole).apply { if(reason!=null) reason(reason) }.promise() then {
            if(reason != null) event.client.logger.newMute(event.member, target.user, reason)
            else               event.client.logger.newMute(event.member, target.user)
            event.replySuccess("${target.user.formattedName(true)} was muted!")
        } catch {
            event.replyError("Muting ${target.user.formattedName(true)} failed for an unexpected reason!")
        }
    }
}

private class SetupMuteCmd : Command()
{
    init {
        this.name = "setup"
        this.fullname = "Mute Setup"
        this.arguments = "<Name of Muted Role>"
        this.help = "Creates a muted role for this server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        if(event.client.manager.getMutedRole(event.guild)!=null)
            return event.replyError("**Muted role already exists on this server!**\n" +
                    "To change it, use `${event.prefixUsed}mute set`!")
        else {
            event.guild.controller.createRole() promise {
                name = if(event.args.isEmpty()) "Muted" else event.args
                color = Color.BLACK
            } then {
                if(it != null)
                event.replySuccess("Successfully created muted role: **${it.name}**!")
            } catch { event.replyError("An error occurred while creating the role!") }
        }
    }
}

@MustHaveArguments
private class SetMutedRoleCmd : Command()
{
    init {
        this.name = "Set"
        this.fullname = "Mute Set"
        this.arguments = "[Role]"
        this.help = "Sets the muted role for this server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
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
        val muted = event.client.manager.getMutedRole(event.guild)
        if(muted!=null && muted == requested)
            return event.replyError("**${requested.name}** is already the muted role for this server!")
        event.client.manager.setMutedRole(requested)
        if(event.selfMember.canInteract(requested)) {
            event.guild.refreshMutedRole(requested)
            event.replySuccess("**${requested.name}** was set as the muted role for this server!")
        } else
            event.replyWarning("**${requested.name}** was set as the muted role for this server!\n" +
                    "Please be aware that due to role hierarchy positioning, I will not be able to give this role to members!\n" +
                    "To fix this, make sure my I have a role higher than `${requested.name}` on the roles list.")
    }
}