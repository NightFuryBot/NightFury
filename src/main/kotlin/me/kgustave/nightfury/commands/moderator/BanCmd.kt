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
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.banFrom
import me.kgustave.nightfury.extensions.formattedName
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Mention a user or provide a user ID to ban.")
class BanCmd : Command()
{
    init {
        this.name = "Ban"
        this.arguments = "[@User or ID] <Reason>"
        this.help = "Bans a user from the server."
        this.botPermissions = arrayOf(Permission.BAN_MEMBERS)
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val parsed = event.modSearch()?:return

        val id = parsed.first
        val reason = parsed.second

        event.jda.retrieveUserById(id).promise() then { target ->
            // If no user is found we just respond and end.
            if(target == null) return@then event.replyError("Could not find a user matching \"${event.args}\"!")

            // Error Responses
            val error = when
            {
                event.selfUser == target
                        -> "I cannot ban myself from the server!"

                event.author == target
                        -> "You cannot ban yourself from the server!"

                event.guild.owner.user == target
                        -> "You cannot ban ${target.formattedName(true)} because they are the owner of the server!"

                event.guild.isMember(target) && !event.selfMember.canInteract(event.guild.getMember(target))
                        -> "I cannot ban ${target.formattedName(true)}!"

                event.guild.isMember(target) && !event.member.canInteract(event.guild.getMember(target))
                        -> "You cannot ban ${target.formattedName(true)}!"

                else    -> null
            }
            if(error!=null) return@then event.replyError(error)

            if(reason != null) {
                target.banFrom(event.guild, 1, reason)
            } else {
                target.banFrom(event.guild, 1)
            }.promise() then {
                if(reason != null) event.client.logger.newBan(event.member, target, reason)
                else               event.client.logger.newBan(event.member, target)
                event.replySuccess("${target.formattedName(true)} was banned from the server.")
            } catch {
                event.replyError("Banning ${target.formattedName(true)} failed for an unexpected reason!")
            }
        } catch { event.replyError("An unexpected error occurred when finding a user with ID: $id!") }
    }
}