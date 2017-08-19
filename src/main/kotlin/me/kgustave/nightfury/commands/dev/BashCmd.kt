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
import java.io.IOException
import net.dv8tion.jda.core.utils.SimpleLog
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
        this.category = Category.MONITOR
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
            SimpleLog.getLog("Bash").apply {
                this.warn("An unknown error occurred!")
                this.log(e)
            }
            return event.replyError("An unknown error occurred!")
        }

        try {
            return event.reply("Input: ```\n${event.args}``` Output: \n```\n$finalOutput```")
        } catch (e: IllegalArgumentException) {
            SimpleLog.getLog("Bash").info("Input: ${event.args}\nOutput: $finalOutput")
            event.reply("Command output too long! Output sent in console.")
        }

    }
}