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
@file:Suppress("unused")
package me.kgustave.nightfury.extensions.waiting

import com.jagrosh.jdautilities.menu.orderedmenu.OrderedMenuBuilder
import com.jagrosh.jdautilities.waiter.EventWaiter
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
class KOrderedMenuBuilder internal constructor(val orderedMenuBuilder: OrderedMenuBuilder)
{

    var description : String? = null
    var color : Int? = null
    var cancel : (() -> Unit)? = null
    var choices : MutableList<OrderedChoice> = ArrayList()
    var users : Array<User> = emptyArray()
    var text : String? = null
    var timeout : TimeOut? = null
    var useCancelButton : Boolean = false
    var useLetters : Boolean = false
    var useNumbers : Boolean = true
    var roles : Array<Role> = emptyArray()
    var allowTextInput : Boolean = false

    operator fun component1()  = description
    operator fun component2()  = color
    operator fun component3()  = cancel
    operator fun component4()  = choices
    operator fun component5()  = users
    operator fun component6()  = text
    operator fun component7()  = timeout
    operator fun component8()  = useCancelButton
    operator fun component9()  = useLetters
    operator fun component10() = useNumbers
    operator fun component11() = roles
    operator fun component12() = allowTextInput

    internal fun build() = with(orderedMenuBuilder)
    {
        val (description, color, cancel, choices, users, text, timeout, useCancelButton, useLetters, useNumbers,
                roles, allowTextInput) = this@KOrderedMenuBuilder

        if(description != null)  setDescription(description)
        if(color != null)        setColor(Color(color))
        if(cancel != null)       setCancel{cancel.invoke()}
        if(text != null)         setText(text)
        if(choices.isNotEmpty()) addChoices(*choices.map{it.name}.toTypedArray()).setAction{choices[it-1].get().invoke()}
        if(timeout != null)      setTimeout(timeout.delay, timeout.unit)
                                 useCancelButton(useCancelButton)
        if(useLetters)           useLetters()
        if(useNumbers)           useNumbers()
        if(users.isNotEmpty())   setUsers(*users)
        if(roles.isNotEmpty())   setRoles(*roles)
                                 allowTextInput(allowTextInput)
        return@with this
    }

    infix inline fun description(lazy: () -> String?) : KOrderedMenuBuilder
    {
        this.description = lazy()
        return this
    }

    infix inline fun colorAwt(lazy: () -> Color?) : KOrderedMenuBuilder
    {
        this.color = lazy()?.rgb
        return this
    }

    infix inline fun color(lazy: () -> Int?) : KOrderedMenuBuilder
    {
        this.color = lazy()
        return this
    }

    infix inline fun choice(lazy: OrderedChoice.() -> OrderedChoice) : KOrderedMenuBuilder
    {
        this.choices.add(lazy(OrderedChoice()))
        return this
    }

    infix inline fun users(lazy: () -> Array<out User>) : KOrderedMenuBuilder
    {
        this.users = arrayOf(*lazy())
        return this
    }

    infix inline fun text(lazy: () -> String?) : KOrderedMenuBuilder
    {
        this.text = lazy()
        return this
    }

    inline fun timeout(unit: TimeUnit, lazy: () -> Long) : KOrderedMenuBuilder
    {
        this.timeout = TimeOut(lazy(), unit)
        return this
    }

    infix inline fun timeout(lazy: () -> Long) : KOrderedMenuBuilder
    {
        this.timeout = TimeOut(lazy())
        return this
    }

    infix inline fun useCancelButton(lazy: () -> Boolean) : KOrderedMenuBuilder
    {
        this.useCancelButton = lazy()
        return this
    }

    infix inline fun useLetters(lazy: () -> Boolean) : KOrderedMenuBuilder
    {
        this.useLetters = lazy()
        return this
    }

    infix inline fun useNumbers(lazy: () -> Boolean) : KOrderedMenuBuilder
    {
        this.useNumbers = lazy()
        return this
    }

    infix inline fun roles(lazy: () -> Array<out Role>) : KOrderedMenuBuilder
    {
        this.roles = arrayOf(*lazy())
        return this
    }

    infix inline fun allowTextInput(lazy: () -> Boolean) : KOrderedMenuBuilder
    {
        this.allowTextInput = lazy()
        return this
    }
}

class OrderedChoice
{
    var name : String = "null"
    var action : (() -> Unit) = {}

    fun get() : (() -> Unit) = action

    infix fun name(lazy: () -> String) : OrderedChoice
    {
        this.name = lazy()
        return this
    }

    infix fun action(action : (() -> Unit)) : OrderedChoice
    {
        this.action = action
        return this
    }
}

fun orderedMenu(waiter: EventWaiter, channel: MessageChannel, init: KOrderedMenuBuilder.() -> Unit) =
        with(KOrderedMenuBuilder(OrderedMenuBuilder().setEventWaiter(waiter)))
{
    init()
    build().build().display(channel)
}

fun orderedMenu(waiter: EventWaiter, message: Message, init: KOrderedMenuBuilder.() -> Unit) =
        with(KOrderedMenuBuilder(OrderedMenuBuilder().setEventWaiter(waiter)))
{
    init()
    build().build().display(message)
}

fun orderedMenu(waiter: EventWaiter, init: KOrderedMenuBuilder.() -> Unit) =
        with(KOrderedMenuBuilder(OrderedMenuBuilder().setEventWaiter(waiter)))
{
    init()
    build()
}

fun OrderedMenuBuilder.modify(init: KOrderedMenuBuilder.() -> Unit) = with(KOrderedMenuBuilder(this))
{
    init()
    build()
}

fun OrderedMenuBuilder.display(channel: MessageChannel, init: KOrderedMenuBuilder.() -> Unit) = with(KOrderedMenuBuilder(this))
{
    init()
    build().build().display(channel)
}

fun OrderedMenuBuilder.display(message: Message, init: KOrderedMenuBuilder.() -> Unit) = with(KOrderedMenuBuilder(this))
{
    init()
    build().build().display(message)
}