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

import net.dv8tion.jda.core.entities.MessageReaction
import java.awt.Color

fun starColor(stars: Int): Color {
    require(stars > 0) { "Stars must be a minimum of 1" }

    return Color(255, 255, (25.44 * Math.min(stars, 10)).toInt())
}

inline val MessageReaction.isStarReaction: Boolean inline get() {
    return when(reactionEmote.name) {
        "\u2B50", "\uD83C\uDF1F", "\uD83D\uDCAB" -> true
        else -> reactionEmote.name.contains("star", ignoreCase = true)
    }
}
