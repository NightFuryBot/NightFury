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
package xyz.nightfury.command.standard

import xyz.nightfury.NightFury
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.ext.await
import xyz.nightfury.util.ext.formattedName

/**
 * @author Kaidan Gustave
 */
class HelpCommand: Command(StandardGroup) {
    override val name = "Help"
    override val help = "Gets a list of all commands."
    override val guildOnly = false

    override suspend fun execute(ctx: CommandContext) {
        val prefix = ctx.client.prefix
        val message = buildString {
            appendln("**Available Commands in ${if(ctx.isGuild) ctx.textChannel.asMention else "Direct Messages"}**")
            ctx.client.groups.forEach { g ->
                // They can't use the command group so we don't display it here
                if(!g.check(ctx))
                    return@forEach
                appendln()
                appendln("__${g.name} Commands__")
                appendln()
                g.commands.forEach { c ->
                    append("`").append(prefix).append(c.name)
                    val arguments = c.arguments
                    if(arguments.isNotBlank()) {
                        append(" $arguments")
                    }
                    appendln("` - ${c.help}")
                }
            }

            val shen = ctx.jda.retrieveUserById(NightFury.DEV_ID).await()
            appendln()
            append("For additional help contact ${shen.formattedName(true)} or join " +
                   "my support server: **<${NightFury.SERVER_INVITE}>**")
        }

        if(ctx.isGuild)
            ctx.reactSuccess()
        ctx.replyInDM(message)
    }
}
