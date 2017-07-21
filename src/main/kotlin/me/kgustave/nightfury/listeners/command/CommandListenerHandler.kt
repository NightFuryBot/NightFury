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
package me.kgustave.nightfury.listeners.command

import me.kgustave.nightfury.Client
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.NightFury
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.utils.SimpleLog

/**
 * @author Kaidan Gustave
 */
interface CommandListener
{
    companion object
    {
        abstract class BlankListener : CommandListener
        {
            override fun checkCall(event: MessageReceivedEvent, client: Client, name: String, args: String) : Boolean = true
            override fun onCommandCall(event: CommandEvent, command: Command){}
            override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?){}
            override fun onCommandCompleted(event: CommandEvent, command: Command){}
            override fun onException(event: CommandEvent, command: Command, exception: Throwable){}
        }

        class DefaultBlankListener : BlankListener()
    }

    fun checkCall(event: MessageReceivedEvent, client: Client, name: String, args: String) : Boolean
    fun onCommandCall(event: CommandEvent, command: Command)
    fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
    fun onCommandCompleted(event: CommandEvent, command: Command)
    fun onException(event: CommandEvent, command: Command, exception: Throwable)
}

class CommandListenerHandler : CommandListener
{
    var target : CommandListener = Companion.DEFAULT

    companion object
    {
        private val DEFAULT : CommandListener = CommandListener.Companion.DefaultBlankListener()
    }

    override fun checkCall(event: MessageReceivedEvent, client: Client, name: String, args: String): Boolean
    {
        return target.checkCall(event,client,name,args)
    }

    override fun onCommandCall(event: CommandEvent, command: Command)
    {
        target.onCommandCall(event,command)
    }

    override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
    {
        target.onCommandTerminated(event,command,msg)
    }

    override fun onCommandCompleted(event: CommandEvent, command: Command)
    {
        target.onCommandCompleted(event,command)
    }

    override fun onException(event: CommandEvent, command: Command, exception: Throwable)
    {
        target.onException(event,command,exception)
    }

    @Suppress("unused")
    fun resetTarget()
    {
        target = DEFAULT
    }
}

class StandardListener : CommandListener.Companion.BlankListener()
{
    companion object
    {
        val name = "standard"
    }

    override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
    {
        if(msg != null) event.reply(msg)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun onException(event: CommandEvent, command: Command, exception: Throwable)
    {
        NightFury.LOG.fatal("The CommandListener caught an Exception!")
        NightFury.LOG.fatal("Command: ${command.name}")
        if(exception.message != null)
            NightFury.LOG.fatal(exception.message)
    }
}

class IdleListener : CommandListener.Companion.BlankListener()
{
    companion object
    {
        val name = "idle"
    }

    @Suppress("UNUSED_PARAMETER")
    override fun checkCall(event: MessageReceivedEvent, client: Client, name: String, args: String): Boolean
    {
        return event.author.idLong == client.ownerID
    }

    override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
    {
        if(msg != null) event.reply(msg)
    }

    @Suppress("UNUSED_PARAMETER")
    override fun onException(event: CommandEvent, command: Command, exception: Throwable)
    {
        NightFury.LOG.fatal("The CommandListener caught an Exception!")
        if(exception.message != null)
            NightFury.LOG.fatal(exception.message)
    }
}

class DebugListener : CommandListener.Companion.BlankListener()
{
    companion object
    {
        private val DEBUG : SimpleLog = SimpleLog.getLog("Debug")
        val name = "debug"
    }

    override fun onCommandCall(event: CommandEvent, command: Command)
    {
        DEBUG.info("Call to Command \"${command.name}\"")
    }

    override fun onCommandTerminated(event: CommandEvent, command: Command, msg: String?)
    {
        if(msg != null) event.reply(msg)
        DEBUG.warn("Terminated Command \"${command.name}\" with message: \"$msg\"")
    }

    override fun onCommandCompleted(event: CommandEvent, command: Command)
    {
        DEBUG.info("Completed Command \"${command.name}\"")
    }

    @Suppress("UNUSED_PARAMETER")
    override fun onException(event: CommandEvent, command: Command, exception: Throwable)
    {
        DEBUG.fatal("Exception Caught for Command \"${command.name}\"")
        DEBUG.log(exception)
    }
}