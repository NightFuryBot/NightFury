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
package xyz.nightfury.command.moderator

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.listeners.ModLog
import xyz.nightfury.util.ext.*

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class UnbanCommand : Command(ModeratorGroup) {
    override val name = "Unban"
    override val arguments = "[User]"
    override val help = "Unbans a user from the server."
    override val botPermissions = arrayOf(Permission.BAN_MEMBERS)

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        val bannedUsers = ctx.guild.findBannedUsers(query) ?: return ctx.replyError {
            "An unexpected error occurred while searching for banned users!"
        }
        val target = when {
            bannedUsers.isEmpty() -> return ctx.replyError(noMatch("banned users", query))
            bannedUsers.size > 1 -> return ctx.replyError(bannedUsers.multipleUsers(query))
            else -> bannedUsers[0]
        }

        target.unbanFrom(ctx.guild).await()
        ctx.replySuccess("Successfully unbanned ${target.formattedName(true)}!")

        launch(ctx) {
            ModLog.newUnban(ctx.member, target)
        }
    }
}
