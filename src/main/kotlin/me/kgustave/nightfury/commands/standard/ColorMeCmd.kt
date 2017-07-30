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
package me.kgustave.nightfury.commands.standard

import club.minnced.kjda.promise
import com.jagrosh.jdautilities.utils.FinderUtil
import me.kgustave.nightfury.*
import me.kgustave.nightfury.utils.multipleRolesFound
import me.kgustave.nightfury.utils.noMatch
import net.dv8tion.jda.core.Permission
import java.awt.Color
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class ColorMeCmd : Command() {

    companion object {
        private val pattern = Regex("#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]").toPattern()
    }

    init {
        this.name = "colorme"
        this.arguments = Argument("<hexcode>")
        this.help = "set the color of your highest ColorMe role"
        this.cooldown = 10
        this.guildOnly = true
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.children = arrayOf(ColorMeAddCmd(), ColorMeRemoveCmd()
        )
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, name))
        val allColormes = event.manager.getColorMes(event.guild)
        if(allColormes.isEmpty())
            return event.replyError("**No ColorMe roles on this server!**\n${SEE_HELP.format(event.prefixUsed, name)}")
        val colormes = event.member.roles.stream().filter { allColormes.contains(it) }.toList()
        if(colormes.isEmpty())
            return event.replyError("**You do not have any ColorMe roles!**\n${SEE_HELP.format(event.prefixUsed, name)}")

        val color : Color = if(pattern.matcher(event.args).matches()) {
            try {
                Color.decode(event.args)
            } catch(e: NumberFormatException) {
                return event.replyError("${event.args} is not a valid hexcode!")
            }
        } else when(event.args.toLowerCase()) {

            // Regular Colors
            "red"                      -> Color.RED
            "orange"                   -> Color.ORANGE
            "yellow"                   -> Color.YELLOW
            "green"                    -> Color.GREEN
            "cyan"                     -> Color.CYAN
            "blue"                     -> Color.BLUE
            "magenta"                  -> Color.MAGENTA
            "pink"                     -> Color.PINK
            "black"                    -> Color.decode("#000001")
            "purple"                   -> Color.decode("#800080")
            "dark gray", "dark grey"   -> Color.DARK_GRAY
            "gray", "grey"             -> Color.GRAY
            "light_gray", "light_grey" -> Color.LIGHT_GRAY
            "white"                    -> Color.WHITE

            // Discord Colors
            "blurple"                  -> Color.decode("#7289DA")
            "greyple"                  -> Color.decode("#99AAB5")
            "darktheme"                -> Color.decode("#2C2F33")

            else                       -> return event.replyError("${event.args} is not a valid color!")
        }
        val requested = colormes[0]

        if(!event.selfMember.canInteract(requested))
            event.replyError("**Cannot interact with your highest ColorMe role!**\n" +
                    "Try moving my highest role above your highest ColorMe role!")
        requested.manager.setColor(color).promise() then {
            event.replySuccess("Successfully changed your color to ${event.args}")
        } catch {
            event.replyError("An unexpected error occurred while changing your color!")
        }
        invokeCooldown(event)
    }
}
private class ColorMeAddCmd : Command()
{
    init {
        this.name = "add"
        this.fullname = "colorme add"
        this.arguments = Argument("<Role>")
        this.help = "adds a ColorMe role for the server"
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        if(query.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val found = FinderUtil.findRoles(query, event.guild)
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(multipleRolesFound(query, found))
        val requested = found[0]
        if(event.manager.isColorMe(requested))
            return event.replyError("The role **${requested.name}** is already a ColorMe role!")
        event.manager.addColorMe(requested)
        if(event.selfMember.canInteract(requested))
            event.replySuccess("The role **${requested.name}** was added as ColorMe!")
        else
            event.replyWarning("The role **${requested.name}** was added as ColorMe!\n" +
                    "Please be aware that due to role hierarchy positioning, I will not be able to give this role to members!\n" +
                    "To fix this, make sure my I have a role higher than `${requested.name}` on the roles list.")
        invokeCooldown(event)
    }

}

private class ColorMeRemoveCmd : Command()
{
    init {
        this.name = "remove"
        this.fullname = "colorme remove"
        this.arguments = Argument("<Role>")
        this.help = "removes a ColorMe role for the server"
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        if(query.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val found = FinderUtil.findRoles(query, event.guild).stream()
                .filter { event.manager.isColorMe(it) }.toList()
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(multipleRolesFound(query, found))
        event.manager.removeColorMe(found[0])
        event.replySuccess("The role **${found[0].name}** was removed from ColorMe!")
    }
}