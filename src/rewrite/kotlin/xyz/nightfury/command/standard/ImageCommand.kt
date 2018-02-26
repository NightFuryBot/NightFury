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
package xyz.nightfury.command.standard

import net.dv8tion.jda.core.Permission
import xyz.nightfury.NightFury
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.requests.GoogleImageAPI
import xyz.nightfury.util.Cleanable
import xyz.nightfury.util.commandArgs
import xyz.nightfury.util.ext.await
import xyz.nightfury.util.ext.embed
import xyz.nightfury.util.ext.message

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify what to search for.")
class ImageCommand(private val api: GoogleImageAPI) : Command(StandardGroup), Cleanable by api {
    override val name = "Image"
    override val aliases = arrayOf("Img")
    override val arguments = "[Search Query]"
    override val help = "Searches for an image."
    override val guildOnly = false
    override val cooldown = 30
    override val cooldownScope = CooldownScope.USER
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        ctx.channel.sendTyping().await()
        val results = api.search(query)
        when {
            results === null  -> ctx.replyError("An unexpected error occurred while searching!")
            results.isEmpty() -> ctx.replyError("No results were found for \"**$query**\"!")
            else              -> ctx.reply(message {
                append { "${NightFury.SUCCESS} ${ctx.author.asMention}" }
                embed {
                    if(ctx.isGuild) {
                        color { ctx.member.color }
                    }
                    image { selectResultURL(query, results) }
                }
            })
        }
        ctx.invokeCooldown()
    }

    private fun selectResultURL(query: String, results: List<String>): String {
        // Start with last index of the results
        var index = results.size - 1

        // Subtract the length of the query
        index -= query.length

        // If the index has fallen below or is at 0 return the first result
        if(index <= 0)
            return results[0]

        // If there is more than 2 spaces, divide the results by the number of them
        val spaces = query.split(commandArgs).size
        if(spaces > 2)
            index /= spaces - 1

        // Once again, grab first index if it's below or at 0
        if(index <= 0)
            return results[0]

        // return a random result between first index and the calculated maximum
        return results[(Math.random() * (index)).toInt()]
    }
}
