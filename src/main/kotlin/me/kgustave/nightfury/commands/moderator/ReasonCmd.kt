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

import club.minnced.kjda.entities.isSelf
import club.minnced.kjda.promise
import me.kgustave.nightfury.Argument
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.entities.Case

/**
 * @author Kaidan Gustave
 */
class ReasonCmd : Command() {

    companion object {
        val invalid_case_number = "**Invalid case number!**\n"
    }

    init {
        this.name = "reason"
        this.arguments = Argument("<case number> [reason]")
        this.help = "updates a reason for a moderation case"
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        val modLog = event.client.manager.getModLog(event.guild)
                ?: return event.replyError("The moderator log channel has not been set!")
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, name))
        val parts = event.args.split(Regex("\\s+"),2)
        val case : Case
        val number : Int
        val reason : String
        // Only one argument or first argument is not a number
        if(parts.size==1 || !parts[0].matches(Regex("\\d{1,5}")))
        {
            case = event.client.manager.getFirstCaseMatching(event.guild, {
                it.modId == event.author.idLong && it.reason == Case.default_case_reason
            }).takeIf { it.number != 0 } ?: return event.replyError("You have no outstanding cases!")
            number = case.number
            reason = event.args
        }
        else
        {
            number = with(parts[0].toInt()) {
                if(this>event.client.manager.getCases(event.guild).size)
                    return event.replyError("${invalid_case_number}Specify a case number lower than the latest case number!")
                else this
            }
            case = event.client.manager.getCaseMatching(event.guild, { it.number == number})
            reason = parts[1].trim()
        }

        if(case.modId!=event.author.idLong)
            return event.replyError("**You are not responsible for case number `$number`!**\n" +
                    "Only the moderator responsible for a case may update it's reason.")
        if(reason.length>300)
            return event.replyError("Reasons must not be longer than 300 characters!")
        if(case.messageId==0L)
            return event.replyError("**The message for this case could not be found!**\n" +
                    "This may be because the original case log message was deleted, or never sent at all!")

        case.reason = reason
        modLog.getMessageById(case.messageId).promise() then {
            if(it == null)
                return@then event.replyError("An unexpected error occurred while updating the reason for case number `$number`!")
            if(it.author.isSelf)
                it.editMessage("${it.rawContent.split(Regex("\n"),2)[0]}\n`[ REASON ]` $reason").queue()
            event.client.manager.updateCase(case)
            event.replySuccess("Successfully updated reason for case number `$number`!")
        } catch {
            event.replyError("An unexpected error occurred while updating the reason for case number `$number`!")
        }
    }
}