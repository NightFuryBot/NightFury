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

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.requests.RestAction
import xyz.nightfury.entities.promise
import xyz.nightfury.entities.then
import xyz.nightfury.extensions.embed
import xyz.nightfury.extensions.message
import java.awt.Color

/**
 * @author Kaidan Gustave
 */
class Paginator(val builder: Paginator.Builder): Menu(builder) {
    companion object {
        const val LEFT = "\u25C0"
        const val STOP = "\u23F9"
        const val RIGHT = "\u25B6"
    }

    private val color: (Int, Int) -> Color? = builder.colorFun
    private val text: (Int, Int) -> String? = builder.textFun
    private val strings: List<String> = builder.items
    private val columns: Int = builder.columns
    private val itemsPerPage: Int = builder.itemsPerPage
    private val numberItems: Boolean = builder.numberItems
    private val showPageNumbers: Boolean = builder.showPageNumbers
    private val waitOnSinglePage: Boolean = builder.waitOnSinglePage

    val pages = Math.ceil(strings.size.toDouble() / itemsPerPage).toInt()

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
                    m.addReaction(LEFT).queue()
                    m.addReaction(STOP).queue()
                    m.addReaction(RIGHT) then { pagination(m, pageNum) } catch { pagination(m, pageNum) }
                }

                waitOnSinglePage -> {
                    m.addReaction(STOP) then { pagination(m, pageNum) } catch { pagination(m, pageNum) }
                }

                else -> finalAction(m)
            }
        }
    }

    private fun pagination(message: Message, pageNum: Int) {
        waiter.waitFor<MessageReactionAddEvent>(
            delay = timeout,
            unit = unit,
            timeout = { finalAction(message) },
            condition = { event ->
                if(event.messageIdLong != message.idLong)
                    false
                else if(!(LEFT == event.reactionEmote.name ||
                          STOP == event.reactionEmote.name ||
                          RIGHT == event.reactionEmote.name))
                    false
                else isValidUser(user = event.user, guild = event.guild)
            },
            action = { event ->
                var newPageNum = pageNum
                when(event.reactionEmote.name) {
                    LEFT -> if(newPageNum > 1) newPageNum--
                    RIGHT -> if(newPageNum < pages) newPageNum++
                    STOP -> {
                        finalAction(message)
                        return@waitFor
                    }
                }

                try { event.reaction.removeReaction(event.user).queue() } catch(e: PermissionException) {}
                message.editMessage(renderPage(newPageNum)).queue {
                    pagination(it ?: return@queue, newPageNum)
                }
            }
        )
    }

    private fun renderPage(pageNum: Int): Message {
        val start = (pageNum - 1) * itemsPerPage
        val end = if(strings.size < pageNum * itemsPerPage) strings.size else pageNum * itemsPerPage

        return message {
            text(pageNum, pages)?.let { this@message.append(it) }
            embed embed@ {
                if(columns == 1) {
                    for (i in start until end) {
                        this@embed.appendln()
                        this@embed.append(if (numberItems) "`${i + 1}.`" else "")
                        this@embed.append(strings[i])
                    }
                } else {
                    val per = Math.ceil((end - start).toDouble() / columns).toInt()
                    for (k in 0 until columns) {
                        this@embed.field field@ {
                            this@field.name = ""
                            var i = start + k * per
                            while ((i < end) and (i < start + (k + 1) * per)) {
                                this@field.appendln()
                                this@field.append(if (numberItems) "${i + 1}. " else "")
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
                require(value in 1..3) { "Number of columns must be at least 1 and at most 3." }
                field = value
            }
        var itemsPerPage: Int = 10
            set(value) {
                require(value >= 1) { "Number of items must be at least 1." }
                field = value
            }
        var numberItems: Boolean = false
        var showPageNumbers: Boolean = true
        var waitOnSinglePage: Boolean = false

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

        infix inline fun add(lazy: () -> String): Builder {
            items.add(lazy())
            return this
        }

        infix inline fun items(lazy: MutableList<in String>.() -> Unit): Builder {
            items.lazy()
            return this
        }

        infix inline fun columns(lazy: () -> Int): Builder {
            columns = lazy()
            return this
        }

        infix inline fun itemsPerPage(lazy: () -> Int): Builder {
            itemsPerPage = lazy()
            return this
        }

        infix inline fun numberItems(lazy: () -> Boolean): Builder {
            numberItems = lazy()
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

        override fun build(): Paginator {
            require(items.size > 0) { "Must include at least one item to paginate." }

            return Paginator(this)
        }
    }
}