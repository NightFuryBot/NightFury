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
package me.kgustave.nightfury.commands.moderator

import club.minnced.kjda.promise
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.annotations.MustHaveArguments
import net.dv8tion.jda.core.Permission
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Provide a number between 2-100!")
class CleanCmd : Command() {

    init {
        this.name = "Clean"
        this.aliases = arrayOf("clear", "prune")
        this.arguments = "[Number of Messages]"
        this.help = "Deletes a specified number of messages from the channel this is called in. **[BETA]**"
        this.cooldown = 15
        this.cooldownScope = CooldownScope.CHANNEL
        this.category = Category.MODERATOR
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        val number : Int = if(event.args.isEmpty()) { 100 }
        else if(event.args.matches(Regex("\\d+"))) {
            val n = event.args.toInt()
            if((n<100 || n>3) && n>0) n
            else return event.replyError(INVALID_ARGS_ERROR.format("${event.args} is not a valid number to clear!"))
        }
        else return event.replyError(INVALID_ARGS_ERROR.format("Try specifying a number of messages to delete."))

        event.textChannel.getHistoryAround(event.messageIdLong,number).promise() then {
            if(it == null)
                return@then event.replyError("Failed to retrieve past $number messages!")
            val toDelete = it.retrievedHistory.stream().filter { it.idLong != event.messageIdLong }.toList()
            event.textChannel.deleteMessages(toDelete).promise() then {
                event.client.logger.newClean(event.member, event.textChannel, number)
                event.replySuccess("Successfully cleared past $number messages!")
            } catch {
                event.replyError("An error occurred when deleting $number messages!")
            }
        } catch { event.replyError("Failed to retrieve past $number messages!") }
        event.invokeCooldown()
    }
}