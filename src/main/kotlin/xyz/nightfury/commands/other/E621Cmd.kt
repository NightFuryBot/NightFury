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
package xyz.nightfury.commands.other

import xyz.nightfury.entities.menus.Slideshow
import xyz.nightfury.entities.menus.EventWaiter
import xyz.nightfury.annotations.APICache
import xyz.nightfury.annotations.AutoInvokeCooldown
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.requests.E621API
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.json.JSONArray
import org.json.JSONObject
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.extensions.randomNextInt
import java.awt.Color
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@APICache
@AutoInvokeCooldown
@MustHaveArguments("Provide up to 6 tags or a number of posts followed by the tags.")
class E621Cmd(val e621 : E621API, val waiter: EventWaiter) : Command()
{
    private val builder: Slideshow.Builder = Slideshow.Builder()
        .waiter           { waiter }
        .showPageNumbers  { false }
        .waitOnSinglePage { true }

    init {
        this.name = "E621"
        this.arguments = "<Number of Posts> [Tags... (maximum of 6)]"
        this.aliases = arrayOf("pron","porn")
        this.help = "Searches e621.net for images matching the tags specified."
        this.cooldown = 15
        this.cooldownScope = CooldownScope.USER_GUILD
        this.guildOnly = true
        this.category = Category.NSFW
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        val args = event.args.split(Regex("\\s+"))
        if(args[0] matches Regex("\\d+")) {
            val limit = args[0].toInt()
            val tags = args.subList(1, args.size)
            if(tags.isEmpty())
                return event.replyError("**No tags specified!**\nPlease specify at least one tag after the number of posts!")
            if(tags.size>6)
                return event.replyError("**Too many tags specified!**\nPlease specify no more than 6 tags!")
            val arr = e621.search(limit, *args.toTypedArray())
                    ?:return event.replyError("Found no results matching ${buildString { args.forEach { append("$it ") } }.trim()}")
            if(arr.length()==0)
                return event.replyError("No results found matching ${buildString { args.forEach { append("$it ") } }.trim()}!")
            generate(arr,event)
        } else {
            if(args.isEmpty())
                return event.replyError("**No tags specified!**\nPlease specify at least one tag!")
            if(args.size>6)
                return event.replyError("**Too many tags specified!**\nPlease specify no more than 6 tags!")
            val arr = e621.search(100, *args.toTypedArray())
                    ?:return event.replyError("Found no results matching ${event.args}")
            if(arr.length()==0)
                return event.replyError("No results found matching ${event.args}!")
            generate(arr,event)
        }
    }

    private fun generate(array: JSONArray, event: CommandEvent) {
        val list = array.toTypedList<JSONObject>()
        with(builder) {
            clearUrls()

            // Populate menu
            list.forEach { add { it["file_url"] as String } }

            text        { _,_ -> "Showing results for ${event.args}" }
            color       { _, _ -> Color(0 randomNextInt 256, 0 randomNextInt 256, 0 randomNextInt 256) }
            description { x, _ -> "[Link](https://e621.net/post/show/${list[x]["id"]}/)" }

            finalAction { m ->
                event.reply("To save this, type `|save`") { message ->
                    waiter.waitFor<MessageReceivedEvent>(
                        delay = 20,
                        unit = TimeUnit.SECONDS,
                        timeout = {
                            message.delete().queue()
                            m.delete().queue()
                        },
                        condition = {
                            it.author == event.author && it.channel == event.channel &&
                            (
                                it.message.contentRaw == "${event.client.prefix}save" ||
                                it.message.contentRaw == "${event.client.prefix}save"
                            )
                        },
                        action = {
                            message.delete().queue()
                            event.message.delete().queue()
                            m.clearReactions().queue()
                        }
                    )
                }
            }

            timeout {
                delay { 30 }
                unit  { TimeUnit.SECONDS }
            }

            user { event.author }
            displayIn { event.channel }
        }
    }

    inline fun <reified T> JSONArray.toTypedList() : List<T> = map { it as T }

    @APICache
    @Suppress("unused")
    fun clearCache() = e621.clearCache()
}
