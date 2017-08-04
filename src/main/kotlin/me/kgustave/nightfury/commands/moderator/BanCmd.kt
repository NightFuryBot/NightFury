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
import me.kgustave.nightfury.utils.TARGET_ID_REASON
import me.kgustave.nightfury.utils.TARGET_MENTION_REASON
import me.kgustave.nightfury.utils.formattedName
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
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

        event.jda.retrieveUserById(id).promise() then {
            // If no user is found we just respond and end.
            if(it == null) return@then event.replyError("Could not find a user matching \"$args\"!")

            // Error Responses
            val error = when
            {
                event.selfUser == it
                        -> "I cannot ban myself from the server!"

                event.author == it
                        -> "You cannot ban yourself from the server!"

                event.guild.owner.user == it
                        -> "You cannot ban ${it.formattedName(true)} because they are the owner of the server!"

                event.guild.isMember(it) && !event.selfMember.canInteract(event.guild.getMember(it))
                        -> "I cannot ban ${it.formattedName(true)}!"

                event.guild.isMember(it) && !event.member.canInteract(event.guild.getMember(it))
                        -> "You cannot ban ${it.formattedName(true)}!"

                else    -> null
            }
            if(error!=null) return@then event.replyError(error)

            val target = it
            if(reason != null) {
                it.banFrom(event.guild, 1, reason)
            } else {
                it.banFrom(event.guild, 1)
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