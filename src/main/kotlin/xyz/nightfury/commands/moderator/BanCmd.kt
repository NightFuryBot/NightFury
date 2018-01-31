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
package xyz.nightfury.commands.moderator

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.entities.promise
import xyz.nightfury.extensions.banFrom
import xyz.nightfury.extensions.formattedName
import net.dv8tion.jda.core.Permission
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.entities.ModLogger

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@MustHaveArguments("Mention a user or provide a user ID to ban.")
class BanCmd : Command() {
    init {
        this.name = "Ban"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Bans a user from the server."
        this.botPermissions = arrayOf(Permission.BAN_MEMBERS)
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val parsed = event.modSearch() ?: return

        val id = parsed.first
        val reason = parsed.second

        event.jda.retrieveUserById(id).promise() then { user ->
            // If no user is found we just respond and end.
            if(user == null)
                return@then event.replyError("Could not find a user matching \"${event.args}\"!")

            // Error Responses
            val error = when {
                event.selfUser == user
                        -> "I cannot ban myself from the server!"

                event.author == user
                        -> "You cannot ban yourself from the server!"

                event.guild.owner.user == user
                        -> "You cannot ban ${user.formattedName(true)} because they are the owner of the server!"

                event.guild.isMember(user) && !event.selfMember.canInteract(event.guild.getMember(user))
                        -> "I cannot ban ${user.formattedName(true)}!"

                event.guild.isMember(user) && !event.member.canInteract(event.guild.getMember(user))
                        -> "You cannot ban ${user.formattedName(true)}!"

                else    -> null
            }

            if(error != null)
                return@then event.replyError(error)

            user.banFrom(event.guild, 1, reason).promise() then {
                ModLogger.newBan(event.member, user, reason)
                event.replySuccess("${user.formattedName(true)} was banned from the server.")
            } catch {
                event.replyError("Banning ${user.formattedName(true)} failed for an unexpected reason!")
            }
        } catch {
            event.replyError("An unexpected error occurred when finding a user with ID: $id!")
        }
    }
}
