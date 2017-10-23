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
@file:Suppress("unused", "MemberVisibilityCanPrivate", "HasPlatformType")
@file:JvmName("OrderedKt")
package me.kgustave.nightfury.extensions

import com.jagrosh.jdautilities.menu.OrderedMenu

class OrderedChoice
{
    var name : String = "null"
    var action : (() -> Unit) = {}

    infix inline fun name(lazy: () -> String) : OrderedChoice
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

infix inline fun OrderedMenu.Builder.choices(lazy: MutableList<OrderedChoice>.() -> Unit) = with(ArrayList<OrderedChoice>()) {
    lazy()
    return@with setChoices(*this.map { it.name }.toTypedArray()).setAction { this[it-1].action() }
}

/**A lazy setter for [OrderedMenu.Builder.setDescription].*/
infix inline fun OrderedMenu.Builder.description(lazy: () -> String?) = setDescription(lazy())

/**A lazy setter for [OrderedMenu.Builder.setText].*/
infix inline fun OrderedMenu.Builder.text(lazy: () -> String?) = setText(lazy())

/**A lazy setter for [OrderedMenu.Builder.useCancelButton].*/
infix inline fun OrderedMenu.Builder.useCancelButton(lazy: () -> Boolean) = useCancelButton(lazy())

/**A lazy setter for [OrderedMenu.Builder.useLetters].*/
infix inline fun OrderedMenu.Builder.useLetters(lazy: () -> Boolean) =  if(lazy()) useLetters() else this

/**A lazy setter for [OrderedMenu.Builder.useNumbers].*/
infix inline fun OrderedMenu.Builder.useNumbers(lazy: () -> Boolean) = if(lazy()) useNumbers() else this

/**A lazy setter for [OrderedMenu.Builder.allowTextInput].*/
infix inline fun OrderedMenu.Builder.allowTextInput(lazy: () -> Boolean) = allowTextInput(lazy())

infix inline fun <reified T: MutableList<OrderedChoice>> T.choice(lazy: OrderedChoice.() -> Unit) : T = with(OrderedChoice()) {
    lazy()
    add(this)
    return@with this@choice
}