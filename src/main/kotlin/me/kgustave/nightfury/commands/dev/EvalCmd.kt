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

import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.connectedChannel
import me.kgustave.nightfury.music.MusicManager
import net.dv8tion.jda.core.entities.ChannelType
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify script to evaluate.")
class EvalCmd(val musicManager: MusicManager) : Command()
{
    internal var engine : ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")

    init {
        this.name = "Eval"
        this.help = "Evaluates using a ScriptEngine."
        this.arguments = "[Script]"
        this.devOnly = true
        this.category = Category.SHENGAERO
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val args = if(event.args.startsWith("```") && event.args.endsWith("```")) {
            event.args.substring(event.args.indexOf('\n')+1, event.args.length-3)
        } else event.args

        when {
            args matches Regex("9\\s*\\+\\s*10") -> event.reply("```java\n$args```Evaluated:\n```\n21```")

            args matches Regex("requires\\(\"discord\\.js\"\\);?") -> event.reply("```js\n$args```Evaluated:\n```\nNo```")

            args matches Regex("System\\.exit\\(\\d+\\);?") -> {
                event.replyWarning("Shutting down...")
                Thread.sleep(4000)
                event.reply("Naaaah, just kidding!")
            }

            else -> try {
                val output = engine load event eval args

                event.reply("```js\n$args```Evaluated:\n```\n$output```")
            } catch (e: ScriptException) {
                event.reply("```js\n$args```A ScriptException was thrown:\n```\n${e.message}```")
            } catch (e: Exception) {
                event.reply("```js\n$args```An exception was thrown:\n```\n$e```")
            }
        }
    }

    private infix inline fun <reified T : ScriptEngine> T.eval(script: String) = eval(script)

    private infix inline fun <reified T : ScriptEngine> T.load(event: CommandEvent) : T {
        // STANDARD
        put("event", event)
        put("jda", event.jda)
        put("author", event.author)
        put("channel", event.channel)
        put("client", event.client)
        put("manager", event.manager)
        put("musicManager", musicManager)

        // GUILD
        if(event.isFromType(ChannelType.TEXT))
        {
            put("guild", event.guild)
            put("member", event.member)
            put("textChannel", event.textChannel)

            // VOICE
            if(event.selfMember.connectedChannel != null)
            {
                put("voiceChannel", event.selfMember.connectedChannel)
                put("voiceState", event.member.voiceState)
            }
        }

        // PRIVATE
        if(event.isFromType(ChannelType.PRIVATE))
        {
            put("privateChannel", event.privateChannel)
        }

        return this
    }
}