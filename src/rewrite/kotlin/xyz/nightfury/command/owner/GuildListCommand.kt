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
package xyz.nightfury.command.owner

import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.listeners.EventWaiter
import xyz.nightfury.util.menus.Paginator

/**
 * @author Kaidan Gustave
 */
class GuildListCommand(waiter: EventWaiter): Command(OwnerGroup) {
    override val name = "GuildList"
    override val help = "Gets a list of all guilds the bot is in."
    override val guildOnly = false

    private val builder = Paginator.Builder {
        waiter           { waiter }
        waitOnSinglePage { true }
        numberItems      { true }
        itemsPerPage     { 10 }
        text             { t, u -> "Page $t/$u" }
        finalAction      { it.delete().queue() }
    }

    override suspend fun execute(ctx: CommandContext) {
        builder.clearItems()
        val paginator = Paginator(builder) {
            ctx.jda.guilds.forEach { + "**${it.name}** (ID: ${it.id})" }
            if(ctx.isGuild) {
                color { _, _ -> ctx.member.color }
            }
            user { ctx.author }
        }
        paginator.displayIn(ctx.channel)
    }
}
