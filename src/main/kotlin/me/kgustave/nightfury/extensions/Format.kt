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
package me.kgustave.nightfury.extensions

import net.dv8tion.jda.core.entities.*
import java.util.regex.Pattern

object ArgumentPatterns
{
    val discordID: Pattern = Regex("(\\d{17,20})").toPattern()
    val userMention: Pattern = Regex("<@!?(\\d{17,20})>").toPattern()
    val reasonPattern = Regex("(^.+)\\s(?:for\\s+)([\\s\\S]+)$", RegexOption.DOT_MATCHES_ALL).toPattern()
    val targetIDWithReason: Pattern = Regex("(\\d{17,20})(?:\\s+(?:for\\s+)?([\\s\\S]+))?").toPattern()
    val targetMentionWithReason: Pattern = Regex("<@!?(\\d{17,20})>(?:\\s+(?:for\\s+)?([\\s\\S]+))?").toPattern()
}

infix fun List<User>.multipleUsers(argument: String) = listOut("user", argument) { it.formattedName(true) }
infix fun List<Member>.multipleMembers(argument: String) = listOut("member", argument) { it.user.formattedName(true) }
infix fun List<TextChannel>.multipleTextChannels(argument: String) = listOut("text channel", argument) { it.asMention }
infix fun List<VoiceChannel>.multipleVoiceChannels(argument: String) = listOut("voice channel", argument) { it.name }
infix fun List<Role>.multipleRoles(argument: String) = listOut("role", argument) { it.name }

private inline fun <T> List<T>.listOut(kind: String, argument: String, conversion: (T) -> String) = with(StringBuilder()) {
    append("Multiple ${kind}s found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${this@listOut[i].let(conversion)}\n")
        if(i==3 && this@listOut.size>4)
            append("And ${this@listOut.size-4} other $kind${if(this@listOut.size-4 > 1) "s..." else "..."}")
        if(this@listOut.size==i+1)
            break
    }
    return@with toString()
}

fun noMatch(lookedFor: String, query: String) = "Could not find any $lookedFor matching \"$query\"!"

infix fun User.formattedName(boldName: Boolean) = "${if(boldName) "**$name**" else name}#$discriminator"