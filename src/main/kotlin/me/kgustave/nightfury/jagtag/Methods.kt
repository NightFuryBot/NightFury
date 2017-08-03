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
package me.kgustave.nightfury.jagtag

import com.jagrosh.jagtag.*
import me.kgustave.nightfury.extensions.findMembers
import me.kgustave.nightfury.extensions.findTextChannels
import me.kgustave.nightfury.extensions.findUsers
import me.kgustave.nightfury.utils.multipleMembersFound
import me.kgustave.nightfury.utils.multipleTextChannelsFound
import me.kgustave.nightfury.utils.multipleUsersFound
import me.kgustave.nightfury.utils.noMatch
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
fun getMethods() : Collection<Method>
{
    return arrayListOf(
            Method("user", ParseFunction { env ->
                env.get<User>("user").name
            }, ParseBiFunction { env, input ->
                if(input[0].isEmpty())
                    throw ParseException("Invalid 'user' statement")
                userSearch(env, input).name
            }),

            Method("nick", ParseFunction { env ->
                if(!env.contains("guild")) env.get<User>("user").name
                else env.get<Guild>("guild").getMember(env.get<User>("user")).run { nickname?:user.name }
            }, ParseBiFunction { env, input ->
                if(input[0].isEmpty())
                    throw ParseException("Invalid 'nick' statement")
                if(!env.contains("guild")) userSearch(env, input).name
                else env.get<Guild>("guild").getMember(userSearch(env, input)).run { nickname?:user.name }
            }),

            Method("discrim", ParseFunction { env ->
                env.get<User>("user").discriminator
            }, ParseBiFunction { env, input ->
                if(input[0].isEmpty())
                    throw ParseException("Invalid 'user' statement")
                userSearch(env, input).discriminator
            }),

            Method("@user", ParseFunction { env ->
                if(env.contains("guild"))
                    env.get<Guild>("guild").getMember(env.get<User>("user")).asMention
                else
                    env.get<User>("user").asMention
            }, ParseBiFunction { env, input ->
                if(input[0].isEmpty())
                    throw ParseException("Invalid '@user' statement")
                if(env.contains("guild"))
                    env.get<Guild>("guild").getMember(userSearch(env, input)).asMention
                else
                    userSearch(env, input).asMention
            }),

            Method("userid", ParseFunction { env ->
                env.get<User>("user").id
            }, ParseBiFunction { env, input ->
                if(input[0].isEmpty())
                    throw ParseException("Invalid 'userid' statement")
                userSearch(env, input).id
            }),

            Method("avatar", ParseFunction { env ->
                env.get<User>("user").avatarUrl
            }, ParseBiFunction { env, input ->
                if(input[0].isEmpty())
                    throw ParseException("Invalid 'avatar' statement")
                userSearch(env, input).avatarUrl
            }),

            Method("server", { env ->
                if(!env.contains("guild"))
                    throw TagErrorException("Tag is only available in a guild!")
                env.get<Guild>("guild").name
            }),

            Method("serverid", { env ->
                if(!env.contains("guild"))
                    throw TagErrorException("Tag is only available in a guild!")
                env.get<Guild>("guild").id
            }),

            Method("servercount", { env ->
                if(!env.contains("guild")) "1" else env.get<Guild>("guild").members.size.toString()
            }),

            Method("channel", ParseFunction { env ->
                if(!env.contains("guild")) "DM"
                else env.get<TextChannel>("channel").name
            }, ParseBiFunction { env, input ->
                channelSearch(env, input)?.name?:"DM"
            }),

            Method("channelid", ParseFunction { env ->
                if(!env.contains("guild")) "0"
                else env.get<TextChannel>("channel").id
            }, ParseBiFunction { env, input ->
                channelSearch(env, input)?.id?:"0"
            }),

            Method("#channel", ParseFunction { env ->
                if(!env.contains("guild")) "DM"
                else env.get<TextChannel>("channel").asMention
            }, ParseBiFunction { env, input ->
                channelSearch(env, input)?.asMention?:"DM"
            }),

            Method("randuser", ParseFunction { env ->
                if(!env.contains("guild"))
                    env.get<User>("user").name
                val guild = env.get<Guild>("guild")
                guild.members[(guild.members.size*Math.random()).toInt()].user.name
            }),

            Method("randonline", ParseFunction { env ->
                if(!env.contains("guild"))
                    env.get<User>("user").name
                val guild = env.get<Guild>("guild")
                val online = guild.members.stream().filter { it.onlineStatus == OnlineStatus.ONLINE }.toList()
                online[(online.size*Math.random()).toInt()].user.name
            }),

            Method("randchannel", ParseFunction { env ->
                if(!env.contains("guild")) "DM"
                else {
                    val guild = env.get<Guild>("guild")
                    guild.textChannels[(guild.textChannels.size * Math.random()).toInt()].name
                }
            })
    )
}

internal fun userSearch(env: Environment, input: Array<out String>) : User
{
    if(env.contains("guild")) { // is from guild
        with(env.get<Guild>("guild").findMembers(input[0])) {
            if(this.isEmpty())
                throw TagErrorException(noMatch("members", input[0]))
            if(this.size>1)
                throw TagErrorException(multipleMembersFound(input[0], this))
            return this[0].user
        }
    } else {
        with(env.get<User>("user").jda.findUsers(input[0])) {
            if(this.isEmpty())
                throw TagErrorException(noMatch("users", input[0]))
            if(this.size>1)
                throw TagErrorException(multipleUsersFound(input[0], this))
            return this[0]
        }
    }
}

internal fun channelSearch(env: Environment, input: Array<out String>) : TextChannel?
{
    if(!env.contains("guild"))
        return null
    if(input[0].isEmpty())
        throw ParseException("Invalid 'channel' statement")
    with(env.get<Guild>("guild").findTextChannels(input[0]))
    {
        if(this.isEmpty())
            throw TagErrorException(noMatch("channels", input[0]))
        if(this.size>1)
            throw TagErrorException(multipleTextChannelsFound(input[0], this))
        return this[0]
    }
}

class TagErrorException(msg: String) : RuntimeException(msg)