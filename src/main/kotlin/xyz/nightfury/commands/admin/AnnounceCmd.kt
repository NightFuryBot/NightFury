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
package xyz.nightfury.commands.admin

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLAnnouncementChannel
import xyz.nightfury.db.SQLAnnouncementRoles
import xyz.nightfury.extensions.*
import xyz.nightfury.resources.Arguments

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class AnnounceCmd : Command() {
    init {
        this.name = "Announce"
        this.aliases = arrayOf("Announcement")
        this.arguments = "[Role] [Message]"
        this.help = "Mentions the requested role in the announcements channel with an important message."
        this.guildOnly = true
        this.category = Category.ADMIN
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
        this.children = arrayOf(
            AnnounceAddRoleCmd(),
            AnnounceChannelCmd(),
            AnnounceRemoveRoleCmd()
        )
    }

    override fun execute(event: CommandEvent) {
        val channel = SQLAnnouncementChannel.getChannel(event.guild)
                      ?: return event.replyError("**No announcements channel has been set!**\n" +
                                                 "Use `$name channel` to set the announcements channel for this server!")

        if(!channel.canTalk()) {
            return event.replyError("I cannot announce to ${channel.asMention} because I do not have permission to " +
                                    "send messages there!")
        }

        val split = event.args.split(Arguments.commandArgs, 2)

        if(split.size < 2) {
            return event.replyError(TOO_FEW_ARGS_ERROR.format("Specify a role followed by a message to announce!"))
        }

        val roleQuery = split[0]
        val message = split[1]

        if(message.isEmpty()) {
            return event.replyError("Message was blank, you must specify a message after the role to announce to!")
        }

        val announcementRoles = SQLAnnouncementRoles.getRoles(event.guild)

        val found = event.guild.findRoles(roleQuery).filter { it in announcementRoles }

        if(found.isEmpty()) {
            return event.replyError(noMatch("announcement roles", roleQuery))
        } else if(found.size > 1) {
            return event.replyError(found.multipleRoles(roleQuery))
        }

        val toAnnouncement = found[0]

        if(toAnnouncement.isMentionable) {
            announce(event, toAnnouncement, channel, message)
        } else {
            toAnnouncement.edit {
                mentionable { true }
            } then {
                announce(event, toAnnouncement, channel, message)
                // Make unmentionable again
                toAnnouncement.edit { mentionable { false } }
            } catch {
                event.replyError("An error occurred when sending the announcement to ${channel.asMention}!")
            }
        }
    }

    private fun announce(event: CommandEvent, role: Role, channel: TextChannel, message: String) {
        channel.send {
            mention(role).appendln()
            append(message)
        } then {
            event.replySuccess("Successfully send announcement to ${channel.asMention}!")
        } catch {
            event.replyError("An error occurred when sending the announcement to ${channel.asMention}!")
        }
    }
}

@MustHaveArguments
private class AnnounceChannelCmd : Command() {
    init {
        this.name = "Channel"
        this.fullname = "Announce Channel"
        this.arguments = "[Channel]"
        this.help = "Sets the channel to send announcements to."
        this.guildOnly = true
        this.category = Category.SERVER_OWNER
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
    }

    override fun execute(event: CommandEvent) {
        val args = event.args

        val channels = event.guild.findTextChannels(args)

        if(channels.isEmpty()) {
            return event.replyError(noMatch("channels", args))
        } else if(channels.size > 1) {
            return event.replyError(channels.multipleTextChannels(args))
        }

        val channel = channels[0]

        SQLAnnouncementChannel.getChannel(event.guild).let {
            if(it != null) {
                if(it == channel) {
                    return event.replyError("${it.asMention} is already the announcement channel for this server!")
                } else if(!it.canTalk()) {
                    return event.replyError("${it.asMention} cannot be set as the announcement channel because " +
                                            "I do not have permission to send messages to it!")
                }
            }
        }

        SQLAnnouncementChannel.setChannel(channel)
        event.replySuccess("Successfully set ${channel.asMention} as the server's announcement channel!")
    }

}

@MustHaveArguments
private class AnnounceAddRoleCmd : Command() {
    init {
        this.name = "AddRole"
        this.fullname = "Announcement AddRole"
        this.aliases = arrayOf("Add")
        this.arguments = "[Role]"
        this.help = "Adds a role as an announcement role for the server."
        this.guildOnly = true
        this.category = Category.SERVER_OWNER
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
    }

    override fun execute(event: CommandEvent) {
        val args = event.args

        val roles = event.guild.findRoles(args)

        if(roles.isEmpty()) {
            return event.replyError(noMatch("roles", args))
        } else if(roles.size > 1) {
            return event.replyError(roles.multipleRoles(args))
        }

        val role = roles[0]

        if(SQLAnnouncementRoles.isRole(role)) {
            return event.replyError("${role.name} is already an announcement role.")
        }

        if(!event.selfMember.canInteract(role)) {
            return event.replyError("I cannot add ${role.name} because I cannot interact with it.")
        }

        SQLAnnouncementRoles.addRole(role)
        event.replySuccess("Successfully added ${role.name} as an announcement role!")
    }
}

@MustHaveArguments
private class AnnounceRemoveRoleCmd : Command() {
    init {
        this.name = "RemoveRole"
        this.fullname = "Announcement RemoveRole"
        this.aliases = arrayOf("Remove", "DeleteRole", "Delete")
        this.arguments = "[Role]"
        this.help = "Removes a role as an announcement role for the server."
        this.guildOnly = true
        this.category = Category.SERVER_OWNER
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
    }

    override fun execute(event: CommandEvent) {
        val args = event.args

        val roles = event.guild.findRoles(args)

        if(roles.isEmpty()) {
            return event.replyError(noMatch("roles", args))
        } else if(roles.size > 1) {
            return event.replyError(roles.multipleRoles(args))
        }

        val role = roles[0]

        if(!SQLAnnouncementRoles.isRole(role)) {
            return event.replyError("${role.name} is not an announcement role.")
        }

        SQLAnnouncementRoles.addRole(role)
        event.replySuccess("Successfully removed ${role.name} as an announcement role!")
    }
}