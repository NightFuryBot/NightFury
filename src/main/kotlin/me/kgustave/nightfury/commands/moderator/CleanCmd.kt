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
import java.util.regex.Pattern
import kotlin.collections.HashSet

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Provide a number between 2-1000, or a flag to delete by!")
class CleanCmd : Command()
{
    companion object {
        private val reasonPattern = Regex("(^.+)\\s(?:for\\s+)([\\s\\S]+)$", RegexOption.DOT_MATCHES_ALL).toPattern()
        private val userID: Pattern = Regex("(\\d{17,20})").toPattern()
        private val userMention: Pattern = Regex("<@!?(\\d{17,20})>").toPattern()
        private val numberPattern = Regex("(\\d{1,4})").toPattern()

        private val linkPattern = Regex("https?:\\/\\/\\S+").toPattern()
        private val quotePattern = Regex("\"(.*?)\"", RegexOption.DOT_MATCHES_ALL).toPattern()
    }

    init {
        this.name = "Clean"
        this.aliases = arrayOf("clear", "prune")
        this.arguments = "[Number of Messages]"
        this.help = "Deletes a specified number of messages from the channel this is called in."
        this.helpBiConsumer = standardSubHelp(
                        "This cleans the channel it is called in of up to 1000 messages that are less than " +
                        "two weeks old.\n\n" +

                        "Several flags can be specified to delete messages by, and are listed below:\n\n" +

                        "`userID` or `@user` - Only deletes messages by a user.\n" +
                        "`bots` - Only deletes messages by bots.\n" +
                        "`embeds` - Only deletes messages containing embeds.\n" +
                        "`links` - Only deletes messages containing links.\n" +
                        "`files` - Only deletes messages containing file uploads.\n" +
                        "`images` - Only deletes messages containing images uploads.\n\n" +

                        "It's worth noting the order above is the exact order of which this command discerns " +
                        "what to delete by. This in term, prevents flags from being combined to specify past " +
                        "their original intent.\n" +
                        "An example is that by using the `bots` flag, you will inevitably delete all `files` " +
                        "uploaded by that bot. However, using the flags in combination (`bots files`) will not " +
                        "produce an effect where only messages from bots containing files are deleted.\n\n" +

                        "As a final note, discord prevents the bulk deletion of messages older than 2 weeks by " +
                        "bots. As a result, NightFury, nor any other bot, is able to bulk clean a channel of " +
                        "messages that were sent two weeks prior to the command being used."
        )
        this.cooldown = 15
        this.cooldownScope = CooldownScope.CHANNEL
        this.category = Category.MODERATOR
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)
    }

    val Message.hasImage : Boolean
        get() = when {
            attachments.stream().anyMatch { it.isImage }                           -> true
            embeds.stream().anyMatch { it.image != null || it.videoInfo != null }  -> true
            else                                                                   -> false
        }

    override fun execute(event: CommandEvent)
    {
        var args = event.args

        val reasonMatcher = reasonPattern.matcher(args)

        val reason : String? = if(reasonMatcher.matches()) {
            args = reasonMatcher.group(1)
            reasonMatcher.group(2)?:null
        } else null


        val quotes : MutableSet<String> = HashSet()

        // Specific text
        val quotesMatcher = quotePattern.matcher(args)
        while(quotesMatcher.find())
            quotes.add(quotesMatcher.group(1).trim().toLowerCase())
        args = args.replace(quotePattern.toRegex(), "").trim()

        val ids : MutableSet<Long> = HashSet()

        // Mentions
        event.message.mentionedUsers.forEach { ids.add(it.idLong) }
        args = args.replace(userMention.toRegex(), "").trim()

        // Raw ID's
        val idsMatcher = userID.matcher(args)
        while(idsMatcher.find())
            ids.add(idsMatcher.group(1).trim().toLong())
        args = args.replace(userID.toRegex(), "").trim()

        // Bots Flag
        val bots = if(args.contains("bots", true)) {
            args = args.replace("bots", "", true).trim()
            true
        } else false

        // Embeds Flag
        val embeds = if(args.contains("embeds", true)) {
            args = args.replace("embeds", "", true)
            true
        } else false

        // Links Flag
        val links = if(args.contains("links", true)) {
            args = args.replace("links", "", true)
            true
        } else false

        // Images Flag
        val images = if(args.contains("images", true)) {
            args = args.replace("images", "", true)
            true
        } else false

        // Files Flag
        val files = if(args.contains("files", true)) {
            args = args.replace("files", "", true)
            true
        } else false

        // Checks to clean all
        val cleanAll = quotes.isEmpty() && ids.isEmpty() && !bots && !embeds && !links && !images && !files

        // Number of messages to delete
        val numMatcher = numberPattern.matcher(args.trim())
        val num : Int = if(numMatcher.find()) {
            val n = numMatcher.group(1).trim().toInt()
            if(n<2 || n>1000)
                return event.replyError(INVALID_ARGS_ERROR.format(
                        "The number of messages to delete must be between 2 and 1000!"))
            else n + 1
        } else if(!cleanAll) {
            100
        } else
            return event.replyError(INVALID_ARGS_ERROR.format("`${event.args}` is not a valid number of messages!"))

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

        // Filter based on flags
        for(message in messages) if(!message.creationTime.isBefore(twoWeeksPrior)) when {

            // Get right away if we're cleaning all
            cleanAll                                                     -> toDelete.add(message)

            ids.contains(message.author.idLong)                          -> toDelete.add(message)
            bots && message.author.isBot                                 -> toDelete.add(message)
            embeds && message.embeds.isNotEmpty()                        -> toDelete.add(message)
            links && linkPattern.matcher(message.rawContent).find()      -> toDelete.add(message)

            // Files comes before images because images are files
            files && message.attachments.isNotEmpty()                    -> toDelete.add(message)
            images && message.hasImage                                   -> toDelete.add(message)

            quotes.any { message.rawContent.toLowerCase().contains(it) } -> toDelete.add(message)

        } else {
            pastTwoWeeks = true
            break
        }

        if(toDelete.isEmpty()) // If it's empty, either nothing fit the criteria or all of it was past 2 weeks
            return event.replyError("**No messages found to delete!**\n" +
                if(pastTwoWeeks) "Messages older than 2 weeks cannot be deleted!"
                else SEE_HELP.format(event.client.prefix,this.name.toLowerCase()))

        val numDeleted = toDelete.size

        try {
            var i = 0
            while(i<numDeleted) // Delet this
            {
                if(i+100>numDeleted)
                    if(i+1==numDeleted) toDelete[numDeleted-1].delete().complete()
                    else event.textChannel.deleteMessages(toDelete.subList(i, numDeleted)).complete()
                else event.textChannel.deleteMessages(toDelete.subList(i, i+100)).complete()
                i+=100
            }
            with(event.client.logger)
            {
                if(reason != null) newClean(event.member, event.textChannel, numDeleted, reason)
                else               newClean(event.member, event.textChannel, numDeleted)
            }
            event.replySuccess("Successfully cleaned $numDeleted messages!")
        } catch (e : Exception) {
            // If something happens, we want to make sure that we inform them because
            // messages may have already been deleted.
            event.replyError("An error occurred when deleting $numDeleted messages!")
        }
    }
}

private class CleanSelfCmd : Command()
{
    init {
        this.name = "Self"
        this.fullname = "Clean Self"
        this.help = "Cleans messages sent in bulk."
        this.guildOnly = true
        this.devOnly = true
        this.category = Category.MONITOR
    }

    override fun execute(event: CommandEvent)
    {
        event.textChannel.history.retrievePast(100).complete()
    }
}
