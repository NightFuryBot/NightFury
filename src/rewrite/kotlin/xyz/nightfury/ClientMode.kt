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
package xyz.nightfury

import ch.qos.logback.classic.Level
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.createLogger

/**
 * @author Kaidan Gustave
 */
enum class ClientMode(val level: Level): ClientListener {
    SERVICE(Level.INFO),
    IDLE(Level.OFF),
    DEBUG(Level.DEBUG) {
        private val log = createLogger("Debugger")

        override fun onCommandCall(ctx: CommandContext, command: Command) {
            log.debug("Call to Command \"${command.name}\"")
        }

        override fun onCommandTerminated(ctx: CommandContext, command: Command, msg: String) {
            ctx.reply(msg)
            log.debug("Terminated Command \"${command.name}\" with message: \"$msg\"")
        }

        override fun onCommandCompleted(ctx: CommandContext, command: Command) {
            log.debug("Completed Command \"${command.name}\"")
        }

        override fun onException(ctx: CommandContext, command: Command, exception: Throwable) {
            log.debug("Exception Caught for Command \"${command.name}\"",exception)
        }
    };
}
