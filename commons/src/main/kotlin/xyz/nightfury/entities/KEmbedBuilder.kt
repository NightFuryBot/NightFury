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
@file:Suppress("unused")
package xyz.nightfury.entities

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color
import java.time.temporal.TemporalAccessor

// Inspired by club.minnced.kjda.builders.KJDAEmbedBuilder

/**
 * @author Kaidan Gustave
 */
class KEmbedBuilder internal constructor() : Appendable {
    val fields: MutableList<MessageEmbed.Field> = mutableListOf()

    var description = StringBuilder()
    var title: String? = null
    var url: String? = null
    var thumbnail: String? = null
    var image: String? = null

    var author: KEmbedEntity? = null
    var footer: KEmbedEntity? = null
    var time: TemporalAccessor? = null
    var color: Color? = null

    internal fun build(): MessageEmbed = EmbedBuilder().apply {
        val(description, fields, title, url, time, color, author, thumbnail, footer, image) = this@KEmbedBuilder

        fields.forEach { addField(it) }

        if(!description.isBlank())
            setDescription(description.toString())
        if(!title.isNullOrBlank())
            setTitle(title, url)
        if(image !== null)
            setImage(image)
        if(time !== null)
            setTimestamp(time)
        if(thumbnail !== null)
            setThumbnail(thumbnail)
        if(color !== null)
            setColor(color)
        if(footer !== null)
            setFooter(footer.value, footer.icon)
        if(author !== null)
            setAuthor(author.value, author.url, author.icon)

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

    operator fun plusAssign(init: FieldBuilder.() -> Unit) = with(FieldBuilder()) {
        field(init)
        Unit
    }

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

    infix fun append(any: Any?) = append(any.normalize())

    infix fun appendln(any: Any?) = append(any).appendln()

    fun appendln() = append("\n")

    operator fun plusAssign(any: Any?) { append(any) }

    inline infix fun description(lazy: () -> String): KEmbedBuilder {
        description = StringBuilder(lazy())
        return this
    }

    inline infix fun image(lazy: () -> String): KEmbedBuilder {
        image = lazy()
        return this
    }

    inline infix fun url(lazy: () -> String): KEmbedBuilder {
        url = lazy()
        return this
    }

    inline infix fun title(lazy: () -> String): KEmbedBuilder {
        title = lazy()
        return this
    }

    inline infix fun thumbnail(lazy: () -> String): KEmbedBuilder {
        thumbnail = lazy()
        return this
    }

    inline infix fun time(lazy: () -> TemporalAccessor): KEmbedBuilder {
        time = lazy()
        return this
    }

    inline infix fun color(lazy: () -> Color?): KEmbedBuilder {
        color = lazy()
        return this
    }

    inline infix fun author(lazy: KEmbedEntity.() -> Unit): KEmbedBuilder {
        val data = KEmbedEntity()
        data.lazy()
        author = data
        return this
    }

    inline infix fun footer(lazy: KEmbedEntity.() -> Unit): KEmbedBuilder {
        val data = KEmbedEntity()
        data.lazy()
        footer = data
        return this
    }

    inline infix fun field(lazy: FieldBuilder.() -> Unit): KEmbedBuilder {
        val builder = FieldBuilder()
        builder.lazy()
        fields.add(MessageEmbed.Field(builder.name, builder.value, builder.inline))
        return this
    }
}

class KEmbedEntity(var value: String = EmbedBuilder.ZERO_WIDTH_SPACE,
                   var url: String? = null,
                   var icon: String? = null)

class FieldBuilder : Appendable {
    var name: String  = EmbedBuilder.ZERO_WIDTH_SPACE
    var value: String  = EmbedBuilder.ZERO_WIDTH_SPACE
    var inline = true

    operator fun plusAssign(any: Any?) {
        append(any)
    }

    override fun append(csq: CharSequence?): FieldBuilder {
        value += csq
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int) = append(csq?.subSequence(start..end))

    override fun append(c: Char) = append(c.toString())

    infix fun appendln(any: Any?) = append(any).appendln()

    fun appendln() = append(System.lineSeparator())

    fun append(any: Any?) = append(any.normalize())
}

internal fun Any?.normalize() = ((this as? IMentionable)?.asMention) ?: this.toString()

fun embed(init: KEmbedBuilder.() -> Unit): MessageEmbed = with (KEmbedBuilder()) {
    init()
    build()
}
