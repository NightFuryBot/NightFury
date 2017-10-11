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
@file:Suppress("UNUSED_PARAMETER")
package me.kgustave.nightfury.listeners

import me.kgustave.nightfury.Client
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.NightFury
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

/**
 * @author Kaidan Gustave
 */
interface CommandListener
{
    fun checkCall(event: MessageReceivedEvent, client: Client, name: String, args: String) = true
    fun onCommandCall(event: CommandEvent, command: Command) {}
    fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?) {}
    fun onCommandCompleted(event: CommandEvent, command: Command) {}
    fun onException(event: CommandEvent, command: Command, exception: Throwable) {}

    enum class Mode(val type: String, val listener: CommandListener)
    {
        STANDARD("standard", object : CommandListener
        {
            override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
            {
                if(msg != null) event.reply(msg)
            }

            override fun onException(event: CommandEvent, command: Command, exception: Throwable)
            {
                NightFury.LOG.error("The CommandListener caught an Exception!")
                NightFury.LOG.error("Command: ${command.name}")
                if(exception.message != null)
                    NightFury.LOG.error(exception.message)
                NightFury.LOG.debug("Full Stacktrace", exception)
            }
        }),

        IDLE("idle", object : CommandListener
        {
            override fun checkCall(event: MessageReceivedEvent,
                                   client: Client, name: String,
                                   args: String) = event.author.idLong == client.devId

            override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
            {
                if(msg != null) event.reply(msg)
            }

            override fun onException(event: CommandEvent, command: Command, exception: Throwable)
            {
                NightFury.LOG.error("The CommandListener caught an Exception!")
                if(exception.message != null)
                    NightFury.LOG.error(exception.message)
            }
        }),

        DEBUG("debug", object : CommandListener
        {
            override fun onCommandCall(event: CommandEvent, command: Command)
            {
                NightFury.LOG.debug("Call to Command \"${command.name}\"")
            }

            override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
            {
                if(msg != null) event.reply(msg)
                NightFury.LOG.debug("Terminated Command \"${command.name}\" with message: \"$msg\"")
            }

            override fun onCommandCompleted(event: CommandEvent, command: Command)
            {
                NightFury.LOG.debug("Completed Command \"${command.name}\"")
            }

            override fun onException(event: CommandEvent, command: Command, exception: Throwable)
            {
                NightFury.LOG.debug("Exception Caught for Command \"${command.name}\"",exception)
            }
        });

        companion object {
            fun typeOf(type: String) = values().firstOrNull { it.type.equals(type, true) }
        }
    }
}