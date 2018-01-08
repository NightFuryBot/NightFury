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
package xyz.nightfury.commands.admin

import xyz.nightfury.*
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLLevel
import xyz.nightfury.resources.Arguments

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class LevelCmd : NoBaseExecutionCommand()
{
    init {
        this.name = "Level"
        this.arguments = "[Function]"
        this.help = "Manage individual command's restrictions."
        this.guildOnly = true
        this.category = Category.SERVER_OWNER
        this.children = arrayOf(
                LevelSetCmd()
        )
    }
}

@MustHaveArguments("Try specifying a level and then a command by full name.")
private class LevelSetCmd : Command()
{
    init {
        this.name = "Set"
        this.fullname = "Level Set"
        this.arguments = "[Command]"
        this.help = "Sets the command's level for the server."
        this.guildOnly = true
        this.category = Category.SERVER_OWNER
    }

    override fun execute(event: CommandEvent)
    {
        val split = event.args.split(Arguments.commandArgs, 2)

        if(split.size < 2)
            return event.replyError(TOO_FEW_ARGS_ERROR.format("Try specifying a level and then a command by full name."))

        val level = CommandLevel.fromArguments(split[0]) ?:
                    return event.replyError("**Invalid command level!**\n" +
                                        "`${split[0]}` is not a valid command level! Valid levels are `owner`, `admin`, `moderator`, and `public`!")

        val command = event.client.searchCommand(split[1]) ?:
                return event.replyError("**Invalid command name!**\n" +
                                        "`${split[1]}` is not a command!")

        if(command.defaultLevel == CommandLevel.SHENGAERO)
            return

        if(SQLLevel.getLevel(event.guild, command) == level)
            return event.replyWarning("The command `${command.fullname}` is already set at $level!")

        SQLLevel.setLevel(event.guild, command, level)
        event.replySuccess("Successfully set level of `${command.fullname}` to $level!")

    }
}