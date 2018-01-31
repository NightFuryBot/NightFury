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

import kotlinx.coroutines.experimental.launch
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.resources.Arguments
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import xyz.nightfury.*
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.entities.ModLogger
import xyz.nightfury.entities.await
import xyz.nightfury.extensions.getPast
import java.util.*
import kotlin.collections.HashSet

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@MustHaveArguments("Provide a number between 2-1000, or a flag to delete by!")
class CleanCmd : Command() {
    companion object {
        private val numberPattern = Regex("(\\d{1,4})")
        private val linkPattern = Regex("https?://\\S+")
        private val quotePattern = Regex("\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
    }

    init {
        this.name = "Clean"
        this.aliases = arrayOf("clear", "prune")
        this.arguments = "[Number of Messages]"
        this.help = "Deletes a specified number of messages from the channel this is called in."
        this.cooldown = 15
        this.cooldownScope = CooldownScope.CHANNEL
        this.category = Category.MODERATOR
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)
    }

    private val Message.hasImage: Boolean
        get() = when {
            attachments.stream().anyMatch { it.isImage } -> true
            embeds.stream().anyMatch { it.image != null || it.videoInfo != null } -> true
            else -> false
        }

    override fun execute(event: CommandEvent) {
        var args = event.args

        // Reason
        val reasonMatcher = Arguments.reasonPattern.matchEntire(args)
        val reason: String? = if(reasonMatcher != null) {
            val groups = reasonMatcher.groupValues
            args = groups[1]
            groups[2]
        } else null

        val quotes = HashSet<String>()

        // Specific text
        quotePattern.findAll(args).forEach { quotes.add(it.groupValues[1].trim().toLowerCase()) }
        args = quotePattern.replace(args, "").trim()

        val ids = HashSet<Long>()

        // Mentions
        event.message.mentionedUsers.forEach { ids.add(it.idLong) }
        args = args.replace(Arguments.userMention, "").trim()

        // Raw ID's
        val idsMatcher = Arguments.discordID.findAll(args)
        for(res in idsMatcher)
            ids.add(res.groupValues[1].trim().toLong())
        args = args.replace(Arguments.discordID, "").trim()

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
        val numMatcher = numberPattern.findAll(args.trim())
        val num: Int = if(numMatcher.any()) {
            val n = numMatcher.first().value.trim().toInt()
            if(n < 2 || n > 1000)
                return event.replyError(INVALID_ARGS_ERROR.format("The number of messages to delete must be between 2 and 1000!"))
            else n + 1
        } else if(!cleanAll) {
            100
        } else return event.replyError(INVALID_ARGS_ERROR.format("`${event.args}` is not a valid number of messages!"))

        val twoWeeksPrior = event.message.creationTime.minusWeeks(2).plusMinutes(1)

        launch {
            val messages = event.textChannel.history.getPast(num, breakIf = {
                it[it.size - 1].creationTime.isBefore(twoWeeksPrior)
            })

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
                links && linkPattern.containsMatchIn(message.contentRaw)     -> toDelete.add(message)

                // Files comes before images because images are files
                files && message.attachments.isNotEmpty()                    -> toDelete.add(message)
                images && message.hasImage                                   -> toDelete.add(message)

                quotes.any { message.contentRaw.toLowerCase().contains(it) } -> toDelete.add(message)
            } else {
                pastTwoWeeks = true
                break
            }

            if(toDelete.isEmpty()) { // If it's empty, either nothing fit the criteria or all of it was past 2 weeks
                return@launch event.replyError(
                    "**No messages found to delete!**\n" +
                    if(pastTwoWeeks) "Messages older than 2 weeks cannot be deleted!"
                    else SEE_HELP.format(event.client.prefix,name.toLowerCase())
                )
            }

            val numDeleted = toDelete.size

            try {
                var i = 0
                while(i < numDeleted) { // Delet this
                    if(i + 100 > numDeleted) {
                        if(i + 1==numDeleted)
                            toDelete[numDeleted - 1].delete().await()
                        else
                            event.textChannel.deleteMessages(toDelete.subList(i, numDeleted)).await()
                    } else {
                        event.textChannel.deleteMessages(toDelete.subList(i, i + 100)).await()
                    }

                    i += 100
                }

                if(reason != null)
                    ModLogger.newClean(event.member, event.textChannel, numDeleted, reason)
                else
                    ModLogger.newClean(event.member, event.textChannel, numDeleted)

                event.replySuccess("Successfully cleaned $numDeleted messages!") {
                    event.invokeCooldown()
                }
            } catch (e: Exception) {
                NightFury.LOG.error("An error occurred", e)
                // If something happens, we want to make sure that we inform them because
                // messages may have already been deleted.
                event.replyError("An error occurred when deleting $numDeleted messages!")
            }
        }
    }
}
