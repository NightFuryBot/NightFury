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
import xyz.nightfury.extensions.formattedName
import net.dv8tion.jda.core.Permission
import xyz.nightfury.annotations.HasDocumentation

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class InviteCmd(vararg requestedPerms : Permission) : Command() {
    companion object {
        private var perms : Long = 0
        private var id : Long = 0
        private val inviteFormat: String = "https://discordapp.com/oauth2/authorize?client_id=%d&permissions=%d&scope=bot"
        val invite : String
            get() = inviteFormat.format(id, perms)
    }

    init {
        this.name = "Invite"
        this.help = "Gets an invite link for NightFury."
        this.guildOnly = false

        var p: Long = 0
        requestedPerms.forEach { p += it.rawValue }
        perms = p
    }

    override fun execute(event: CommandEvent) {
        if(id == 0L)
            id = event.selfUser.idLong
        event.reply(buildString {
            appendln("NightFury is a general discord bot for moderation, utility, and larger communities!")
            appendln("To add me to your server, click the link below:")
            appendln()
            appendln("${event.client.success} **<$invite>**")
            appendln()
            appendln("To see a full list of my commands, type `${event.client.prefix}help`.")
            append("If you require additional help ")
            val owner = event.jda.getUserById(event.client.devId)
            if(owner != null)
                append("contact ${owner.formattedName(true)} or ")
            append("join my support server **<${event.client.server}>**")
        })
    }
}
