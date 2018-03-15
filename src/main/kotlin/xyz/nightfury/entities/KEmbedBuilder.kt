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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.nightfury.entities

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.MessageEmbed
import xyz.nightfury.util.lineSeparator
import xyz.nightfury.util.modifyIf
import java.awt.Color
import java.time.temporal.TemporalAccessor

// Inspired by club.minnced.kjda.builders.KJDAEmbedBuilder

/**
 * @author Kaidan Gustave
 */
@MessageDsl
class KEmbedBuilder: Appendable {
    @PublishedApi
    internal val fields = mutableListOf<MessageEmbed.Field>()
    private val description = StringBuilder()

    var title: String? = null
    var url: String? = null
    var thumbnail: String? = null
    var image: String? = null

    var author: Entity? = null
    var footer: Entity? = null
    var time: TemporalAccessor? = null
    var color: Color? = null

    @PublishedApi
    internal fun build(): MessageEmbed = EmbedBuilder().apply {
        val(description, fields, title, url, time, color, author, thumbnail, footer, image) = this@KEmbedBuilder

        fields.forEach { addField(it) }

        if(!description.isBlank()) setDescription(description.toString())
        if(!title.isNullOrBlank()) setTitle(title, url)
        image?.let(::setImage)
        time?.let(::setTimestamp)
        thumbnail?.let(::setThumbnail)
        color?.let(::setColor)
        footer?.let { setFooter(it.value, it.icon) }
        author?.let { setAuthor(it.value, it.url, it.icon) }

    }.build()

    operator fun component1()  = description
    operator fun component2()  = fields
    operator fun component3()  = title
    operator fun component4()  = url
    operator fun component5()  = time
    operator fun component6()  = color
    operator fun component7()  = author
    operator fun component8()  = thumbnail
    operator fun component9()  = footer
    operator fun component10() = image

    override fun append(csq: CharSequence?): KEmbedBuilder {
        description.append(csq)
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): KEmbedBuilder {
        description.append(csq, start, end)
        return this
    }

    override fun append(c: Char): KEmbedBuilder {
        description.append(c)
        return this
    }

    fun append(any: Any?) = append(((any as? IMentionable)?.asMention) ?: any.toString())

    fun appendln(any: Any?) = append(any).appendln()

    fun appendln() = append("\n")

    operator fun plusAssign(any: Any?) { append(any) }

    operator fun String.unaryPlus(): KEmbedBuilder {
        description.append(this)
        return this@KEmbedBuilder
    }

    inline fun image(lazy: () -> String): KEmbedBuilder {
        image = lazy()
        return this
    }

    inline fun url(lazy: () -> String): KEmbedBuilder {
        url = lazy()
        return this
    }

    inline fun title(lazy: () -> String): KEmbedBuilder {
        title = lazy()
        return this
    }

    inline fun thumbnail(lazy: () -> String): KEmbedBuilder {
        thumbnail = lazy()
        return this
    }

    inline fun time(lazy: () -> TemporalAccessor): KEmbedBuilder {
        time = lazy()
        return this
    }

    inline fun color(lazy: () -> Color?): KEmbedBuilder {
        color = lazy()
        return this
    }

    inline fun author(lazy: Entity.() -> Unit): KEmbedBuilder {
        val data = Entity()
        data.lazy()
        author = data
        return this
    }

    inline fun footer(lazy: Entity.() -> Unit): KEmbedBuilder {
        val data = Entity()
        data.lazy()
        footer = data
        return this
    }

    inline fun field(name: String = EmbedBuilder.ZERO_WIDTH_SPACE,
                     inline: Boolean = true,
                     lazy: Field.() -> Unit): KEmbedBuilder {
        val builder = Field(name = name, inline = inline)
        builder.lazy()
        fields.add(MessageEmbed.Field(builder.name, builder.value.toString(), builder.inline))
        return this
    }

    @MessageDsl
    data class Entity @PublishedApi internal constructor(var value: String = EmbedBuilder.ZERO_WIDTH_SPACE,
                                                         var url: String? = null,
                                                         var icon: String? = null) {
        inline fun value(lazy: () -> String): Entity {
            val value = lazy()
            this.value = value.modifyIf(value.isBlank()) { EmbedBuilder.ZERO_WIDTH_SPACE }
            return this
        }

        inline fun url(lazy: () -> String?): Entity {
            url = lazy()
            return this
        }

        inline fun icon(lazy: () -> String?): Entity {
            icon = lazy()
            return this
        }
    }

    @MessageDsl
    data class Field @PublishedApi internal constructor(
        var name: String = EmbedBuilder.ZERO_WIDTH_SPACE,
        var inline: Boolean = true
    ): Appendable {
        @PublishedApi
        internal val value = StringBuilder()

        operator fun String.unaryPlus(): Field {
            append(this)
            return this@Field
        }

        override fun append(csq: CharSequence?): Field {
            value.append(csq)
            return this
        }

        override fun append(csq: CharSequence?, start: Int, end: Int) = append(csq?.subSequence(start..end))

        override fun append(c: Char) = append(c.toString())

        fun appendln(any: Any?) = append(any).appendln()

        fun appendln() = append(lineSeparator)

        fun append(any: Any?) = append(((any as? IMentionable)?.asMention) ?: any.toString())
    }
}
