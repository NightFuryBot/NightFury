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
package xyz.nightfury.commands.moderator

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLCases
import xyz.nightfury.db.SQLModeratorLog
import xyz.nightfury.entities.Case
import xyz.nightfury.entities.then
import xyz.nightfury.extensions.edit
import xyz.nightfury.resources.Arguments
import xyz.nightfury.extensions.isSelf

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Provide a reason to give or specify a case number followed by a reason.")
class ReasonCmd : Command() {
    companion object {
        private val reasonRegex: Regex = Regex("\\d{1,5}")
        private val reasonSplit: Regex = Regex("\n")
    }

    init {
        this.name = "Reason"
        this.arguments = "<Case Number> [Reason]"
        this.help = "Updates a reason for a moderation case."
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val modLog = SQLModeratorLog.getChannel(event.guild)
                ?: return event.replyError("The moderator log channel has not been set!")
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.client.prefix, name))
        val parts = event.args.split(Arguments.commandArgs,2)
        val case : Case
        val number : Int
        val reason : String
        // Only one argument or first argument is not a number
        if(parts.size==1 || !(parts[0] matches reasonRegex)) {
            val cases = SQLCases.getCasesByUser(event.member).takeIf { it.isNotEmpty() }
                    ?: return event.replyError("You have no outstanding cases!")
            case = cases[0]
            number = case.number
            reason = event.args
        } else {
            number = with(parts[0].toInt()) {
                if(this > SQLCases.getCases(event.guild).size)
                    return event.replyError("**Invalid case number!**\n" +
                            "Specify a case number lower than the latest case number!")
                else this
            }

            case = SQLCases.getCaseNumber(event.guild, number)
                    ?: return event.replyError("An error occurred getting case number $number!")

            reason = parts[1].trim()
        }

        if(case.modId!=event.author.idLong)
            return event.replyError("**You are not responsible for case number `$number`!**\n" +
                    "Only the moderator responsible for a case may update it's reason.")
        if(reason.length>200)
            return event.replyError("Reasons must not be longer than 200 characters!")
        if(case.messageId==0L)
            return event.replyError("**The message for this case could not be found!**\n" +
                    "This may be because the original case log message was deleted, or never sent at all!")

        case.reason = reason
        modLog.getMessageById(case.messageId) then {
            if(this == null)
                return@then event.replyError("An unexpected error occurred while updating the reason for case number `$number`!")
            if(author.isSelf)
                this.edit { "${contentRaw.split(reasonSplit,2)[0]}\n`[ REASON ]` $reason" }
            SQLCases.updateCase(case)
            event.replySuccess("Successfully updated reason for case number `$number`!")
        } catch {
            event.replyError("An unexpected error occurred while updating the reason for case number `$number`!")
        }
    }
}