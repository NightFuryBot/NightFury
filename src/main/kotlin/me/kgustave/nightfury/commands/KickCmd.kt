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
package me.kgustave.nightfury.commands

import club.minnced.kjda.promise
import me.kgustave.nightfury.Argument
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.extensions.kick
import me.kgustave.nightfury.utils.TARGET_ID_REASON
import me.kgustave.nightfury.utils.TARGET_MENTION_REASON
import me.kgustave.nightfury.utils.formatUserName
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
class KickCmd : Command() {

    init {
        this.name = "kick"
        this.arguments = Argument("[@user or ID] <reason>")
        this.help = "kicks a user from the server"
        this.botPermissions = arrayOf(Permission.KICK_MEMBERS)
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

        val target = event.guild.getMemberById(id) ?: return event.replyError("Could not find a member matching \"$args\"!")

        // Error Responses
        val error = when
        {
            event.selfMember == target
                    -> "I cannot kick myself from the server!"

            event.author == target
                    -> "You cannot kick yourself from the server!"

            event.guild.owner.user == target
                    -> "You cannot kick ${formatUserName(target.user,true)} because they are the owner of the server!"

            !event.selfMember.canInteract(target)
                    -> "I cannot kick ${formatUserName(target.user,true)} because they have a higher role than me!"

            !event.member.canInteract(target)
                    -> "You cannot kick ${formatUserName(target.user,true)} because they have a higher role than you!"

            else    -> null
        }

        if(error!=null) return event.replyError(error)

        if(reason != null) {
            target.kick(reason)
        } else {
            target.kick()
        }.promise() then {
            event.replySuccess("${formatUserName(target.user,true)} was kicked from the server.")
        } catch {
            event.replyError("Kicking ${formatUserName(target.user,true)} failed for an unexpected reason!")
        }
    }
}