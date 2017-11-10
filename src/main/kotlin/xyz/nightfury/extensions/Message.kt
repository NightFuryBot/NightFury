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
package xyz.nightfury.extensions

import xyz.nightfury.entities.KEmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import xyz.nightfury.entities.RestPromise
import xyz.nightfury.entities.promise

inline fun message(builder: MessageBuilder = MessageBuilder(), init: MessageBuilder.() -> Unit): Message {
    builder.init()
    return builder.build()
}

infix inline fun <reified C: MessageChannel> C.send(init: MessageBuilder.() -> Unit): RestPromise<Message>
    = sendMessage(message { init() }).promise()

infix inline fun MessageBuilder.embed(crossinline init: KEmbedBuilder.() -> Unit): MessageBuilder
    = setEmbed(xyz.nightfury.entities.embed { init() })

infix inline fun MessageBuilder.mention(lazy: () -> IMentionable): MessageBuilder = mention(lazy())

infix fun MessageBuilder.mention(lazy: IMentionable): MessageBuilder {
    append(lazy)
    return this
}

infix inline fun <reified M: Message> M.edit(block: () -> String): M = apply { editMessage(block()).queue() }