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
@file:Suppress("unused", "LiftReturnOrAssignment")
package xyz.nightfury.util.menus

import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.entities.ChannelType
import xyz.nightfury.util.ext.message
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.requests.RestAction
import xyz.nightfury.entities.promise
import xyz.nightfury.util.collections.unmodifiableList
import xyz.nightfury.util.ext.embed
import xyz.nightfury.util.ext.modifyIf
import java.awt.Color
import java.util.*

/**
 * Modeled after jagrosh's OrderedMenu in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
class OrderedMenu(builder: OrderedMenu.Builder): Menu(builder) {
    companion object {
        val numbers = arrayOf(
            "1\u20E3", "2\u20E3",
            "3\u20E3", "4\u20E3",
            "5\u20E3", "6\u20E3",
            "7\u20E3", "8\u20E3",
            "9\u20E3", "\uD83D\uDD1F"
        )

        val letters = arrayOf(
            "\uD83C\uDDE6", "\uD83C\uDDE7",
            "\uD83C\uDDE8", "\uD83C\uDDE9",
            "\uD83C\uDDEA", "\uD83C\uDDEB",
            "\uD83C\uDDEC", "\uD83C\uDDED",
            "\uD83C\uDDEE", "\uD83C\uDDEF"
        )

        const val cancel: String = "\u274C"
    }

    private val color: Color? = builder.color
    private val text: String? = builder.text
    private val description: String? = builder.description
    private val choices: List<OrderedMenu.Choice> = builder.choices
    private val useLetters: Boolean = builder.useLetters
    private val allowTypedInput: Boolean = builder.allowTypedInput
    private val useCancel: Boolean = builder.useCancel
    private val finalAction: (suspend (Message) -> Unit)? = builder.finalAction

    constructor(builder: OrderedMenu.Builder = OrderedMenu.Builder(),
                build: OrderedMenu.Builder.() -> Unit): this(builder.apply(build))

    override fun displayIn(channel: MessageChannel) {
        if(channel is TextChannel && !allowTypedInput &&
           !channel.guild.selfMember.hasPermission(channel, MESSAGE_ADD_REACTION)) {
            throw PermissionException("Must be able to add reactions if not allowing typed input!")
        }
        initialize(channel.sendMessage(message))
    }

    override fun displayAs(message: Message) {
        // This check is basically for whether or not the menu can even display.
        // Is from text channel
        // Does not allow typed input
        // Does not have permission to add reactions
        if(message.channelType == ChannelType.TEXT && !allowTypedInput &&
           !message.guild.selfMember.hasPermission(message.textChannel, MESSAGE_ADD_REACTION)) {
            throw PermissionException("Must be able to add reactions if not allowing typed input!")
        }
        initialize(message.editMessage(this.message))
    }

    private fun initialize(action: RestAction<Message?>) {
        action.promise() then {
            it ?: return@then
            try {
                // From 0 until the number of choices.
                // The last run of this loop will be used to queue
                // the last reaction and possibly a cancel emoji
                // if useCancel was set true before this OrderedMenu
                // was built.
                for(i in choices.indices) {
                    // If this is not the last run of this loop
                    if(i < choices.size - 1)
                        it.addReaction(i.emoji).queue()
                    else {
                        var re = it.addReaction(i.emoji)
                        // If we're using the cancel function we want
                        // to add a "step" so we queue the last emoji being
                        // added and then make the RestAction to start waiting
                        // on the cancel reaction being added.
                        if(useCancel) {
                            re.queue() // queue the last emoji
                            re = it.addReaction(cancel)
                        }
                        // queue the last emoji or the cancel button
                        re.promise() then { _ ->
                            if(allowTypedInput) waitGeneric(it) else waitReactionOnly(it)
                        }
                    } // If this is the last run of this loop
                }
            } catch (ex: PermissionException) {
                // If there is a permission exception mid process, we'll still
                // attempt to make due with what we have.
                if(allowTypedInput) waitGeneric(it) else waitReactionOnly(it)
            }
        }
    }

    private fun waitGeneric(message: Message) {
        waiter.waitFor<GenericMessageEvent>(
            delay = timeout,
            unit = unit,
            timeout = { finalAction?.invoke(message) },
            condition = {
                when(it) {
                    is MessageReactionAddEvent -> it.isValid(message)
                    is MessageReceivedEvent -> it.isValid(message)
                    else -> false
                }
            },
            action = {
                if(it is MessageReactionAddEvent) {
                    if(it.reactionEmote.name == cancel && useCancel) {
                        finalAction?.invoke(message)
                    } else {
                        val choice = choices[it.reactionEmote.name.number]
                        choice(message)
                    }
                } else if(it is MessageReceivedEvent) {
                    val num = it.message.contentRaw.messageNumber
                    if(num < 0 || num > choices.size) {
                        finalAction?.invoke(message)
                    } else {
                        val choice = choices[num]
                        choice(message)
                    }
                }
            }
        )
    }

    private fun waitReactionOnly(message: Message) {
        waiter.waitFor<MessageReactionAddEvent>(
            delay = timeout,
            unit = unit,
            timeout = { finalAction?.invoke(message) },
            condition = {
                it.isValid(message)
            },
            action = {
                if(it.reactionEmote.name == cancel && useCancel) {
                    finalAction?.invoke(message)
                } else {
                    val choice = choices[it.reactionEmote.name.number]
                    choice(message)
                }
            }
        )
    }

    private fun MessageReactionAddEvent.isValid(message: Message): Boolean {
        if(messageIdLong != message.idLong)
            return false
        if(!isValidUser(user, guild))
            return false
        if(reactionEmote.name == cancel && useCancel)
            return true

        val num = reactionEmote.name.number

        return !(num < 0 || num > choices.size)
    }

    private fun MessageReceivedEvent.isValid(message: Message): Boolean {
        if(channel.idLong != message.channel.idLong)
            return false

        return isValidUser(author, guild)
    }

    private val message: Message get() = message {
        text?.let { append { text } }
        embed {
            this@OrderedMenu.description?.let { append(it) }

            this@embed.color = this@OrderedMenu.color

            for((i, c) in choices.withIndex()) {
                append("\n${i.emoji} $c")
            }
        }
    }

    private inline val Int.emoji: String inline get() = numbers.modifyIf(useLetters){letters}[this]

    private inline val String.number: Int inline get() {
        return numbers.modifyIf(useLetters) { letters }.withIndex().firstOrNull { it.value == this }?.index ?: -1
    }

    private val String.messageNumber: Int get() {
        if(useLetters) {
            // This doesn't look good, but bear with me for a second:
            // So the maximum number of letters you can have as reactions
            // is 10 (the maximum number of choices in general even).
            // If you look carefully, you'll see that a corresponds to the
            // index 1, b to the index 2, and so on.
            return if(length == 1) " abcdefghij".indexOf(toLowerCase()) else -1
        } else {
            // The same as above applies here, albeit in a different way.
            return when {
                length == 1 -> " 123456789".indexOf(this)
                this == "10" -> 10
                else -> -1
            }
        }
    }

    class Builder(): Menu.Builder<OrderedMenu.Builder, OrderedMenu>() {
        var color: Color? = null
        var text: String? = null
        var description: String? = null
        var useLetters = false
        var allowTypedInput = true
        var useCancel = false
        var finalAction: (suspend (Message) -> Unit)? = null

        private val _choices: MutableList<OrderedMenu.Choice> = LinkedList()
        val choices: List<OrderedMenu.Choice> get() = unmodifiableList(_choices)

        constructor(build: OrderedMenu.Builder.() -> Unit): this() {
            build()
        }

        operator fun set(name: String, action: suspend (Message) -> Unit) {
            _choices += OrderedMenu.Choice(name, action)
        }

        fun clearChoices() = _choices.clear()
        fun finalAction(block: suspend (Message) -> Unit): OrderedMenu.Builder = apply { finalAction = block }
        fun choice(name: String, action: suspend (Message) -> Unit): OrderedMenu.Builder = apply { set(name, action) }
        inline fun description(lazy: () -> String?): OrderedMenu.Builder = apply { description = lazy() }
        inline fun text(lazy: () -> String?): OrderedMenu.Builder = apply { text = lazy() }
        inline fun useCancelButton(lazy: () -> Boolean): OrderedMenu.Builder = apply { useCancel = lazy() }
        inline fun color(lazy: () -> Color?): OrderedMenu.Builder = apply { color = lazy() }
        inline fun useLetters(lazy: () -> Boolean): OrderedMenu.Builder = apply { useLetters = lazy() }
        inline fun useNumbers(lazy: () -> Boolean): OrderedMenu.Builder = apply { useLetters = !lazy() }
        inline fun allowTextInput(lazy: () -> Boolean): OrderedMenu.Builder = apply { allowTypedInput = lazy() }
    }

    class Choice(val name: String = "null", private val action: (suspend (Message) -> Unit)? = null) {
        suspend operator fun invoke(message: Message) {
            action?.invoke(message)
        }

        override fun toString(): String = name
    }
}
