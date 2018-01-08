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
package xyz.nightfury.entities.menus

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.requests.RestAction
import xyz.nightfury.entities.promise
import xyz.nightfury.entities.then
import xyz.nightfury.extensions.embed
import xyz.nightfury.extensions.message
import java.awt.Color
import kotlin.math.max

/**
 * Modeled after jagrosh's Slideshow in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
@Suppress("Unused")
class Slideshow(builder: Slideshow.Builder): Menu(builder) {
    companion object {
        const val BIG_LEFT = "\u23EA"
        const val LEFT = "\u25C0"
        const val STOP = "\u23F9"
        const val RIGHT = "\u25B6"
        const val BIG_RIGHT = "\u23E9"
    }

    private val color: (Int, Int) -> Color? = builder.colorFun
    private val text: (Int, Int) -> String? = builder.textFun
    private val description: (Int, Int) -> String? = builder.descriptionFun
    private val urls: List<String> = builder.urls
    private val showPageNumbers: Boolean = builder.showPageNumbers
    private val waitOnSinglePage: Boolean = builder.waitOnSinglePage
    private val bulkSkipNumber: Int = builder.bulkSkipNumber
    private val wrapPageEnds: Boolean = builder.wrapPageEnds
    private val leftText: String? = builder.textToLeft
    private val rightText: String? = builder.textToRight
    private val allowTextInput: Boolean = builder.allowTextInput

    private val pages = urls.size

    override fun displayIn(channel: MessageChannel) = paginate(channel, 1)

    override fun displayAs(message: Message) = paginate(message, 1)

    fun paginate(message: Message, pageNum: Int) {
        val num = if(pageNum < 1) 1 else if(pageNum > pages) pages else pageNum

        initialize(message.editMessage(renderPage(num)), num)
    }

    fun paginate(channel: MessageChannel, pageNum: Int) {
        val num = if(pageNum < 1) 1 else if(pageNum > pages) pages else pageNum

        initialize(channel.sendMessage(renderPage(num)), num)
    }

    private fun initialize(action: RestAction<Message?>, pageNum: Int) {
        action.promise() then { m ->
            m ?: return@then
            when {
                pages > 1 -> {
                    if(bulkSkipNumber > 1)
                        m.addReaction(BIG_LEFT).queue()
                    m.addReaction(LEFT).queue()
                    m.addReaction(STOP).queue()
                    if(bulkSkipNumber > 1)
                        m.addReaction(RIGHT).queue()
                    m.addReaction(if(bulkSkipNumber > 1) BIG_RIGHT else RIGHT) then {
                        pagination(m, pageNum)
                    } catch { pagination(m, pageNum) }
                }

                waitOnSinglePage -> {
                    m.addReaction(STOP) then { pagination(m, pageNum) } catch { pagination(m, pageNum) }
                }

                else -> finalAction(m)
            }
        }
    }

    private fun pagination(message: Message, pageNum: Int) {

        if(allowTextInput || leftText != null && rightText != null)
            paginationWithTextInput(message, pageNum)
        else
            paginationWithoutTextInput(message, pageNum)
    }

    private fun paginationWithTextInput(message: Message, pageNum: Int) {
        waiter.waitFor<GenericMessageEvent>(
            delay = timeout,
            unit = unit,
            timeout = { finalAction(message) },
            condition = { event ->
                if(event is MessageReactionAddEvent) {
                    return@waitFor checkReaction(event, message)
                } else if(event is MessageReceivedEvent) {
                    if(event.channel != message.channel)
                        return@waitFor false

                    val content = event.message.contentRaw
                    if(leftText != null && rightText != null) {
                        if(content.equals(leftText, ignoreCase = true) || content.equals(rightText, ignoreCase = true))
                            return@waitFor isValidUser(event.author, event.guild)
                    }

                    if(allowTextInput) {
                        try {
                            val i = content.toInt()
                            if(i in 1..pages && i != pageNum)
                                return@waitFor isValidUser(event.author, event.guild)
                        } catch(ignored: NumberFormatException) {}
                    }
                }

                // Default return false
                false
            },
            action = { event ->
                if(event is MessageReactionAddEvent) {
                    handleMessageReactionAddAction(event, message, pageNum)
                } else if(event is MessageReceivedEvent) {
                    val content = event.message.contentRaw

                    val targetPage: Int = when {
                        leftText != null && content.equals(leftText, true) && (1 < pageNum || wrapPageEnds) -> {
                            if(pageNum - 1 < 1 && wrapPageEnds) pages else pageNum - 1
                        }

                        rightText != null && content.equals(rightText, true) && (pageNum < pages || wrapPageEnds) -> {
                            if(pageNum + 1 > pages && wrapPageEnds) 1 else pageNum + 1
                        }

                        else -> content.toInt()
                    }

                    message.editMessage(renderPage(targetPage)).queue { pagination(it, targetPage) }

                    // Delete if we have permission so it doesn't get too spammy
                    if(event.guild != null && event.guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
                        event.message.delete().queue()
                    }
                }
            }
        )
    }

    private fun paginationWithoutTextInput(message: Message, pageNum: Int) {
        waiter.waitFor<MessageReactionAddEvent>(
            delay = timeout,
            unit = unit,
            timeout = { finalAction(message) },
            condition = { checkReaction(it, message) },
            action = { handleMessageReactionAddAction(it, message, pageNum) }
        )
    }

    private fun checkReaction(event: MessageReactionAddEvent, message: Message): Boolean {
        return if(event.messageIdLong != message.idLong) false
        else when(event.reactionEmote.name) {
            LEFT, RIGHT, STOP -> isValidUser(event.user, event.guild)
            BIG_LEFT, BIG_RIGHT -> bulkSkipNumber > 1 && isValidUser(event.user, event.guild)

            else -> false
        }
    }

    private fun handleMessageReactionAddAction(event: MessageReactionAddEvent, message: Message, pageNum: Int) {
        var newPageNum = pageNum
        when(event.reactionEmote.name) {
            LEFT -> {
                if(newPageNum > 1)
                    newPageNum--
            }
            RIGHT -> {
                if(newPageNum < pages)
                    newPageNum++
            }
            BIG_LEFT -> {
                if(newPageNum > 1 || wrapPageEnds) {
                    var i = 1
                    while((newPageNum > 1 || wrapPageEnds) && i < bulkSkipNumber) {
                        if(newPageNum == 1 && wrapPageEnds)
                            newPageNum = pages + 1
                        newPageNum--
                        i++
                    }
                }
            }
            BIG_RIGHT -> {
                if(newPageNum < pages || wrapPageEnds) {
                    var i = 1
                    while((newPageNum < pages || wrapPageEnds) && i < bulkSkipNumber) {
                        if(newPageNum == pages && wrapPageEnds)
                            newPageNum = 0
                        newPageNum++
                        i++
                    }
                }
            }
            STOP -> {
                finalAction(message)
                return // Stop and return
            }
        }

        try {
            event.reaction.removeReaction(event.user).queue()
        } catch(e: PermissionException) {}

        message.editMessage(renderPage(newPageNum)).queue {
            pagination(it ?: return@queue, newPageNum)
        }
    }

    private fun renderPage(pageNum: Int): Message {
        return message {
            text(pageNum, pages)?.let { this@message.append(it) }
            embed embed@ {
                image { urls[pageNum - 1] }
                this@embed.color { this@Slideshow.color(pageNum, pages) }
                this@Slideshow.description(pageNum, urls.size)?.let { description { it } }

                if(showPageNumbers) {
                    this@embed.footer {
                        value = "Image $pageNum/$pages"
                        url = null
                    }
                }
            }
        }
    }

    class Builder : Menu.Builder<Slideshow.Builder, Slideshow>() {
        var colorFun: (Int, Int) -> Color? = { _, _ -> null }
        var textFun: (Int, Int) -> String? = { _, _ -> null }
        var descriptionFun: (Int, Int) -> String? = { _, _ -> null }
        val urls: MutableList<String> = ArrayList()
        var showPageNumbers: Boolean = true
        var waitOnSinglePage: Boolean = false
        var bulkSkipNumber = 1
            set(value) { field = max(value, 1) }
        var wrapPageEnds = false
        var textToLeft: String? = null
        var textToRight: String? = null
        var allowTextInput = false

        operator fun plusAssign(item: String) {
            urls.add(item)
        }

        operator fun set(index: Int, item: String) {
            if(index > urls.size) {
                for(i in (urls.size) until (index - 1)) {
                    urls[i] = ""
                }
            }
            urls[index] = item
        }

        fun clearUrls(): Builder {
            urls.clear()
            return this
        }

        infix inline fun add(lazy: () -> String): Builder {
            urls.add(lazy())
            return this
        }

        infix inline fun urls(lazy: MutableList<in String>.() -> Unit): Builder {
            urls.lazy()
            return this
        }

        infix inline fun showPageNumbers(lazy: () -> Boolean): Builder {
            showPageNumbers = lazy()
            return this
        }

        infix inline fun waitOnSinglePage(lazy: () -> Boolean): Builder {
            waitOnSinglePage = lazy()
            return this
        }

        infix inline fun text(crossinline lazy: (Int, Int) -> String?): Builder {
            textFun = { p, t -> lazy(p, t) }
            return this
        }

        infix inline fun color(crossinline lazy: (Int, Int) -> Color?): Builder {
            colorFun = { p, t -> lazy(p, t) }
            return this
        }

        infix inline fun description(crossinline lazy: (Int, Int) -> String?): Builder {
            descriptionFun = { p, t -> lazy(p, t) }
            return this
        }

        override fun build(): Slideshow = Slideshow(this)
    }
}