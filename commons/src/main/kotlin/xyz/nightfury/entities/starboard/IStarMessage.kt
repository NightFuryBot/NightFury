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

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.util.Emojis

/**
 * @author Kaidan Gustave
 */
interface IStarMessage<M: IStarMessage<M, B>, B: IStarboard<M>> {
    val starboard: B
    val starred: Message
    val starReactions: MutableList<StarReaction>

    val entryIsCreated: Boolean
    val entry: Message

    val count: Int
    val starType: Emojis.Star

    fun addStar(user: User)

    fun createEntry()

    fun isStarring(user: User): Boolean

    fun removeStar(user: User)

    fun delete()

    fun updateEntry()
}