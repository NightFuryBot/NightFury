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
package me.kgustave.nightfury.commands.admin

import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.NoBaseExecutionCommand

/**
 * @author Kaidan Gustave
 */
class EnableCmd : NoBaseExecutionCommand()
{
    init
    {
        name = "Toggle"
        arguments = "[Function]"
        help = "Toggles sensitive server functions. [BETA]"
        category = Category.SERVER_OWNER
        guildOnly = true
        children = arrayOf(EnableRolePersistCmd())
    }
}

private class EnableRolePersistCmd : Command()
{
    init
    {
        name = "RolePersist"
        fullname = "Toggle RolePersist"
        help = "Toggles RolePersist for the server."
        guildOnly = true
        category = Category.SERVER_OWNER
    }

    override fun execute(event: CommandEvent)
    {
        if(event.manager.isRolePersist(event.guild))
        {
            event.manager.setIsRolePersist(event.guild, false)
            event.manager.removeAllRolePersist(event.guild)
            event.replySuccess("RolePersist has been toggled `OFF`!")
        }
        else
        {
            event.manager.setIsRolePersist(event.guild, true)
            event.replySuccess("RolePersist has been toggled `ON`!")
        }
    }
}