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
package xyz.nightfury.command.administrator

import net.dv8tion.jda.core.Permission
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.EmptyCommand
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.util.*
import xyz.nightfury.util.db.announcementChannel
import xyz.nightfury.util.db.announcementRoles
import xyz.nightfury.util.db.hasAnnouncementsChannel
import xyz.nightfury.util.db.isAnnouncements
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.jda.findRoles
import xyz.nightfury.util.jda.findTextChannels
import xyz.nightfury.util.jda.mentionable

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a role and a message in the format `%prefix%name %arguments`")
class AnnouncementCommand : Command(AdministratorGroup) {
    override val name = "Announcement"
    override val arguments = "[Role] [Message]"
    override val help = "Make an announcement to a role on the server."
    override val botPermissions = arrayOf(
        Permission.MANAGE_PERMISSIONS,
        Permission.MANAGE_ROLES
    )
    override val children = arrayOf<Command>(
        AnnouncementChannelCommand(),
        AnnouncementRoleCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val splitArgs = ctx.args.split(commandArgs, 2)

        if(splitArgs.size < 2) return ctx.missingArgs {
            "Specify a role and a channel in the format `$arguments`!"
        }

        val announcementChannel = ctx.guild.announcementChannel ?: return ctx.replyError {
            "This server does not have an announcement channel!"
        }

        if(!announcementChannel.canTalk()) return ctx.replyError {
            "I cannot make announcements in ${announcementChannel.asMention} because " +
            "I do not have permission to send messages there!"
        }

        // It's easier to filter based on which are and are not by getting them all at once
        // The only other option is making a DB query on each of the roles found which can
        // get really intense on the machine if it has to check 100 roles or something.
        val announcementRoles = ctx.guild.announcementRoles
        val foundRoles = ctx.guild.findRoles(splitArgs[0])
        val matchingRoles = foundRoles.mapNotNull { it.takeIf { it in announcementRoles } }

        val match = when {
            matchingRoles.isEmpty() -> return ctx.replyError(noMatch("announcement roles", splitArgs[0]))
            matchingRoles.size > 1 -> return ctx.replyError(matchingRoles.multipleRoles(splitArgs[0]))
            else -> matchingRoles[0]
        }

        val manager = if(!match.isMentionable) {
            if(!ctx.selfMember.canInteract(match)) return ctx.replyError {
                "I cannot interact with **${match.name}**!"
            }

            // We need to make it mentionable first
            match.managerUpdatable.apply {
                mentionable { true }
                ignored { update().await() }
            }
        } else null

        announcementChannel.sendMessage("${match.asMention}\n${splitArgs[1]}").await()

        manager?.let {
            ignored { manager.mentionable { false }.update().queue() }
        }

        ctx.replySuccess("Successfully made announcement in ${announcementChannel.asMention}!")
    }

    private inner class AnnouncementChannelCommand : EmptyCommand(this@AnnouncementCommand) {
        override val name = "Channel"
        override val help = "Configure the announcement channel for the server."
        override val children = arrayOf(
            AnnouncementChannelRemoveCommand(),
            AnnouncementChannelSetCommand()
        )

        private inner class AnnouncementChannelRemoveCommand : Command(this@AnnouncementChannelCommand) {
            override val name = "Remove"
            override val help = "Removes the server's announcement channel."

            override suspend fun execute(ctx: CommandContext) {
                if(!ctx.guild.hasAnnouncementsChannel) return ctx.replyError {
                    "This server does not have an announcement channel to remove!"
                }

                ctx.guild.announcementChannel = null
                ctx.replySuccess("Successfully removed this server's announcement channel!")
            }
        }

        @MustHaveArguments("Specify a channel to use.")
        private inner class AnnouncementChannelSetCommand :  Command(this@AnnouncementChannelCommand) {
            override val name = "Set"
            override val arguments = "[Channel]"
            override val help = "Sets the server's announcement channel."

            override suspend fun execute(ctx: CommandContext) {
                val args = ctx.args
                val found = ctx.guild.findTextChannels(args)

                val target = when {
                    found.isEmpty() -> return ctx.replyError(noMatch("channels", args))
                    found.size > 1 -> return ctx.replyError(found.multipleTextChannels(args))
                    else -> found[0]
                }

                if(ctx.guild.announcementChannel == target) return ctx.replyError {
                    "${target.asMention} is already this server's announcement channel!"
                }

                if(!target.canTalk()) return ctx.replyError {
                    "Cannot use ${target.asMention} because I cannot send messages there!"
                }

                ctx.guild.announcementChannel = target

                ctx.replySuccess("Successfully set this channel's announcement channel as ${target.asMention}!")
            }
        }
    }

    private inner class AnnouncementRoleCommand : EmptyCommand(this@AnnouncementCommand) {
        override val name = "Role"
        override val help = "Configure announcement roles for the server."
        override val botPermissions = arrayOf(
            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_ROLES
        )
        override val children = arrayOf(
            AnnouncementRoleAddCommand(),
            AnnouncementRoleRemoveCommand()
        )

        @MustHaveArguments("Specify a role to add.")
        private inner class AnnouncementRoleAddCommand : Command(this@AnnouncementRoleCommand) {
            override val name = "Add"
            override val arguments = "[Role]"
            override val help = "Adds a new announcement role."

            override suspend fun execute(ctx: CommandContext) {
                val args = ctx.args
                val found = ctx.guild.findRoles(args)

                val target = when {
                    found.isEmpty() -> return ctx.replyError(noMatch("roles", args))
                    found.size > 1 -> return ctx.replyError(found.multipleRoles(args))
                    else -> found[0]
                }

                if(target.isAnnouncements) return ctx.replyError {
                    "**${target.name}** is already an announcements role!"
                }

                target.isAnnouncements = true

                ctx.replySuccess("Successfully made **${target.name}** an announcements role!")
            }
        }

        @MustHaveArguments("Specify an announcement role to remove.")
        private inner class AnnouncementRoleRemoveCommand : Command(this@AnnouncementRoleCommand) {
            override val name = "Remove"
            override val arguments = "[Role]"
            override val help = "Removes an announcement role."

            override suspend fun execute(ctx: CommandContext) {
                val args = ctx.args
                val found = ctx.guild.findRoles(args)

                val target = when {
                    found.isEmpty() -> return ctx.replyError(noMatch("roles", args))
                    found.size > 1 -> return ctx.replyError(found.multipleRoles(args))
                    else -> found[0]
                }

                if(!target.isAnnouncements) return ctx.replyError {
                    "**${target.name}** is not an announcements role!"
                }

                target.isAnnouncements = false

                ctx.replySuccess("Successfully removed announcement role **${target.name}**!")
            }
        }
    }
}
