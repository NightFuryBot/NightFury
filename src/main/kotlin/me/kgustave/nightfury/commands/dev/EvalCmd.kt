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
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.db.sql.closeAfter
import me.kgustave.nightfury.db.sql.executeQuery
import me.kgustave.nightfury.db.sql.prepare
import net.dv8tion.jda.core.entities.ChannelType
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify script to evaluate.")
class EvalCmd : Command()
{
    internal var engine : ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")

    init {
        this.name = "Eval"
        this.help = "Evaluates using a ScriptEngine."
        this.arguments = "[Script]"
        this.devOnly = true
        this.category = Category.MONITOR
        this.guildOnly = false
        this.children = arrayOf(EvalSQLCmd(engine), EvalEngineCmd(this))
    }

    override fun execute(event: CommandEvent)
    {
        val args = event.args

        when {
            args matches Regex("9\\s*\\+\\s*10") -> event.reply("```java\n$args```Evaluated:\n```\n21```")

            args matches Regex("requires\\(\"discord\\.js\"\\);?") -> event.reply("```java\n$args```Evaluated:\n```\nNo```")

            args matches Regex("System\\.exit\\(\\d+\\);?") -> {
                event.replyWarning("Shutting down...")
                Thread.sleep(4000)
                event.reply("Naaaah, just kidding!")
            }

            else -> try {

                val output = engine load event eval args

                event.reply("```java\n$args```Evaluated:\n```\n$output```")
            } catch (e: ScriptException) {
                event.reply("```java\n$args```A ScriptException was thrown:\n```\n${e.message}```")
            } catch (e: Exception) {
                event.reply("```java\n$args```An exception was thrown:\n```\n$e```")
            }
        }
    }
}

@MustHaveArguments("SQL Evaluation requires an SQL statement ending in a `;` and a result set function!")
private class EvalSQLCmd(private var engine: ScriptEngine) : Command()
{
    init {
        this.name = "SQL"
        this.help = "Evaluates SQL."
        this.arguments = "[Script]"
        this.devOnly = true
        this.category = Category.MONITOR
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val parts = event.args.split(Regex(";"),2)

        if(parts.size<2)
            return event.replyError("SQL Evaluation requires an SQL statement ending in a `;` and a result set function!")

        event.manager.connection prepare parts[0] closeAfter {
            executeQuery {
                engine.put("results", it)
                try {
                    val output = engine load event eval parts[1]
                    event.reply("SQL:```sql\n${parts[0]}```Function:```java\n${parts[1]}```Evaluated:\n```\n$output```")
                } catch (e: ScriptException) {
                    event.reply("SQL:```sql\n${parts[0]}```Function:```java\n${parts[1]}```A ScriptException was thrown:\n```\n${e.message}```")
                } catch (e: Exception) {
                    event.reply("SQL:```sql\n${parts[0]}```Function:```java\n${parts[1]}```An exception was thrown:\n```\n$e```")
                }
            }
        }
    }
}

@MustHaveArguments("Specify an engine to switch to!")
private class EvalEngineCmd(private var base: EvalCmd) : Command()
{
    init {
        this.name = "Engine"
        this.fullname = "Eval Engine"
        this.help = "Changes the current script engine."
        this.arguments = "[Engine]"
        this.devOnly = true
        this.category = Category.MONITOR
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val newEngine = ScriptEngineManager().getEngineByName(event.args)
                ?: return event.replyError("${event.args} is not a valid ScriptEngine!")

        base.engine = newEngine
        event.reply("Switched to ${base.engine.factory.engineName}")
    }
}

private infix inline fun <reified T : ScriptEngine> T.eval(script: String) = eval(script)

private infix inline fun <reified T : ScriptEngine> T.load(event: CommandEvent) : T {
    // STANDARD
    put("event", event)
    put("args", event.args)
    put("jda", event.jda)
    put("author", event.author)
    put("channel", event.channel)
    put("client", event.client)
    put("manager", event.manager)

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