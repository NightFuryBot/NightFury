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
package me.kgustave.nightfury.commands

import club.minnced.kjda.promise
import me.kgustave.nightfury.Argument
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import net.dv8tion.jda.core.Permission
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class CleanCmd : Command() {

    init {
        this.name = "clean"
        this.aliases = arrayOf("clear", "prune")
        this.arguments = Argument("<number of messages>", Regex("\\d+").toPattern())
        this.help = "deletes a specified number of messages from the channel this is called in"
        this.category = Category.MODERATOR
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)
    }

    override fun execute(event: CommandEvent)
    {
        val number = event.args.toInt()
        // TODO Error
        if(number>100 || number<2)
            return event.replyError("")
        val history = event.textChannel.history
        history.retrievePast(1).complete()
        history.retrievePast(number).promise() then {
            // TODO Error
            if(it == null) return@then event.replyError("")
            event.textChannel.deleteMessages(it.stream().skip(1).toList()).promise() then {
                event.replySuccess("Successfully deleted $number messages!")
            } catch { event.replyError("An error occurred while deleting the messages retrieved!") }
        } catch {
            event.replyError("An error occurred while retrieving the messages to delete!")
        }
    }
}