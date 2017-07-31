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

import com.jagrosh.jdautilities.menu.pagination.Paginator
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder
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
class KPaginatorBuilder internal constructor(val paginatorBuilder: PaginatorBuilder)
{
    val items : MutableList<String> = ArrayList()
    var finalAction : (Message) -> Unit = {}
    var color : Int? = null
    var colorBiFunction : (Int, Int) -> Color? = { _, _ -> null }
    var columns : Int = 1
    var text : String? = null
    var textBiFunction : ((Int, Int) -> String)? = null
    var waitOnSinglePage : Boolean = false
    var timeout : TimeOut? = null
    var itemsPerPage : Int = 12
    var showPageNumber : Boolean = false
    var useNumberedItems : Boolean = false
    var users : Array<User> = emptyArray()
    var roles : Array<Role> = emptyArray()

    operator fun component1() = items
    operator fun component2() = finalAction
    operator fun component3() = color
    operator fun component4() = colorBiFunction
    operator fun component5() = columns
    operator fun component6() = text
    operator fun component7() = textBiFunction
    operator fun component8() = waitOnSinglePage
    operator fun component9() = timeout
    operator fun component10() = itemsPerPage
    operator fun component11() = showPageNumber
    operator fun component12() = useNumberedItems
    operator fun component13() = users
    operator fun component14() = roles

    internal fun build() = with(paginatorBuilder)
    {
        val(items, finalAction, color, colorBiFunction, columns, text, textBiFunction, waitOnSinglePage,
                timeout, itemsPerPage, showPageNumber, useNumberedItems, users, roles) = this@KPaginatorBuilder
        setItems(*items.toTypedArray())
        setFinalAction(finalAction)

        if(color!=null) setColor(Color(color))
        else            setColor(colorBiFunction)

        if(columns>1) setColumns(columns)

        if(textBiFunction==null && text!=null)      setText(text)
        else if(textBiFunction!=null && text==null) setText(textBiFunction)
        else if(textBiFunction!=null && text!=null) setText(textBiFunction)

        waitOnSinglePage(waitOnSinglePage)
        setItemsPerPage(itemsPerPage)
        showPageNumbers(showPageNumber)
        useNumberedItems(useNumberedItems)

        if(timeout!=null) setTimeout(timeout.delay, timeout.unit)

        setUsers(*users)
        setRoles(*roles)

        return@with this
    }

    infix inline fun items(lazy: MutableList<String>.() -> Unit) : KPaginatorBuilder
    {
        lazy(items)
        return this
    }

    infix inline fun finalAction(crossinline lazy: (Message) -> Unit) : KPaginatorBuilder
    {
        this.finalAction = { lazy(it) }
        return this
    }

    infix inline fun colorAwt(lazy: () -> Color?) : KPaginatorBuilder
    {
        this.color = lazy()?.rgb
        return this
    }

    infix inline fun color(lazy: () -> Int?) : KPaginatorBuilder
    {
        this.color = lazy()
        return this
    }

    infix inline fun columns(lazy: () -> Int) : KPaginatorBuilder
    {
        this.columns = lazy()
        return this
    }

    infix inline fun text(lazy: () -> String?) : KPaginatorBuilder
    {
        this.text = lazy()
        return this
    }

    infix inline fun waitOnSinglePage(lazy: () -> Boolean) : KPaginatorBuilder
    {
        this.waitOnSinglePage = lazy()
        return this
    }

    infix inline fun timeout(lazy: () -> Long) : KPaginatorBuilder
    {
        this.timeout = TimeOut(lazy())
        return this
    }

    inline fun timeout(unit: TimeUnit, lazy: () -> Long) : KPaginatorBuilder
    {
        this.timeout = TimeOut(lazy(), unit)
        return this
    }

    infix inline fun itemsPerPage(lazy: () -> Int) : KPaginatorBuilder
    {
        this.itemsPerPage = lazy()
        return this
    }

    infix inline fun showPageNumbers(lazy: () -> Boolean) : KPaginatorBuilder
    {
        this.showPageNumber = lazy()
        return this
    }

    infix inline fun useNumberedItems(lazy: () -> Boolean) : KPaginatorBuilder
    {
        this.useNumberedItems = lazy()
        return this
    }

    infix inline fun roles(lazy: () -> Array<out Role>) : KPaginatorBuilder
    {
        this.roles = arrayOf(*lazy())
        return this
    }

    infix inline fun users(lazy: () -> Array<out User>) : KPaginatorBuilder
    {
        this.users = arrayOf(*lazy())
        return this
    }

}

fun paginator(waiter: EventWaiter, init : KPaginatorBuilder.() -> Unit) : Paginator =
        with(KPaginatorBuilder(PaginatorBuilder().setEventWaiter(waiter)))
{
    init()
    build().build()
}

fun paginator(waiter: EventWaiter, channel: MessageChannel, init : KPaginatorBuilder.() -> Unit) : Unit =
        with(KPaginatorBuilder(PaginatorBuilder().setEventWaiter(waiter)))
{
    this.init()
    build().build().display(channel)
}

fun PaginatorBuilder.modify(init: KPaginatorBuilder.() -> Unit) : PaginatorBuilder =
        with(KPaginatorBuilder(this))
{
    init()
    build()
}