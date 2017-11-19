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
package xyz.nightfury.commands.dev

import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.annotations.MustHaveArguments
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import java.io.BufferedReader

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Cannot execute a empty command!")
class BashCmd : Command() {

    init {
        this.name = "Bash"
        this.aliases = arrayOf("$")
        this.arguments = "[Command] <Flags... <Flag Arguments...>>"
        this.help = "Executes a command in the Ubuntu OS."
        this.category = Category.SHENGAERO
        this.devOnly = true
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        val output = StringBuilder()
        val finalOutput: String
        try {
            val builder = ProcessBuilder()
            val p = builder.start()
            val reader = BufferedReader(InputStreamReader(p.inputStream))

            reader.useLines {
                for(runningLineOutput in it)
                    output.append(runningLineOutput).append("\n")
            }

            if(output.toString().isEmpty()) {
                return event.reply("Done, with no output!")
            }
            finalOutput = output.substring(0, output.length - 1)
        } catch (e: IOException) {
            return event.reply("I wasn't able to find the command `${event.args}`!")
        } catch (e: Exception) {
            ModLoggerFactory.getModLogger("Bash").apply {
                warn("An unknown error occurred!",e)
            }
            return event.replyError("An unknown error occurred!")
        }

        try {
            return event.reply("Input: ```\n${event.args}``` Output: \n```\n$finalOutput```")
        } catch (e: IllegalArgumentException) {
            ModLoggerFactory.getModLogger("Bash").info("Input: ${event.args}\nOutput: $finalOutput")
            event.reply("Command output too long! Output sent in console.")
        }

    }
}
