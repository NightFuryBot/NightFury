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
@file:Suppress("LiftReturnOrAssignment")

package xyz.nightfury.command.standard

import xyz.nightfury.NightFury
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.jda.await
import xyz.nightfury.util.formattedName

/**
 * @author Kaidan Gustave
 */
class InviteCommand : Command(StandardGroup) {
    override val name = "Invite"
    override val help = "Gets an invite link for NightFury."
    override val guildOnly = false
    override val hasAdjustableLevel = false

    private lateinit var oAuth2Link: String

    override suspend fun execute(ctx: CommandContext) {
        if(!::oAuth2Link.isInitialized || oAuth2Link.isEmpty()) {
            try {
                val info = ctx.jda.asBot().applicationInfo.await()
                oAuth2Link = info.getInviteUrl(*NightFury.permissions)
            } catch(t: Throwable) {
                NightFury.LOG.warn("Failed to generate OAuth2 URL!")
                return ctx.replyError("An unexpected error occurred!")
            }
        }

        ctx.reply(buildString {
            appendln("NightFury is a general discord bot for moderation, utility, and larger communities!")
            appendln("To add me to your server, click the link below:")
            appendln()
            appendln("${NightFury.SUCCESS} **<$oAuth2Link>**")
            appendln()
            appendln("To see a full list of my commands, type `${ctx.client.prefix}help`.")
            append("If you require additional help ")
            val owner = ctx.jda.retrieveUserById(NightFury.DEV_ID).await()
            if(owner != null)
                append("contact ${owner.formattedName(true)} or ")
            append("join my support server **<${NightFury.SERVER_INVITE}>**")
        })
    }
}
