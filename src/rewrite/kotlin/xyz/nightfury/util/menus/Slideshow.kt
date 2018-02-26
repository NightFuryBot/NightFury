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
package xyz.nightfury.util.menus

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.requests.RestAction
import xyz.nightfury.entities.RestDeferred
import xyz.nightfury.util.ext.await
import xyz.nightfury.util.ext.embed
import xyz.nightfury.util.ext.message
import xyz.nightfury.util.ignored
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Modeled after jagrosh's Slideshow in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
@Suppress("Unused", "MemberVisibilityCanBePrivate")
class Slideshow(builder: Slideshow.Builder): Menu(builder) {
    companion object {
        const val BIG_LEFT = Paginator.BIG_LEFT
        const val LEFT = Paginator.LEFT
        const val STOP = Paginator.STOP
        const val RIGHT = Paginator.RIGHT
        const val BIG_RIGHT = Paginator.BIG_RIGHT
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
    private val finalAction: (suspend (Message) -> Unit)? = builder.finalAction
    private val pages = urls.size

    override fun displayIn(channel: MessageChannel) = paginate(channel, 1)

    override fun displayAs(message: Message) = paginate(message, 1)

    fun paginate(message: Message, pageNum: Int) {
        val num = min(max(pageNum, 1), pages)
        initialize(message.editMessage(renderPage(num)), num)
    }

    fun paginate(channel: MessageChannel, pageNum: Int) {
        val num = min(max(pageNum, 1), pages)
        initialize(channel.sendMessage(renderPage(num)), num)
    }

    private fun initialize(action: RestAction<Message?>, pageNum: Int) {
        RestDeferred(action, waiter).promise { m ->
            m ?: return@promise
            when {
                pages > 1 -> {
                    ignored {
                        if(bulkSkipNumber > 1) m.addReaction(BIG_LEFT).await()
                        m.addReaction(LEFT).await()
                        m.addReaction(STOP).await()
                        m.addReaction(RIGHT).await()
                        if(bulkSkipNumber > 1) m.addReaction(BIG_RIGHT).await()
                    }
                    pagination(m, pageNum)
                }

                waitOnSinglePage -> {
                    ignored { m.addReaction(STOP).await() }
                    pagination(m, pageNum)
                }

                else -> finalAction?.invoke(m)
            }
        }
    }

    private suspend fun pagination(message: Message, pageNum: Int) {
        if(allowTextInput || leftText !== null && rightText !== null)
            paginationWithTextInput(message, pageNum)
        else
            paginationWithoutTextInput(message, pageNum)
    }

    private suspend fun paginationWithTextInput(message: Message, pageNum: Int) {
        val deferred = waiter.receive<GenericMessageEvent>(delay = timeout, unit = unit) { event ->
            if(event is MessageReactionAddEvent) {
                return@receive checkReaction(event, message)
            } else if(event is MessageReceivedEvent) {
                if(event.channel != message.channel) {
                    return@receive false
                }

                val content = event.message.contentRaw
                if(leftText !== null && rightText !== null) {
                    if(content.equals(leftText, true) || content.equals(rightText, true)) {
                        return@receive isValidUser(event.author, event.guild)
                    }
                }

                if(allowTextInput) {
                    ignored {
                        val i = content.toInt()
                        if(i in 1..pages && i != pageNum)
                            return@receive isValidUser(event.author, event.guild)
                    }
                }
            }

            // Default return false
            false
        }

        val event = deferred.await()

        when(event) {
            null -> finalAction?.invoke(message)
            is MessageReactionAddEvent -> handleMessageReactionAddAction(event, message, pageNum)
            is MessageReceivedEvent -> {
                val content = event.message.contentRaw

                val targetPage = when {
                    leftText !== null && content.equals(leftText, true) &&
                    (1 < pageNum || wrapPageEnds) -> {
                        if(pageNum - 1 < 1 && wrapPageEnds) pages else pageNum - 1
                    }

                    rightText !== null && content.equals(rightText, true) &&
                    (pageNum < pages || wrapPageEnds) -> {
                        if(pageNum + 1 > pages && wrapPageEnds) 1 else pageNum + 1
                    }

                    else -> content.toInt()
                }

                val m = ignored(message) { message.editMessage(renderPage(targetPage)).await() }
                pagination(m, targetPage)
            }
        }
    }

    private suspend fun paginationWithoutTextInput(message: Message, pageNum: Int) {
        val deferred = waiter.receive<MessageReactionAddEvent>(delay = timeout, unit = unit) {
            checkReaction(it, message)
        }

        val event = deferred.await()

        when(event) {
            null -> finalAction?.invoke(message)
            else -> handleMessageReactionAddAction(event, message, pageNum)
        }
    }

    private fun checkReaction(event: MessageReactionAddEvent, message: Message): Boolean {
        return if(event.messageIdLong != message.idLong) false else when(event.reactionEmote.name) {
            LEFT, RIGHT, STOP -> isValidUser(event.user, event.guild)
            BIG_LEFT, BIG_RIGHT -> bulkSkipNumber > 1 && isValidUser(event.user, event.guild)

            else -> false
        }
    }

    private suspend fun handleMessageReactionAddAction(event: MessageReactionAddEvent, message: Message, pageNum: Int) {
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
                finalAction?.invoke(message)
                return // Stop and return
            }
        }

        event.reaction.removeReaction(event.user).queue()
        val m = ignored(message) { message.editMessage(renderPage(newPageNum)).await() }
        pagination(m, newPageNum)
    }

    private fun renderPage(pageNum: Int): Message = message {
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
        var finalAction: (suspend (Message) -> Unit)? = null

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

        fun clearUrls(): Slideshow.Builder {
            urls.clear()
            return this
        }

        inline fun add(lazy: () -> String): Slideshow.Builder {
            urls.add(lazy())
            return this
        }

        inline fun urls(lazy: MutableList<in String>.() -> Unit): Slideshow.Builder {
            urls.lazy()
            return this
        }

        inline fun showPageNumbers(lazy: () -> Boolean): Slideshow.Builder {
            showPageNumbers = lazy()
            return this
        }

        inline fun waitOnSinglePage(lazy: () -> Boolean): Slideshow.Builder {
            waitOnSinglePage = lazy()
            return this
        }

        inline fun text(crossinline lazy: (Int, Int) -> String?): Slideshow.Builder {
            textFun = { p, t -> lazy(p, t) }
            return this
        }

        inline fun color(crossinline lazy: (Int, Int) -> Color?): Slideshow.Builder {
            colorFun = { p, t -> lazy(p, t) }
            return this
        }

        inline fun description(crossinline lazy: (Int, Int) -> String?): Slideshow.Builder {
            descriptionFun = { p, t -> lazy(p, t) }
            return this
        }

        fun finalAction(block: suspend (Message) -> Unit): Slideshow.Builder {
            finalAction = block
            return this
        }
    }
}
