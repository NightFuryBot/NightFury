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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.MustHaveArguments
import org.slf4j.LoggerFactory

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a mode to set to.")
class ModeCmd : Command()
{
    init {
        this.name = "Mode"
        this.arguments = "[Standard, Idle, Debug]"
        this.help = "Sets the bots mode."
        this.guildOnly = false
        this.devOnly = true
        this.category = Category.MONITOR
        this.children = arrayOf(ModeLogCmd())
    }

    override fun execute(event: CommandEvent)
    {
        try {
            event.client.targetListener(event.args)
            event.replySuccess("Targeted listener `${event.args.toLowerCase()}`!")
        } catch (e : IllegalArgumentException) {
            if(e.message!=null) event.replyError(e.message!!)
            else throw e
        }
    }
}

private class ModeLogCmd : Command()
{
    init {
        this.name = "Log"
        this.fullname = "Mode Log"
        this.arguments = "[Info, Debug]"
        this.help = "Sets the bots log level."
        this.guildOnly = false
        this.devOnly = true
        this.category = Category.MONITOR
    }

    override fun execute(event: CommandEvent)
    {
        val level = when {
            event.args.equals("debug", true) -> Level.DEBUG
            event.args.equals("info", true) -> Level.INFO
            else -> return event.replyError("${event.args} is not a valid logger level!")
        }

        (LoggerFactory.getILoggerFactory() as LoggerContext).loggerList.forEach { it.level = level }

        event.replySuccess("Set log level to ${level.levelStr}")
    }
}