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
package me.kgustave.nightfury.commands.dev

import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.kjdautils.menu.*
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
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
        this.category = Category.MONITOR
        this.devOnly = true
        this.guildOnly = false
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    private val builder : PaginatorBuilder = PaginatorBuilder()
            .setFinalAction   { it.delete().queue() }
            .waitOnSinglePage { false }
            .useNumberedItems { true }
            .itemsPerPage     { 10 }
            .setText          { t, u -> "Page $t/$u" }
            .waiter           { waiter }


    override fun execute(event: CommandEvent)
    {
        builder.clearItems()
        event.jda.guilds.forEach { builder.add { "**${it.name}** (ID: ${it.id})" } }
        if(event.isFromType(ChannelType.TEXT))
            builder.color { event.member.color }
        builder.user      { event.author }
        builder.displayIn { event.channel }
    }
}