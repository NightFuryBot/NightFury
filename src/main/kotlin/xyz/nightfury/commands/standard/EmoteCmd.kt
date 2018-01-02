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
package xyz.nightfury.commands.standard

import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.entities.embed
import xyz.nightfury.extensions.readableFormat
import xyz.nightfury.resources.Arguments
import xyz.nightfury.resources.Emojis
import net.dv8tion.jda.core.Permission

/**
 * @author Kaidan Gustave
 */
class EmoteCmd : Command() {
    companion object {
        private val bullet = "\uD83D\uDD39"
    }

    init {
        this.name = "Emote"
        this.aliases = arrayOf("Emoji")
        this.arguments = "[Emote|Emoji]"
        this.help = "Gets info on an emote or unicode character."
        this.botPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val args = event.args
        if(args matches Arguments.emoteRegex) {
            val emotes = event.message.emotes
            if(emotes.size < 1)
                return event.replyError("The specified emote was fake, or could not be retrieved!")
            val emote = emotes[0]
            event.reply(embed {
                title { "Info on ${if(event.jda.getEmoteById(emote.idLong)==null) ":${emote.name}:" else emote.asMention}" }
                color { event.member?.color }

                append("$bullet **Name:** ${emote.name}\n")
                if(emote.guild != null)
                    append("$bullet **Guild:** ${emote.guild.name} (ID: ${emote.guild.id})\n")
                append("$bullet **Creation Date:** ${emote.creationTime.readableFormat}\n")
                append("$bullet **ID:** ${emote.id}\n")
                append("$bullet **Managed:** ${if(emote.isManaged) Emojis.GREEN_TICK else Emojis.RED_TICK}")
                image  { emote.imageUrl }
                footer { value = "Created at" }
                time   { emote.creationTime }
            })
        } else {
            val unicode = args.replace(" ", "")
            if(unicode.length > 10)
                return event.replyError("Cannot process more than 10 characters!")
            event.reply(embed {
                color { event.selfMember.color }
                title { "Unicode Information:" }
                unicode.codePoints().use {
                    it.forEach {
                        val chars = Character.toChars(it)
                        var hex = Integer.toHexString(it).toUpperCase()

                        while(hex.length<4) hex = "0"+hex

                        append("\n`\\u")
                        append(hex)
                        append("`   ")

                        if(chars.size>1) {
                            var hex0 = Integer.toHexString(chars[0].toInt()).toUpperCase()
                            var hex1 = Integer.toHexString(chars[1].toInt()).toUpperCase()
                            while(hex0.length<4) hex0 = "0"+hex0
                            while(hex1.length<4) hex1 = "0"+hex1
                            append("[`\\u")
                            append(hex0)
                            append("\\u")
                            append(hex1)
                            append("`]   ")
                        }

                        append(String(chars))
                        append("   _")
                        append(Character.getName(it))
                        append("_")
                    }
                }
            })
        }
    }
}