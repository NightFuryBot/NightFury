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

import net.dv8tion.jda.core.OnlineStatus.DO_NOT_DISTURB
import xyz.nightfury.NightFury
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.watching

/**
 * @author Kaidan Gustave
 */
class ShutdownCommand: Command(OwnerGroup) {
    override val name = "Shutdown"
    override val help = "Shuts down NightFury."

    override suspend fun execute(ctx: CommandContext) {
        NightFury.LOG.info("Shutting down...")
        ctx.jda.presence.setPresence(DO_NOT_DISTURB, watching("Everything shut down..."))

        // Await to prevent shutting down while replying
        ctx.sendWarning("Shutting down...").await()
        ctx.jda.shutdown()
    }

}
