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
import me.kgustave.nightfury.Argument
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.extensions.Find
import me.kgustave.nightfury.extensions.promise
import me.kgustave.nightfury.extensions.giveRole
import me.kgustave.nightfury.extensions.refreshMutedRole
import me.kgustave.nightfury.utils.*
import net.dv8tion.jda.core.Permission
import java.awt.Color

/**
 * @author Kaidan Gustave
 */
class MuteCmd : Command() {

    init {
        this.name = "mute"
        this.arguments = Argument("[@user or ID] <reason>")
        this.help = "mutes a user"
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
                    -> "You cannot mute ${formatUserName(target.user,true)} because they are the owner of the server!"
            target.roles.contains(mutedRole)
                    -> "I cannot mute ${formatUserName(target.user,true)} because they are already muted!"
            !event.selfMember.canInteract(mutedRole)
                    -> "I cannot give **${mutedRole.name}** to ${formatUserName(target.user,true)}" +
                       "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.giveRole(mutedRole).apply { if(reason!=null) reason(reason) }.promise() then {
            if(reason != null) event.client.logger.newMute(target, event.author, reason)
            else               event.client.logger.newMute(target, event.author)
            event.replySuccess("${formatUserName(target.user,true)} was muted!")
        } catch {
            event.replyError("Muting ${formatUserName(target.user,true)} failed for an unexpected reason!")
        }
    }
}

private class SetupMuteCmd : Command()
{
    init {
        this.name = "setup"
        this.arguments = Argument("<name of muted role>")
        this.help = "creates a muted role for this server"
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

private class SetMutedRoleCmd : Command()
{
    init
    {
        this.name = "set"
        this.fullname = "mute set"
        this.arguments = Argument("<role>")
        this.help = "sets the muted role for this server"
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        if(query.isEmpty())
            return event.replyError(Command.TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val found = Find.roles(query, event.guild)
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