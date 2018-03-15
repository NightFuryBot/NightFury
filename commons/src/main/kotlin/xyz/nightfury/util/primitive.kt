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
@file:Suppress("Unused")
package xyz.nightfury.util

val Int.name: String? get() = Character.getName(this)
fun Int.toChars(): CharArray = Character.toChars(this)
fun Int.toHexString(): String = Integer.toHexString(this)

inline val Long.length: Int inline get() = "$this".length

fun emptyShortArray(): ShortArray = ShortArray(0)
fun emptyIntArray(): IntArray = IntArray(0)
fun emptyLongArray(): LongArray = LongArray(0)

fun arrayOf(vararg shorts: Short): ShortArray = ShortArray(shorts.size) { shorts[it] }
fun arrayOf(vararg ints: Int): IntArray = IntArray(ints.size) { ints[it] }
fun arrayOf(vararg longs: Long): LongArray = LongArray(longs.size) { longs[it] }
