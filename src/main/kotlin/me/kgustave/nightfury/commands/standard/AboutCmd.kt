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
package me.kgustave.nightfury.commands.standard

import club.minnced.kjda.builders.colorAwt
import club.minnced.kjda.builders.embed
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.NightFury
import me.kgustave.nightfury.utils.formatUserName
import net.dv8tion.jda.core.JDAInfo
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.utils.SimpleLog

/**
 * @author Kaidan Gustave
 */
class AboutCmd(vararg val permissions : Permission) : Command()
{

    private var oauthLink: String? = null
    private var isPublic: Boolean = true
    private val perms : Long

    init {
        this.name = "about"
        this.help = "shows an overview of the bot"
        this.guildOnly = false
        perms = with(permissions)
        {
            var p = 0L
            this.forEach { p += it.rawValue }
            return@with p
        }
    }

    override fun execute(event: CommandEvent)
    {
        if(oauthLink == null && isPublic)
        {
            try {
                val info = event.jda.asBot().applicationInfo.complete()
                isPublic = info.isBotPublic
                if(isPublic) oauthLink = info.getInviteUrl(perms)
            } catch (e: Exception) {
                SimpleLog.getLog("OAuth2").fatal("Could not generate invite link: $e")
            }
        }
        val embed = embed {
            if(event.isFromType(ChannelType.TEXT))
                colorAwt = event.selfMember.color
            author {
                this.value = "All About ${event.selfUser.name}"
                this.icon = event.selfUser.avatarUrl
            }
            appendln("Hello, I am **${event.selfUser.name}**!")
            appendln("I am a discord bot with many functions from utility, to moderation, to fun commands!")
            appendln("I was written in Kotlin by ${formatUserName(event.jda.retrieveUserById(event.client.devId).complete(),true)} " +
                    "using the [JDA Library](${JDAInfo.GITHUB}) (${JDAInfo.VERSION}).")
            appendln("I am at [Version ${NightFury.version}](${NightFury.github}). To see a full list of my commands, type " +
                    "`${event.client.prefix}help`, or if you require additional assistance, join my [support server](${event.client.server})!")
            if(isPublic)
                appendln("If you want to invite me to your server, click [here]($oauthLink) or use `${event.client.prefix}invite`!")
            val shard = event.jda.shardInfo
            thumbnail { event.selfUser.avatarUrl }
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