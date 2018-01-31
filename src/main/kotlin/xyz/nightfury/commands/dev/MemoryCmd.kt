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

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.entities.embed
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType

/**
 * @author Kaidan Gustave
 */
class MemoryCmd : Command() {
    companion object {
        private const val mb = 1024 * 1024
    }

    init {
        this.name = "Memory"
        this.help = "Gets NightFury's runtime memory statistics."
        this.devOnly = true
        this.category = Category.SHENGAERO
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val runtime = Runtime.getRuntime()

        event.reply(embed {
            title { "NightFury Runtime Statistics" }
            if(event.isFromType(ChannelType.TEXT)) color { event.selfMember.color }
            appendln("```ini")
            appendln("[ Current Memory Usage ]     ${(runtime.totalMemory() - runtime.freeMemory()) / mb}mb")
            appendln("[ Free Memory Available ]    ${runtime.freeMemory() / mb}mb")
            appendln("[ Total Memory Usage ]       ${runtime.totalMemory() / mb}mb")
            append("[ Maximum Memory Available ] ${runtime.maxMemory() / mb}mb")
            append("```")
        })
    }
}
