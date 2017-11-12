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
package xyz.nightfury.entities

/**
 * A basic formatted string builder.
 *
 * Usage is through [format].
 *
 * @author Kaidan Gustave
 */
@SinceKotlin("1.2")
class FormattedStringBuilder(val format: String): Appendable {
    private val set: MutableSet<Any> = HashSet()

    operator fun plusAssign(any: Any?) {
        append(any)
    }

    operator fun plus(any: Any?): FormattedStringBuilder {
        append(any)
        return this
    }

    operator fun get(any: Any?): FormattedStringBuilder {
        append(any)
        return this
    }

    fun append(any: Any?): FormattedStringBuilder {
        set.add(requireNotNull(any))
        return this
    }

    override fun append(csq: CharSequence?): FormattedStringBuilder {
        set.add(requireNotNull(csq))
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): FormattedStringBuilder {
        set.add(requireNotNull(csq?.subSequence(start, end)))
        return this
    }

    override fun append(c: Char): FormattedStringBuilder {
        set.add(c.toString())
        return this
    }

    override fun toString(): String = format.format(*set.toTypedArray())
}

/**
 * Uses a [FormattedStringBuilder] block to process the provided [format] String.
 *
 * Basic usage can be demonstrated as:
 *
 * ```kotlin
 * val f = "Hi, my name is %s, and I am %d years old!"
 *
 * fun main(args: Array<String>) {
 *     val out = format(f) {
 *         this["Kaidan"]
 *         this[18]
 *     }
 *
 *     println(out)
 * }
 *
 * // Hi, my name is Kaidan, and I am 18 years old!
 * ```
 */
@SinceKotlin("1.2")
inline fun format(format: String, init: FormattedStringBuilder.() -> Unit): String =
    with(FormattedStringBuilder(format)){
        init()
        return@with toString()
    }

/**
 * See [format] for more details.
 */
@[SinceKotlin("1.2") JvmName("formatThis")] // @JvmName because apparently inline extensions have same JVM Name???
inline fun String.format(init: FormattedStringBuilder.() -> Unit): String =
    with(FormattedStringBuilder(this)){
        init()
        return@with toString()
    }