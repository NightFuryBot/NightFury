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
package xyz.nightfury.entities.starboard

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageReaction
import xyz.nightfury.extensions.embed
import xyz.nightfury.extensions.formattedName
import java.awt.Color

fun starColor(stars: Int): Color {
    require(stars > 0) { "Stars must be a minimum of 1" }

    return Color(255, 255, (25.44 * Math.min(stars, 10)).toInt())
}

fun MessageBuilder.generateStarEntry(message: StarMessage): MessageBuilder {
    append { message.toString() }
    return generateStarEntry(message.starred, message.count)
}

fun MessageBuilder.generateStarEntry(message: Message, stars: Int): MessageBuilder {
    embed {
        author {
            icon = message.author.effectiveAvatarUrl
            value = message.author.formattedName(false)
        }

        if(message.contentRaw.isNotEmpty())
            description { message.contentRaw }

        if(message.attachments.isNotEmpty())
            message.attachments[0].takeIf { it.isImage }?.let { image { it.url } }

        // Image embeds take precedence over attachments
        if(message.embeds.isNotEmpty())
            image { message.embeds[0].url }

        color { starColor(stars) }

        time { message.creationTime }
    }
    return this
}

inline val MessageReaction.isStarReaction: Boolean
    inline get() = when(reactionEmote.name) {
        "\u2B50", "\uD83C\uDF1F", "\uD83D\uDCAB" -> true
        else -> reactionEmote.name.contains("star", ignoreCase = true)
    }
