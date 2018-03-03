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
package xyz.nightfury.command.moderator

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageHistory
import xyz.nightfury.NightFury
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.entities.await
import xyz.nightfury.listeners.ModLog
import xyz.nightfury.util.discordID
import xyz.nightfury.util.reasonPattern
import xyz.nightfury.util.userMention
import java.util.*

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class CleanCommand : Command(ModeratorGroup) {
    companion object {
        private const val MAX_RETRIEVABLE = 100

        private val numberPattern = Regex("(\\d{1,4})")
        private val linkPattern = Regex("https?://\\S+")
        private val quotePattern = Regex("\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
    }

    override val name = "Clean"
    override val aliases = arrayOf("Clear", "Prune")
    override val arguments = "[Flags]"
    override val help = "Cleans messages from a channel."
    override val botPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)

    override suspend fun execute(ctx: CommandContext) {
        var args = ctx.args

        // Reason
        val reasonMatcher = reasonPattern.matchEntire(args)
        val reason: String? = if(reasonMatcher != null) {
            val groups = reasonMatcher.groupValues
            args = groups[1]
            groups[2]
        } else null

        val quotes = HashSet<String>()
        val ids = HashSet<Long>()

        // Specific text
        quotePattern.findAll(args).forEach { quotes.add(it.groupValues[1].trim().toLowerCase()) }
        args = quotePattern.replace(args, "").trim()

        // Mentions
        ctx.message.mentionedUsers.forEach { ids.add(it.idLong) }
        args = args.replace(userMention, "").trim()

        // Raw ID's
        val idsMatcher = discordID.findAll(args)
        for(res in idsMatcher)
            ids.add(res.groupValues[1].trim().toLong())
        args = args.replace(discordID, "").trim()

        // Bots Flag
        val bots = args.contains("bots", true)
        if(bots) args = args.replace("bots", "", true).trim()

        // Embeds Flag
        val embeds = args.contains("embeds", true)
        if(embeds) args = args.replace("embeds", "", true)

        // Links Flag
        val links = args.contains("links", true)
        if(links) args = args.replace("links", "", true)

        // Images Flag
        val images = args.contains("images", true)
        if(images) args = args.replace("images", "", true)

        // Files Flag
        val files = args.contains("files", true)
        if(files) args = args.replace("files", "", true)

        // Checks to clean all
        val cleanAll = quotes.isEmpty() && ids.isEmpty() && !bots && !embeds && !links && !images && !files

        // Number of messages to delete
        val numMatcher = numberPattern.findAll(args.trim())
        val num = if(numMatcher.any()) {
            val n = numMatcher.first().value.trim().toInt()
            if(n < 2 || n > 1000) return ctx.invalidArgs {
                "The number of messages to delete must be between 2 and 1000!"
            } else n + 1
        } else if(!cleanAll) 100 else return ctx.invalidArgs {
            "`${ctx.args}` is not a valid number of messages!"
        }

        val twoWeeksPrior = ctx.message.creationTime.minusWeeks(2).plusMinutes(1)

        val channel = ctx.textChannel
        val messages = channel.history.getPast(num, breakIf = {
            it[it.size - 1].creationTime.isBefore(twoWeeksPrior)
        })

        messages.remove(ctx.message) // Remove call message
        var pastTwoWeeks = false

        // Get right away if we're cleaning all
        val toDelete = if(cleanAll) messages else toDelete@ {
            val toDelete = LinkedList<Message>()
            // Filter based on flags
            for(message in messages) {
                if(!message.creationTime.isBefore(twoWeeksPrior)) {
                    when {
                        ids.contains(message.author.idLong)                          -> toDelete.add(message)
                        bots && message.author.isBot                                 -> toDelete.add(message)
                        embeds && message.embeds.isNotEmpty()                        -> toDelete.add(message)
                        links && linkPattern.containsMatchIn(message.contentRaw)     -> toDelete.add(message)

                        // Files comes before images because images are files
                        files && message.attachments.isNotEmpty()                    -> toDelete.add(message)
                        images && message.hasImage                                   -> toDelete.add(message)

                        quotes.any { message.contentRaw.toLowerCase().contains(it) } -> toDelete.add(message)
                    }
                } else {
                    pastTwoWeeks = true
                    break
                }
            }
            return@toDelete toDelete
        }

        // If it's empty, either nothing fit the criteria or all of it was past 2 weeks
        if(toDelete.isEmpty()) return ctx.replyError {
            if(pastTwoWeeks) "Messages older than 2 weeks cannot be deleted!"
            else "Found no messages to delete!"
        }

        val numDeleted = toDelete.size

        try {
            var i = 0
            while(i < numDeleted) { // Delet this
                if(i + MAX_RETRIEVABLE > numDeleted) {
                    if(i + 1 == numDeleted) toDelete[numDeleted - 1].delete().await()
                    else channel.deleteMessages(toDelete.subList(i, numDeleted)).await()
                } else channel.deleteMessages(toDelete.subList(i, i + 100)).await()

                i += MAX_RETRIEVABLE
            }

            ModLog.newClean(ctx.member, channel, numDeleted, reason)
            ctx.sendSuccess("Successfully cleaned $numDeleted messages!")
            ctx.invokeCooldown()
        } catch (e: Exception) {
            NightFury.LOG.error("An error occurred", e)
            // If something happens, we want to make sure that we inform them because
            // messages may have already been deleted.
            ctx.replyError("An error occurred when deleting $numDeleted messages!")
        }
    }

    private inline val Message.hasImage: Boolean inline get() {
        return attachments.any { it.isImage } || embeds.any {
            it.image !== null || it.videoInfo !== null
        }
    }

    private suspend inline fun MessageHistory.getPast(
        number: Int,
        breakIf: (List<Message>) -> Boolean
    ): MutableList<Message> {
        require(number > 0) { "Minimum of one message must be retrieved" }

        if(number <= MAX_RETRIEVABLE) {
            return retrievePast(number).await() ?: mutableListOf()
        }

        val list = LinkedList<Message>()
        var left = number

        while(left > MAX_RETRIEVABLE) {
            list += retrievePast(MAX_RETRIEVABLE).await() ?: break
            left -= MAX_RETRIEVABLE
            if(breakIf(list)) {
                left = 0
                break
            }
        }

        if(left in 1..MAX_RETRIEVABLE) {
            retrievePast(left).await()?.let { list += it }
        }

        return list
    }

}
