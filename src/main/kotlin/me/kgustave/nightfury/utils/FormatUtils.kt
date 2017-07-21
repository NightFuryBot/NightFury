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
package me.kgustave.nightfury.utils

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.util.regex.Pattern

val TARGET_ID_REASON : Pattern = Regex("(\\d{17,20})(?:\\s+(?:for\\s+)?([\\s\\S]+))?").toPattern()

val TARGET_MENTION_REASON : Pattern = Regex("<@!?(\\d{17,20})>(?:\\s+(?:for\\s+)?([\\s\\S]+))?").toPattern()

fun multipleUsersFound(argument: String, users: List<User>) : String = with(StringBuilder())
{
    append("Multiple users found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${formatUserName(users[i],true)}\n")
        if(i==3 && users.size>4)
            append("And ${users.size-4} other user${if(users.size-4 > 1) "s..." else "..."}")
        if(users.size==i+1)
            break
    }
    return@with toString()
}

fun multipleMembersFound(argument: String, members: List<Member>) : String = with(StringBuilder())
{
    append("Multiple members found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${formatUserName(members[i].user,true)}\n")
        if(i==3 && members.size>4)
            append("And ${members.size-4} other member${if(members.size-4 > 1) "s..." else "..."}")
        if(members.size==i+1)
            break
    }
    return@with toString()
}

fun multipleTextChannelsFound(argument: String, channels: List<TextChannel>) : String = with(StringBuilder())
{
    append("Multiple text channels found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${channels[i].asMention}\n")
        if(i==3 && channels.size>4)
            append("And ${channels.size-4} other member${if(channels.size-4 > 1) "s..." else "..."}")
        if(channels.size==i+1)
            break
    }
    return@with toString()
}

fun multipleVoiceChannelsFound(argument: String, channels: List<TextChannel>) : String = with(StringBuilder())
{
    append("Multiple voice channels found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${channels[i].asMention}\n")
        if(i==3 && channels.size>4)
            append("And ${channels.size-4} other member${if(channels.size-4 > 1) "s..." else "..."}")
        if(channels.size==i+1)
            break
    }
    return@with toString()
}

fun multipleRolesFound(argument: String, roles: List<Role>) : String = with(StringBuilder())
{
    append("Multiple roles found matching \"$argument\":\n")
    for(i in 0..3)
    {
        append("${roles[i].name}\n")
        if(i==3 && roles.size>4)
            append("And ${roles.size-4} other member${if(roles.size-4 > 1) "s..." else "..."}")
        if(roles.size==i+1)
            break
    }
    return@with toString()
}

fun noMatch(lookedFor: String, query: String) = "Could not find any $lookedFor matching \"$query\"!"

fun formatUserName(user: User, boldName: Boolean) = "${if(boldName) "**${user.name}**" else user.name}#${user.discriminator}"