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

import kotlinx.coroutines.experimental.delay
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.util.jda.connectedChannel
import xyz.nightfury.util.modifyIf
import java.util.concurrent.TimeUnit
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class EvalCommand: Command(OwnerGroup) {
    private companion object {
        private const val SCRIPT_ENGINE_NAME = "nashorn"
        private val ENGINE_MANAGER = ScriptEngineManager()
        private val SYS_EXIT_REGEX = Regex("System\\.exit\\(\\d+\\);?")
    }

    override val name = "Eval"
    override val help = "Evaluates using a ScriptEngine."
    override val arguments = "[Script]"
    override val hasAdjustableLevel = false

    private val engine: ScriptEngine = ENGINE_MANAGER.getEngineByName(SCRIPT_ENGINE_NAME)

    override suspend fun execute(ctx: CommandContext) {
        // Trim off code block if present.
        val args = ctx.args.let { args ->
            args.modifyIf(args.startsWith("```") && args.endsWith("```")) {
                args.substring(args.indexOf('\n') + 1, it.length - 3)
            }
        }

        when {
            args matches SYS_EXIT_REGEX -> {
                val message = ctx.sendWarning("Shutting Down...")
                delay(4, TimeUnit.SECONDS)
                message.editMessage("Naaaah, just kidding!").queue()
            }

            else -> {
                try {
                    val output = engine.load(ctx).eval(args)
                    ctx.reply("```js\n$args```Evaluated:\n```\n$output```")
                } catch (e: ScriptException) {
                    ctx.reply("```js\n$args```A ScriptException was thrown:\n```\n${e.message}```")
                } catch (e: Exception) {
                    ctx.reply("```js\n$args```An exception was thrown:\n```\n$e```")
                }
            }
        }
    }

    private inline fun <reified T: ScriptEngine> T.load(ctx: CommandContext): T {
        // STANDARD
        put("ctx", ctx)
        put("jda", ctx.jda)
        put("author", ctx.author)
        put("channel", ctx.channel)
        put("client", ctx.client)

        // GUILD
        if(ctx.isGuild) {
            put("guild", ctx.guild)
            put("member", ctx.member)

            put("textChannel", ctx.textChannel)
            // VOICE
            if(ctx.selfMember.connectedChannel !== null) {
                put("voiceChannel", ctx.selfMember.connectedChannel)
                put("voiceState", ctx.member.voiceState)
            }
        }

        // PRIVATE
        if(ctx.isPrivate) {
            put("privateChannel", ctx.privateChannel)
        }

        return this
    }
}
