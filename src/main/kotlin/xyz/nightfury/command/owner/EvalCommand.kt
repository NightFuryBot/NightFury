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
        private const val SCRIPT_ENGINE_EXT = "kts"
        private val ENGINE_MANAGER = ScriptEngineManager()
        private val SYS_EXIT_REGEX = Regex("(?:System\\.)?exit(?:Process)?\\(\\d+\\);?")
        private val DEFAULT_IMPORTS = arrayOf(
            "xyz.nightfury.util.db.*",
            "xyz.nightfury.util.jda.*",
            "xyz.nightfury.util.*",
            "xyz.nightfury.*",
            "kotlinx.coroutines.experimental.*",
            "kotlin.io.*",
            "java.util.function.*",
            "java.util.*",
            "me.kgustave.json.*"
        )
    }

    override val name = "Eval"
    override val help = "Evaluates using a ScriptEngine."
    override val arguments = "[Script]"
    override val hasAdjustableLevel = false

    private val engine: ScriptEngine = ENGINE_MANAGER.getEngineByExtension(SCRIPT_ENGINE_EXT)
    private val engineContext = Context()

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
                    engineContext.load(ctx)
                    val output = engine.eval("${engineContext.scriptPrefix}\n$args")
                    ctx.reply("```kotlin\n$args```Evaluated:\n```\n$output```")
                } catch (e: ScriptException) {
                    ctx.reply("```kotlin\n$args```A ScriptException was thrown:\n```\n${e.message}```")
                } catch (e: Exception) {
                    ctx.reply("```kotlin\n$args```An exception was thrown:\n```\n$e```")
                }

                engineContext.clear()
            }
        }
    }

    private fun Context.load(ctx: CommandContext) {
        // STANDARD
        this["ctx"] = ctx
        this["jda"] = ctx.jda
        this["author"] = ctx.author
        this["channel"] = ctx.channel
        this["client"] = ctx.client

        // GUILD
        if(ctx.isGuild) {
            this["guild"] = ctx.guild
            this["member"] = ctx.member

            this["textChannel"] = ctx.textChannel
            // VOICE
            if(ctx.selfMember.connectedChannel !== null) {
                this["voiceChannel"] = ctx.selfMember.connectedChannel
                this["voiceState"] = ctx.member.voiceState
            }
        }

        // PRIVATE
        if(ctx.isPrivate) {
            this["privateChannel"] = ctx.privateChannel
        }
    }

    private inner class Context {
        val properties = HashMap<String, Any?>()
        val imports = HashSet<String>()

        operator fun set(key: String, value: Any?) {
            properties[key] = value
            if(value !== null) {
                val qualifiedName = value::class.qualifiedName
                if(qualifiedName !== null && qualifiedName !in imports) {
                    imports += qualifiedName
                }
            }
            engine.put(key, value)
        }

        fun clear() {
            properties.clear()
            imports.clear()
        }

        val scriptPrefix get() = buildString {
            for(import in imports) {
                appendln("import $import")
            }

            for(import in DEFAULT_IMPORTS.filter { it !in imports }) {
                appendln("import $import")
            }

            for((key, value) in properties) {
                if(value === null) {
                    appendln("val $key = null")
                    continue
                }

                val simpleName = value::class.simpleName

                if(simpleName === null) {
                    appendln("val $key = bindings[\"$key\"]")
                    continue
                }

                appendln("val $key = bindings[\"$key\"] as $simpleName")
            }
        }
    }
}
