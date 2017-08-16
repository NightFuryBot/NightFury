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

val TARGET_ID_REASON : Pattern = Regex("(\\d{17,20})(?:\\s+(?:for\\s+)?([\\s\\S]+))?").toPattern()

val TARGET_MENTION_REASON : Pattern = Regex("<@!?(\\d{17,20})>(?:\\s+(?:for\\s+)?([\\s\\S]+))?").toPattern()

infix fun List<User>.multipleUsers(argument: String) =
        map { it.formattedName(true) }.listOut("user", argument)
infix fun List<Member>.multipleMembers(argument: String) =
        map { it.user.formattedName(true) }.listOut("member", argument)
infix fun List<TextChannel>.multipleTextChannels(argument: String) =
        map { it.asMention }.listOut("text channel", argument)
infix fun List<VoiceChannel>.multipleVoiceChannels(argument: String) =
        map { it.name }.listOut("voice channel", argument)
infix fun List<Role>.multipleRoles(argument: String) =
        map { it.name }.listOut("role", argument)

private fun List<String>.listOut(kind: String, argument: String) = with(StringBuilder())
{
    append("Multiple ${kind}s found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${this@listOut[i]}\n")
        if(i==3 && this@listOut.size>4)
            append("And ${this@listOut.size-4} other $kind${if(this@listOut.size-4 > 1) "s..." else "..."}")
        if(this@listOut.size==i+1)
            break
    }
    return@with toString()
}

fun noMatch(lookedFor: String, query: String) = "Could not find any $lookedFor matching \"$query\"!"

fun User.formattedName(boldName: Boolean) = "${if(boldName) "**${this.name}**" else this.name}#${this.discriminator}"