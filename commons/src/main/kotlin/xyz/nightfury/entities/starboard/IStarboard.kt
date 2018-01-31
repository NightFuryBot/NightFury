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
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User

/**
 * @author Kaidan Gustave
 */
interface IStarboard<M: IStarMessage<M,*>>: MutableMap<Long, M> {
    var channel: TextChannel?

    var threshold: Int

    var maxAge: Int

    fun addStar(user: User, starred: Message)

    fun deletedMessage(messageId: Long)

    companion object {
        const val DEFAULT_THRESHOLD = 5
        const val DEFAULT_MAX_AGE = 72
    }
}