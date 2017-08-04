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

import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.annotations.MustHaveArguments
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import java.util.*

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Provide a number between 2-1000!")
class CleanCmd : Command()
{
    companion object {
        private val number_pattern = Regex("(\\d{1,4})").toPattern()
    }

    init {
        this.name = "Clean"
        this.aliases = arrayOf("clear", "prune")
        this.arguments = "[Number of Messages]"
        this.help = "Deletes a specified number of messages from the channel this is called in. **[BETA]**"
        this.cooldown = 15
        this.cooldownScope = CooldownScope.CHANNEL
        this.category = Category.MODERATOR
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        val matcher = number_pattern.matcher(event.args)
        val num : Int
        if(matcher.matches()) {
            val n = matcher.group(1).trim().toInt()
            if(n<2 || n>1000)
                return event.replyError(INVALID_ARGS_ERROR.format("The number of messages to delete must be between 2 and 1000!"))
            else num = n + 1
        } else return event.replyError(INVALID_ARGS_ERROR.format("`${event.args}` is not a valid number of messages!"))

        val history = event.textChannel.history
        val messages = LinkedList<Message>()
        var left = num
        val twoWeeksPrior = event.message.creationTime.minusWeeks(2).plusMinutes(1)
        while(left>100)
        {
            messages.addAll(history.retrievePast(100).complete())
            left -= 100
            if(messages[messages.size-1].creationTime.isBefore(twoWeeksPrior))
            {
                left = 0
                break
            }
        }
        if(left>0) messages.addAll(history.retrievePast(left).complete())

        messages.remove(event.message) // Remove call message

        val toDelete = LinkedList<Message>()
        var pastTwoWeeks = false
        for(message in messages)
            if(message.creationTime.isBefore(twoWeeksPrior)) {
                pastTwoWeeks = true
                break
            } else toDelete.add(message)
        if(toDelete.isEmpty())
            return event.replyError("**No messages found to delete!**\n${
                if(pastTwoWeeks) "Messages older than 2 weeks cannot be deleted!"
                else SEE_HELP.format(event.client.prefix,this.name.toLowerCase())
            }")
        val numDeleted = toDelete.size
        try {
            var i = 0
            while(i<numDeleted)
            {
                if(i+100>numDeleted)
                    if(i+1==numDeleted) toDelete[numDeleted-1].delete().complete()
                    else event.textChannel.deleteMessages(toDelete.subList(i, numDeleted)).complete()
                else event.textChannel.deleteMessages(toDelete.subList(i, i+100)).complete()
                i+=100
            }
            event.client.logger.newClean(event.member, event.textChannel, numDeleted)
        } catch (e : Exception) {
            return event.replyError("An error occurred when deleting $numDeleted messages!")
        }
    }
}