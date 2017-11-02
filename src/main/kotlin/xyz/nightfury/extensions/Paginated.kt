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
@file:Suppress("unused", "HasPlatformType")
@file:JvmName("PaginatedKt")
package xyz.nightfury.extensions

import com.jagrosh.jdautilities.menu.Paginator
import net.dv8tion.jda.core.entities.Message
import java.awt.Color

infix fun Paginator.Builder.finalAction(lazy: (Message) -> Unit) = setFinalAction(lazy)

infix fun Paginator.Builder.colorBiFunction(lazy: (Int, Int) -> Color?) = setColor(lazy)

infix fun Paginator.Builder.text(lazy: (Int, Int) -> String?) = setText(lazy)

infix inline fun Paginator.Builder.items(lazy: MutableList<String>.() -> Unit) = setItems(*with(ArrayList<String>()) {
    lazy()
    toTypedArray()
})

infix inline fun Paginator.Builder.add(lazy: () -> String) = addItems(lazy())

infix inline fun Paginator.Builder.columns(lazy: () -> Int) = setColumns(lazy())

infix inline fun Paginator.Builder.text(lazy: () -> String?) = setText(lazy())

infix inline fun Paginator.Builder.waitOnSinglePage(lazy: () -> Boolean) = waitOnSinglePage(lazy())

infix inline fun Paginator.Builder.itemsPerPage(lazy: () -> Int) = setItemsPerPage(lazy())

infix inline fun Paginator.Builder.showPageNumbers(lazy: () -> Boolean) = showPageNumbers(lazy())

infix inline fun Paginator.Builder.useNumberedItems(lazy: () -> Boolean) = useNumberedItems(lazy())