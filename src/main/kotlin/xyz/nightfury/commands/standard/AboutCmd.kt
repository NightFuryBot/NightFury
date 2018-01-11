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
import xyz.nightfury.NightFury
import xyz.nightfury.entities.embed
import xyz.nightfury.extensions.formattedName
import net.dv8tion.jda.core.JDAInfo
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import org.slf4j.LoggerFactory
import xyz.nightfury.annotations.HasDocumentation

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class AboutCmd(vararg val permissions : Permission) : Command() {
    private var oauthLink: String? = null
    private var isPublic: Boolean = true
    private val perms : Long

    init {
        this.name = "About"
        this.help = "Shows an overview of the bot."
        this.guildOnly = false
        perms = with(permissions) {
            var p = 0L
            this.forEach { p += it.rawValue }
            return@with p
        }
    }

    override fun execute(event: CommandEvent) {
        if(oauthLink == null && isPublic) {
            try {
                val info = event.jda.asBot().applicationInfo.complete()
                isPublic = info.isBotPublic
                if(isPublic) oauthLink = info.getInviteUrl(perms)
            } catch (e: Exception) {
                LoggerFactory.getLogger("OAuth2").error("Could not generate invite link: $e")
            }
        }
        val embed = embed {
            if(event.isFromType(ChannelType.TEXT))
                color { event.selfMember.color }
            author {
                this.value = "All About ${event.selfUser.name}"
                this.icon = event.selfUser.avatarUrl
            }
            appendln("Hello, I am **${event.selfUser.name}**!")
            appendln("I am a discord bot with many functions from utility, to moderation, to fun commands!")
            appendln("I was written in Kotlin by ${event.jda.retrieveUserById(event.client.devId).complete().formattedName(true)} " +
                    "using the [JDA Library](${JDAInfo.GITHUB}) (${JDAInfo.VERSION}).")
            appendln("I am at [Version ${NightFury.VERSION}](${NightFury.GITHUB}). To see a full list of my commands, type " +
                    "`${event.client.prefix}help`, or if you require additional assistance, join my [support server](${event.client.server})!")
            if(isPublic)
                appendln("If you want to invite me to your server, click [here]($oauthLink) or use `${event.client.prefix}invite`!")
            val shard = event.jda.shardInfo
            thumbnail { event.selfUser.effectiveAvatarUrl }
            field {
                this.name = if(shard != null) "This Shard" else "Users"
                this.value = "${event.jda.users.size} Unique${if(shard != null) " Users" else ""}\n" +
                        if(shard != null)
                            "${event.jda.guilds.size} Servers"
                        else
                            "${event.jda.guilds.stream().mapToInt { it.members.size }.sum()} Total"
                this.inline = true
            }
            field {
                this.name = if(shard != null) "" else "Channels"
                this.value = "${event.jda.textChannels.size} Text${if(shard != null) " Channels" else ""}\n" +
                        "${event.jda.voiceChannels.size} Voice${if(shard != null) " Channels" else ""}"
                this.inline = true
            }
            field {
                this.name = "Stats"
                this.value = if(shard!=null) {
                    "${event.client.totalGuilds} Servers\n" +
                    "${event.client.messageCacheSize} Cached Messages\n" +
                    "Shard ${shard.shardId+1}"
                } else {
                    "${event.jda.guilds.size} Servers\n" +
                    "${event.client.messageCacheSize} Cached Messages"
                }
                this.inline = true
            }
            footer {
                this.value = "Last Restart"
                this.icon = null
            }
            time { event.client.startTime }
        }

        event.reply(embed)
    }
}
