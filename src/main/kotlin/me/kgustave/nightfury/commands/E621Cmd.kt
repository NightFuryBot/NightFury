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

import com.jagrosh.jdautilities.menu.slideshow.SlideshowBuilder
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.*
import me.monitor.je621.E621Array
import me.monitor.je621.JE621
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class E621Cmd(val e621 : JE621, val waiter: EventWaiter, val random: Random = Random()) : Command() {

    companion object {
        val NSFW = Category("NSFW", { Category.OWNER.test(it) || (it.isFromType(ChannelType.TEXT) && it.textChannel.isNSFW) })
    }

    init {
        this.name = "e621"
        this.arguments = Argument("<number of posts> [tags...]")
        this.aliases = arrayOf("pron","porn")
        this.guildOnly = true
        this.category = NSFW
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        this.cooldown = 15
        this.cooldownScope = CooldownScope.USER_GUILD
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, name))
        val args = event.args.split(Regex("\\s+"))
        if(args.isEmpty())
            return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, name))
        if(args[0].matches(Regex("\\d+"))) {
            val limit = args[0].toInt()
            val tags = args
            if(tags.isEmpty())
                return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, name))
            if(tags.size>6)
                return event.replyError("**Too many tags specified!**\nPlease specify no more than 6 tags!")
            e621.startNewSearch().addTags(*args.toTypedArray()).setMaxRetrieved(limit).search().queue({ generate(it, event) })
        } else {
            val tags = args
            if(tags.isEmpty())
                return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, name))
            if(tags.size>6)
                return event.replyError("**Too many tags specified!**\nPlease specify no more than 6 tags!")
            e621.startNewSearch().addTags(*args.toTypedArray()).setMaxRetrieved(100).search().queue({ generate(it, event) })
        }
    }
    private fun generate(array: E621Array, event: CommandEvent)
    {
        with(SlideshowBuilder())
        {
            setUrls(*array.arrayList.stream().map { it.fileUrl }.toList().toTypedArray())
            setText("Showing results for ${event.args}")
            setColor { _,_ -> Color(random.nextInt(256),random.nextInt(256),random.nextInt(256)) }
            setDescription { x, _ -> "[Link](https://e621.net/post/show/${array[x].id}/)"}
            setFinalAction { m ->
                waiter.waitForEvent(MessageReceivedEvent::class.java, { e : MessageReceivedEvent ->
                    e.author == event.author && e.channel == event.channel
                            && (e.message.rawContent == "${event.prefixUsed}save" || e.message.rawContent == "${event.client.prefix}save")
                }, {
                    event.replySuccess("Saved Picture!")
                }, 20, TimeUnit.SECONDS, {
                    m.delete().queue()
                })
            }
                    .
            setTimeout<SlideshowBuilder>(30, TimeUnit.SECONDS)
            setUsers<SlideshowBuilder>(event.author)
            setEventWaiter<SlideshowBuilder>(waiter)
        }.build().display(event.channel)
    }
}