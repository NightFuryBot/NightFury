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
import xyz.nightfury.util.ext.embed
import xyz.nightfury.util.ext.message
import java.awt.Color
import kotlin.math.max

/**
 * Modeled after jagrosh's Paginator in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
@Suppress("Unused", "MemberVisibilityCanBePrivate")
class Paginator(builder: Paginator.Builder): Menu(builder) {
    companion object {
        const val BIG_LEFT = "\u23EA"
        const val LEFT = "\u25C0"
        const val STOP = "\u23F9"
        const val RIGHT = "\u25B6"
        const val BIG_RIGHT = "\u23E9"
    }

    private val color: (Int, Int) -> Color? = builder.colorFun
    private val text: (Int, Int) -> String? = builder.textFun
    private val strings: List<String> = builder.items
    private val columns: Int = builder.columns
    private val itemsPerPage: Int = builder.itemsPerPage
    private val numberItems: Boolean = builder.numberItems
    private val showPageNumbers: Boolean = builder.showPageNumbers
    private val waitOnSinglePage: Boolean = builder.waitOnSinglePage
    private val bulkSkipNumber: Int = builder.bulkSkipNumber
    private val wrapPageEnds: Boolean = builder.wrapPageEnds
    private val leftText: String? = builder.textToLeft
    private val rightText: String? = builder.textToRight
    private val allowTextInput: Boolean = builder.allowTextInput

    private val pages = Math.ceil(strings.size.toDouble() / itemsPerPage).toInt()

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
        val start = (pageNum - 1) * itemsPerPage
        val end = if(strings.size < pageNum * itemsPerPage) strings.size else pageNum * itemsPerPage

        return message {
            text(pageNum, pages)?.let { this@message.append(it) }
            embed embed@ {
                if(columns == 1) {
                    for(i in start until end) {
                        this@embed.appendln()
                        this@embed.append(if(numberItems) "`${i + 1}.`" else "")
                        this@embed.append(strings[i])
                    }
                } else {
                    val per = Math.ceil((end - start).toDouble() / columns).toInt()
                    for(k in 0 until columns) {
                        this@embed.field field@ {
                            this@field.name = ""
                            var i = start + k * per
                            while((i < end) and (i < start + (k + 1) * per)) {
                                this@field.appendln()
                                this@field.append(if(numberItems) "${i + 1}. " else "")
                                this@field.append(strings[i])
                                i++
                            }

                            this@field.inline = true
                        }
                    }
                }

                this@embed.color { this@Paginator.color(pageNum, pages) }

                if(showPageNumbers) {
                    this@embed.footer {
                        value = "Page $pageNum/$pages"
                        url = null
                    }
                }
            }
        }
    }

    class Builder : Menu.Builder<Paginator.Builder, Paginator>() {
        var colorFun: (Int, Int) -> Color? = { _, _ -> null }
        var textFun: (Int, Int) -> String? = { _, _ -> null }
        val items: MutableList<String> = ArrayList()
        var columns: Int = 1
            set(value) {
                require(value in 1..3) { "Number of columns must be at least 1 and at most 3" }
                field = value
            }
        var itemsPerPage: Int = 10
            set(value) {
                require(value >= 1) { "Number of items must be at least 1" }
                field = value
            }
        var numberItems: Boolean = false
        var showPageNumbers: Boolean = true
        var waitOnSinglePage: Boolean = false
        var bulkSkipNumber: Int = 1
            set(value) { field = max(value, 1) }
        var wrapPageEnds: Boolean = false
        var textToLeft: String? = null
        var textToRight: String? = null
        var allowTextInput: Boolean = false

        operator fun String.unaryPlus() {
            items.add(this)
        }

        operator fun plusAssign(item: String) {
            items.add(item)
        }

        operator fun set(index: Int, item: String) {
            if(index > items.size) {
                for(i in (items.size) until (index - 1)) {
                    items[i] = ""
                }
            }
            items[index] = item
        }

        fun clearItems(): Paginator.Builder {
            items.clear()
            return this
        }

        inline infix fun add(lazy: () -> String): Builder {
            items.add(lazy())
            return this
        }

        inline infix fun items(lazy: MutableList<in String>.() -> Unit): Builder {
            items.lazy()
            return this
        }

        inline infix fun columns(lazy: () -> Int): Builder {
            columns = lazy()
            return this
        }

        inline infix fun itemsPerPage(lazy: () -> Int): Builder {
            itemsPerPage = lazy()
            return this
        }

        inline infix fun numberItems(lazy: () -> Boolean): Builder {
            numberItems = lazy()
            return this
        }

        inline infix fun showPageNumbers(lazy: () -> Boolean): Builder {
            showPageNumbers = lazy()
            return this
        }

        inline infix fun waitOnSinglePage(lazy: () -> Boolean): Builder {
            waitOnSinglePage = lazy()
            return this
        }

        inline infix fun text(crossinline lazy: (Int, Int) -> String?): Builder {
            textFun = { p, t -> lazy(p, t) }
            return this
        }

        inline infix fun color(crossinline lazy: (Int, Int) -> Color?): Builder {
            colorFun = { p, t -> lazy(p, t) }
            return this
        }

        override fun build(): Paginator {
            require(items.size > 0) { "Must include at least one item to paginate." }

            return Paginator(this)
        }
    }
}
