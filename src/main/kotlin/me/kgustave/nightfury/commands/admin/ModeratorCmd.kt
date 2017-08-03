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
package me.kgustave.nightfury.commands.admin

import club.minnced.kjda.builders.colorAwt
import club.minnced.kjda.builders.embed
import club.minnced.kjda.promise
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.*
import me.kgustave.nightfury.utils.*
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import java.util.Comparator
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class ModeratorCmd : NoBaseExecutionCommand()
{
    init {
        this.name = "Moderator"
        this.aliases = arrayOf("mod")
        this.arguments = "[Add|Remove|Set]"
        this.help = "Add, remove, and manage moderators for this ser"
        this.guildOnly = true
        this.category = Category.ADMIN
        this.children = arrayOf(
                ModeratorAddCmd(),
                ModeratorListBaseCmd.ModeratorListCmd(),
                ModeratorOnlineCmd(),
                ModeratorRemoveCmd(),
                ModeratorSetCmd()
        )
    }
}

@MustHaveArguments
private class ModeratorAddCmd : Command()
{
    init {
        this.name = "Add"
        this.fullname = "Moderator Add"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Gives a member the moderator role."
        this.guildOnly = true
        this.category = Category.ADMIN
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override fun execute(event: CommandEvent)
    {
        val modRole = event.manager.getModRole(event.guild)
                ?: return event.replyError("**Moderator role has not been set!**\n${SEE_HELP.format(event.prefixUsed, fullname)}")

        val args = event.args
        val targetId = TARGET_ID_REASON.matcher(args)
        val targetMention = TARGET_MENTION_REASON.matcher(args)

        val id : String =
                if(targetId.matches()) targetId.group(1).trim()
                else if(targetMention.matches()) targetMention.group(1).trim()
                else return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, fullname))
        val reason : String? =
                if(targetId.matches()) targetId.group(2)?.trim()
                else if(targetMention.matches()) targetMention.group(2)?.trim()
                else return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, fullname))

        val target = event.guild.getMemberById(id) ?: return event.replyError("Could not find a member matching \"$args\"!")

        // Error Responses
        val error = when
        {
            target.roles.contains(modRole)
            -> "I cannot make ${formatUserName(target.user,true)} a moderator because they are already a Moderator!"
            !event.selfMember.canInteract(modRole)
            -> "I cannot give **${modRole.name}** to ${formatUserName(target.user,true)} " +
                    "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.giveRole(modRole).apply { if(reason!=null) reason(reason) }.promise() then {
            event.replySuccess("${formatUserName(target.user,true)} was added as a Moderator!")
        } catch {
            event.replyError("Adding ${formatUserName(target.user,true)} as a Moderator failed for an unexpected reason!")
        }
    }
}

@MustHaveArguments
private class ModeratorRemoveCmd : Command()
{
    init {
        this.name = "Remove"
        this.fullname = "Moderator Remove"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Removes a moderator's mod role."
        this.guildOnly = true
        this.category = Category.ADMIN
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override fun execute(event: CommandEvent)
    {
        val modRole = event.manager.getModRole(event.guild)
                ?: return event.replyError("**Moderator role has not been set!**\n${SEE_HELP.format(event.prefixUsed, fullname)}")

        val args = event.args
        val targetId = TARGET_ID_REASON.matcher(args)
        val targetMention = TARGET_MENTION_REASON.matcher(args)

        val id : String =
                if(targetId.matches()) targetId.group(1).trim()
                else if(targetMention.matches()) targetMention.group(1).trim()
                else return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, fullname))
        val reason : String? =
                if(targetId.matches()) targetId.group(2)?.trim()
                else if(targetMention.matches()) targetMention.group(2)?.trim()
                else return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, fullname))

        val target = event.guild.getMemberById(id) ?: return event.replyError("Could not find a member matching \"$args\"!")

        // Error Responses
        val error = when
        {
            !target.roles.contains(modRole)
            -> "I cannot remove the moderator role from ${formatUserName(target.user,true)} because they do not have it!"
            !event.selfMember.canInteract(modRole)
            -> "I cannot remove the moderator role from **${modRole.name}** to ${formatUserName(target.user,true)} " +
                    "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.removeRole(modRole).apply { if(reason!=null) reason(reason) }.promise() then {
            event.replySuccess("The moderator role was removed from ${formatUserName(target.user,true)}!")
        } catch {
            event.replyError("Removing ${formatUserName(target.user,true)} as a Moderator failed for an unexpected reason!")
        }
    }
}

@MustHaveArguments
private class ModeratorSetCmd : Command()
{
    init {
        this.name = "Set"
        this.fullname = "Moderator Set"
        this.arguments = "[Role]"
        this.help = "Sets the server's moderator role."
        this.guildOnly = true
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
        val mod = event.manager.getModRole(event.guild)
        if(mod!=null && mod == requested)
            return event.replyError("**${requested.name}** is already the moderator role for this server!")
        event.manager.setModRole(requested)
        if(event.selfMember.canInteract(requested)) {
            event.replySuccess("**${requested.name}** was set as the moderator role for this server!")
        } else
            event.replyWarning("**${requested.name}** was set as the moderator role for this server!\n" +
                    "Please be aware that due to role hierarchy positioning, I will not be able to give this role to members!\n" +
                    "To fix this, make sure my I have a role higher than `${requested.name}` on the roles list.")
    }
}

abstract class ModeratorListBaseCmd : Command()
{
    init {
        this.help = "Lists moderators on the server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val guild = event.guild
        val modRole = event.manager.getModRole(event.guild)

        val mods = guild.members.stream()
                .filter { !(it.user.isBot) && (it.isOwner || it.isAdmin || (modRole!=null && it.roles.contains(modRole))) }
                .sorted(Comparator.comparing(Member::getJoinDate))
                .toList()

        event.reply(embed {
            title { "**Moderators On ${event.guild.name}**" }
            colorAwt = event.selfMember.color
            mods.forEach {
                append("${event.jda.getEmoteById(statusEmote(it.onlineStatus)).asMention} ")
                append(formatUserName(it.user, true))
                if(it.isOwner)      appendln(" `[OWNER]`")
                else if(it.isAdmin) appendln(" `[ADMIN]`")
                else                appendln(" `[ MOD ]`")
            }
            footer { this.value = "Total ${mods.size}" }
        })
    }

    class ModeratorListCmd : ModeratorListBaseCmd()
    {
        init {
            this.name = "List"
            this.fullname = "Moderator List"
        }
    }

    class ServerModeratorsCmd : ModeratorListBaseCmd()
    {
        init {
            this.name = "Moderators"
            this.fullname = "Server Moderators"
            this.aliases = arrayOf("mods")
        }
    }
}

private class ModeratorOnlineCmd : Command()
{
    init {
        this.name = "Online"
        this.fullname = "Moderator Online"
        this.help = "Lists online moderators on the server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val guild = event.guild
        val modRole = event.manager.getModRole(event.guild)

        val mods = guild.members.stream()
                .filter { !(it.user.isBot) && (it.isOwner || it.isAdmin || (modRole!=null && it.roles.contains(modRole)))
                        && it.onlineStatus == OnlineStatus.ONLINE }
                .sorted(Comparator.comparing(Member::getJoinDate))
                .toList()

        if(mods.isEmpty())
            return event.replyError("**No Moderators Online!**\nFor a full list of Moderators, use `${event.prefixUsed}moderator list`.")

        event.reply(embed {
            title { "**Moderators On ${event.guild.name}**" }
            colorAwt = event.selfMember.color
            mods.forEach {
                append("${event.jda.getEmoteById(statusEmote(it.onlineStatus)).asMention} ")
                append(formatUserName(it.user, true))
                if(it.isOwner)      appendln(" `[OWNER]`")
                else if(it.isAdmin) appendln(" `[ADMIN]`")
                else                appendln(" `[ MOD ]`")
            }
            footer { this.value = "Total ${mods.size}" }
        })
    }
}