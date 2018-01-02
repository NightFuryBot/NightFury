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
package xyz.nightfury.commands.admin

import xyz.nightfury.extensions.findRoles
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.entities.embed
import xyz.nightfury.entities.then
import xyz.nightfury.extensions.*
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.NoBaseExecutionCommand
import xyz.nightfury.db.SQLModeratorRole
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
        this.documentation =
            "You can use this command to manage moderators on the server.\n\n" +

            "`|Add` can be used to add a member as a moderator, while `|Remove` " +
            "can be used to remove one.\n" +
            "Server moderation is an integral part of communities in discord.\n" +
            "Check out the links below for more info on how to properly structure " +
            "your server's permission hierarchy and roles.\n\n" +

            "https://support.discordapp.com/hc/en-us/articles/211056628--Video-Make-an-Admin-Mod-and-Private-Channel-with-Discord-Permissions\n" +
            "https://support.discordapp.com/hc/en-us/articles/206141927-How-is-the-permission-hierarchy-structured-\n" +
            "https://support.discordapp.com/hc/en-us/articles/206029707-How-do-I-set-up-Permissions-"
        this.children = arrayOf(
                ModeratorListBaseCmd.ModeratorListCmd(),
                ModeratorOnlineCmd(),

                ModeratorAddCmd(),
                ModeratorRemoveCmd(),
                ModeratorSetCmd()
        )
    }
}

@MustHaveArguments("Specify a member to give the moderator role.")
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
        val modRole = SQLModeratorRole.getRole(event.guild)
                ?: return event.replyError("**Moderator role has not been set!**\n${SEE_HELP.format(event.client.prefix, fullname)}")

        val parsed = event.modSearch()?:return

        val id = parsed.first
        val reason = parsed.second

        val target = event.guild.getMemberById(id)?:return event.replyError("Could not find a member matching \"${event.args}\"!")

        // Error Responses
        val error = when
        {
            target.roles.contains(modRole)
            -> "I cannot make ${target.user.formattedName(true)} a moderator because they are already a Moderator!"
            !event.selfMember.canInteract(modRole)
            -> "I cannot give **${modRole.name}** to ${target.user.formattedName(true)} " +
                    "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.giveRole(modRole).apply { if(reason!=null) reason(reason) } then {
            event.replySuccess("${target.user.formattedName(true)} was added as a Moderator!")
        } catch {
            event.replyError("Adding ${target.user.formattedName(true)} as a Moderator failed for an unexpected reason!")
        }
    }
}

@MustHaveArguments("Specify a member with the moderator role to remove it from.")
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
        val modRole = SQLModeratorRole.getRole(event.guild)
                ?: return event.replyError("**Moderator role has not been set!**\n${SEE_HELP.format(event.client.prefix, fullname)}")

        val parsed = event.modSearch()?:return

        val id = parsed.first
        val reason = parsed.second

        val target = event.guild.getMemberById(id)?:return event.replyError("Could not find a member matching \"${event.args}\"!")

        // Error Responses
        val error = when
        {
            !target.roles.contains(modRole)
            -> "I cannot remove the moderator role from ${target.user.formattedName(true)} because they do not have it!"
            !event.selfMember.canInteract(modRole)
            -> "I cannot remove the moderator role from **${modRole.name}** to ${target.user.formattedName(true)} " +
                    "because it is a higher role I can interact with!"
            else    -> null
        }
        if(error!=null) return event.replyError(error)

        target.removeRole(modRole).apply { if(reason!=null) reason(reason) } then {
            event.replySuccess("The moderator role was removed from ${target.user.formattedName(true)}!")
        } catch {
            event.replyError("Removing ${target.user.formattedName(true)} as a Moderator failed for an unexpected reason!")
        }
    }
}

@MustHaveArguments("Specify a role to use as the moderator role.")
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
        val found = event.guild findRoles query
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(found multipleRoles query)
        val requested = found[0]
        val mod = SQLModeratorRole.getRole(event.guild)
        if(mod!=null && mod == requested)
            return event.replyError("**${requested.name}** is already the moderator role for this server!")
        SQLModeratorRole.setRole(requested)
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
        val modRole = SQLModeratorRole.getRole(event.guild)

        val mods = guild.members.stream()
                .filter { !(it.user.isBot) && (it.isOwner || it.isAdmin || (modRole!=null && it.roles.contains(modRole))) }
                .sorted(Comparator.comparing(Member::getJoinDate))
                .toList()

        event.reply(embed {
            title { "**Moderators On ${event.guild.name}**" }
            color = event.selfMember.color
            mods.forEach {
                append("${event.jda.getEmoteById(it.onlineStatus.emoteId).asMention} ")
                append(it.user.formattedName(true))
                when
                {
                    it.isOwner -> appendln(" `[OWNER]`")
                    it.isAdmin -> appendln(" `[ADMIN]`")
                    else ->       appendln(" `[ MOD ]`")
                }
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
        val modRole = SQLModeratorRole.getRole(event.guild)

        val mods = guild.members.stream()
                .filter { !(it.user.isBot) && (it.isOwner || it.isAdmin || (modRole!=null && it.roles.contains(modRole)))
                        && it.onlineStatus == OnlineStatus.ONLINE }
                .sorted(Comparator.comparing(Member::getJoinDate))
                .toList()

        if(mods.isEmpty())
            return event.replyError("**No Moderators Online!**\nFor a full list of Moderators, use `${event.client.prefix}moderator list`.")

        event.reply(embed {
            title { "**Moderators On ${event.guild.name}**" }
            color = event.selfMember.color
            mods.forEach {
                append("${event.jda.getEmoteById(it.onlineStatus.emoteId).asMention} ")
                append(it.user.formattedName(true))
                when
                {
                    it.isOwner -> appendln(" `[OWNER]`")
                    it.isAdmin -> appendln(" `[ADMIN]`")
                    else ->       appendln(" `[ MOD ]`")
                }
            }
            footer { this.value = "Total ${mods.size}" }
        })
    }
}