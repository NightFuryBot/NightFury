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
import xyz.nightfury.CooldownScope
import xyz.nightfury.entities.embed
import xyz.nightfury.extensions.*
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import xyz.nightfury.annotations.HasDocumentation

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class AvatarCmd : Command() {
    init {
        this.name = "Avatar"
        this.aliases = arrayOf("avy", "pfp")
        this.arguments = "<User>"
        this.help = "Gets a user's avatar."
        this.cooldown = 5
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val user = if(event.isFromType(ChannelType.TEXT)) {
            if(event.args.isNotEmpty()) {
                val members = event.guild findMembers event.args
                if(members.isEmpty())
                    return event.replyError(noMatch("members", event.args))
                if(members.size > 1)
                    return event.replyError(members multipleMembers event.args)
                members[0].user
            } else event.author
        } else {
            if(event.args.isNotEmpty()) {
                val users = event.jda findUsers event.args
                if(users.isEmpty()) // None found
                    return event.replyError(noMatch("users", event.args))
                if(users.size > 1) // More than one found
                    return event.replyError(users multipleUsers event.args)
                users[0] // Back up to if
            } else event.author
        }

        event.reply(embed {
            title { "Avatar For ${user.formattedName(true)}" }

            if(event.isFromType(ChannelType.TEXT)) {
                val member = event.guild.getMember(user)
                color { if(member!=null) member.color else event.selfMember.color }
            }

            "${user.effectiveAvatarUrl}?size=1024".apply { url { this } image { this } }
        })
    }
}