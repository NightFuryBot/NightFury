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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.concurrent.KeyLockedContinuation
import xyz.nightfury.util.concurrent.suspendAndLockCoroutine

/**
 * @author Kaidan Gustave
 */
enum class ClientMode(val level: Level): ClientListener {
    SERVICE(Level.INFO),
    IDLE(Level.OFF) {
        override fun checkCall(event: MessageReceivedEvent, client: Client, name: String, args: String): Boolean {
            return event.author.idLong == NightFury.DEV_ID
        }
    },
    DEBUG(Level.DEBUG) {
        override fun onCommandCall(ctx: CommandContext, command: Command) {
            ClientListener.debug("Call to Command \"${command.name}\"")
        }

        override fun onCommandTerminated(ctx: CommandContext, command: Command, msg: String) {
            super.onCommandTerminated(ctx, command, msg)
            ClientListener.debug("Terminated Command \"${command.name}\" with message: \"$msg\"")
        }

        override fun onCommandCompleted(ctx: CommandContext, command: Command) {
            ClientListener.debug("Completed Command \"${command.name}\"")
        }
    },
    TEST(Level.DEBUG) {
        override fun onCommandCall(ctx: CommandContext, command: Command) {
            DEBUG.onCommandCall(ctx, command)
        }

        override fun onCommandTerminated(ctx: CommandContext, command: Command, msg: String) {
            DEBUG.onCommandTerminated(ctx, command, msg)
        }

        override fun onCommandCompleted(ctx: CommandContext, command: Command) {
            DEBUG.onCommandCompleted(ctx, command)
            lock?.tryUnlockAndResume(command, Unit)
        }

        override fun onException(ctx: CommandContext, command: Command, exception: Throwable) {
            DEBUG.onException(ctx, command, exception)
            lock?.tryUnlockAndResumeWithException(command, exception)
        }
    };

    companion object {
        @Volatile private var lock: KeyLockedContinuation<Command, Unit>? = null

        suspend fun loadContinuation(command: Command) {
            ClientListener.debug("Loaded test for ${command.fullname}!")
            suspendAndLockCoroutine<Command, Unit>(command) { cont ->
                lock = cont
            }
        }
    }
}
