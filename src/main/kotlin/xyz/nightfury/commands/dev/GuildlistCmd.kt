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
package xyz.nightfury.commands.dev

import xyz.nightfury.entities.menus.Paginator
import xyz.nightfury.entities.menus.EventWaiter
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType

/**
 * @author Kaidan Gustave
 */
class GuildlistCmd(waiter: EventWaiter) : Command()
{
    init {
        this.name = "Guildlist"
        this.help = "Gets a list of all guilds on this shard."
        this.category = Category.SHENGAERO
        this.devOnly = true
        this.guildOnly = false
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    private val builder : Paginator.Builder = Paginator.Builder()
            .finalAction      { it.delete().queue() }
            .waitOnSinglePage { false }
            .numberItems      { true }
            .itemsPerPage     { 10 }
            .text             { t, u -> "Page $t/$u" }
            .waiter           { waiter }


    override fun execute(event: CommandEvent)
    {
        builder.clearItems()
        event.jda.guilds.forEach { builder.add { "**${it.name}** (ID: ${it.id})" } }
        if(event.isFromType(ChannelType.TEXT))
            builder.color { _,_ -> event.member.color }
        builder.user      { event.author }
        builder.displayIn { event.channel }
    }
}