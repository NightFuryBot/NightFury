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

import club.minnced.kjda.entities.connectedChannel
import me.kgustave.nightfury.Argument
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.extensions.Find
import net.dv8tion.jda.core.entities.ChannelType
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * @author Kaidan Gustave
 */
class EvalCmd : Command()
{
    init {
        this.name = "eval"
        this.help = "evaluates using Nashorn"
        this.arguments = Argument("<script>")
        this.devOnly = true
        this.category = Category.OWNER
        this.guildOnly = false
    }

    val engine : ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")

    override fun execute(event: CommandEvent)
    {
        val args = event.args

        loadSE(engine, event)

        try {
            event.reply("```java\n$args```Evaluated:\n```\n${engine.eval(args)}```")
        } catch (e: ScriptException) {
            event.reply("```java\n$args```A ScriptException was thrown:\n```\n${e.message}```")
        } catch (e: Exception) {
            event.reply("```java\n$args```An exception was thrown:\n```\n$e```")
        }
    }

    private fun loadSE(se: ScriptEngine, event: CommandEvent) : ScriptEngine
    {
        // STANDARD
        se.put("event", event)
        se.put("args", event.args)
        se.put("jda", event.jda)
        se.put("author", event.author)
        se.put("channel", event.channel)
        se.put("client", event.client)
        se.put("manager", event.manager)

        // GUILD
        if(event.isFromType(ChannelType.TEXT))
        {
            se.put("guild", event.guild)
            se.put("member", event.member)
            se.put("textChannel", event.textChannel)

            // VOICE
            if(event.selfMember.connectedChannel != null)
            {
                se.put("voiceChannel", event.selfMember.connectedChannel)
                se.put("voiceState", event.member.voiceState)
            }
        }

        // PRIVATE
        if(event.isFromType(ChannelType.PRIVATE))
        {
            se.put("privateChannel", event.privateChannel)
        }

        // UTILITY
        se.put("Find", Find)

        return se
    }
}