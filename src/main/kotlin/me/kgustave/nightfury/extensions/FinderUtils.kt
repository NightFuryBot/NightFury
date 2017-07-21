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
package me.kgustave.nightfury.extensions

import com.jagrosh.jdautilities.utils.FinderUtil
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*

class Find
{
    companion object
    {
        fun users(query: String, jda: JDA) : MutableList<User> = FinderUtil.findUsers(query, jda)
        fun members(query: String, guild: Guild) : MutableList<Member> = FinderUtil.findMembers(query, guild)
        fun textChannels(query: String, guild: Guild) : MutableList<TextChannel> = FinderUtil.findTextChannels(query, guild)
        fun textChannels(query: String, jda: JDA) : MutableList<TextChannel> = FinderUtil.findTextChannels(query, jda)
        fun voiceChannels(query: String, guild: Guild) : MutableList<VoiceChannel> = FinderUtil.findVoiceChannels(query, guild)
        fun voiceChannels(query: String, jda: JDA) : MutableList<VoiceChannel> = FinderUtil.findVoiceChannels(query, jda)
        fun roles(query: String, guild: Guild) : MutableList<Role> = FinderUtil.findRoles(query, guild)
    }
}