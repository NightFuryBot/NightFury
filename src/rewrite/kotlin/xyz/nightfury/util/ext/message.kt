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
@file:Suppress("Unused", "HasPlatformType")
package xyz.nightfury.util.ext

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.requests.restaction.MessageAction
import xyz.nightfury.entities.KEmbedBuilder

inline fun message(builder: MessageBuilder = MessageBuilder(), init: MessageBuilder.() -> Unit): Message {
    builder.init()
    return builder.build()
}

inline fun <reified C: MessageChannel> C.send(init: MessageBuilder.() -> Unit) = sendMessage(message { init() })

inline fun MessageBuilder.embed(crossinline init: KEmbedBuilder.() -> Unit): MessageBuilder
    = setEmbed(xyz.nightfury.entities.embed { init() })

inline fun <reified M: Message> M.edit(text: String): MessageAction = editMessage(text)
inline fun <reified M: Message> M.edit(embed: MessageEmbed): MessageAction = editMessage(embed)
inline fun <reified M: Message> M.edit(message: Message): MessageAction = editMessage(message)


