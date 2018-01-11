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
package xyz.nightfury.extensions

import com.jagrosh.jdautilities.utils.FinderUtil
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*

infix fun JDA.findUsers(query: String) : List<User>                   = FinderUtil.findUsers(query, this)
infix fun JDA.findTextChannels(query: String) : List<TextChannel>     = FinderUtil.findTextChannels(query, this)
infix fun JDA.findVoiceChannels(query: String) : List<VoiceChannel>   = FinderUtil.findVoiceChannels(query, this)
infix fun JDA.findCategories(query: String) : List<Category>          = FinderUtil.findCategories(query, this)
infix fun JDA.findEmotes(query: String) : List<Emote>                 = FinderUtil.findEmotes(query, this)
infix fun Guild.findBannedUsers(query: String) : List<User>?          = FinderUtil.findBannedUsers(query, this)
infix fun Guild.findMembers(query: String) : List<Member>             = FinderUtil.findMembers(query, this)
infix fun Guild.findTextChannels(query: String) : List<TextChannel>   = FinderUtil.findTextChannels(query, this)
infix fun Guild.findVoiceChannels(query: String) : List<VoiceChannel> = FinderUtil.findVoiceChannels(query, this)
infix fun Guild.findCategories(query: String) : List<Category>        = FinderUtil.findCategories(query, this)
infix fun Guild.findEmotes(query: String) : List<Emote>               = FinderUtil.findEmotes(query, this)
infix fun Guild.findRoles(query: String) : List<Role>                 = FinderUtil.findRoles(query, this)
