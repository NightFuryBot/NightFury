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
package xyz.nightfury.commands.standard

import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.entities.promise
import xyz.nightfury.extensions.findRoles
import xyz.nightfury.extensions.multipleRoles
import xyz.nightfury.extensions.noMatch
import net.dv8tion.jda.core.Permission
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.SQLColorMe
import java.awt.Color
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@MustHaveArguments("Specify a color or hex code!")
class ColorMeCmd : Command() {
    companion object {
        private val pattern = Regex("#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]").toPattern()
    }

    init {
        this.name = "ColorMe"
        this.arguments = "[Color or Hex Code]"
        this.help = "Set the color of your highest ColorMe role."
        this.cooldown = 20
        this.guildOnly = true
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.children = arrayOf(
                ColorMeAddCmd(),
                ColorMeRemoveCmd()
        )
    }

    override fun execute(event: CommandEvent) {
        val allColormes = SQLColorMe.getRoles(event.guild)
        if(allColormes.isEmpty())
            return event.replyError("**No ColorMe roles on this server!**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        val colormes = event.member.roles.stream().filter { allColormes.contains(it) }.toList()
        if(colormes.isEmpty())
            return event.replyError("**You do not have any ColorMe roles!**\n" +
                    SEE_HELP.format(event.client.prefix, name))

        val color : Color = if(pattern.matcher(event.args).matches()) {
            try {
                Color.decode(event.args)
            } catch(e: NumberFormatException) {
                return event.replyError("${event.args} is not a valid hex!")
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
            return event.replyError("**Cannot interact with your highest ColorMe role!**\n" +
                    "Try moving my highest role above your highest ColorMe role!")
        requested.manager.setColor(color).promise() then {
            event.replySuccess("Successfully changed your color to ${event.args}")
            event.invokeCooldown()
        } catch {
            event.replyError("An unexpected error occurred while changing your color!")
        }
    }
}

@MustHaveArguments("Specify the name of a role to add!")
private class ColorMeAddCmd : Command()
{
    init {
        this.name = "Add"
        this.fullname = "ColorMe Add"
        this.arguments = "[Role]"
        this.help = "Adds a ColorMe role for the server."
        this.cooldown = 30
        this.cooldownScope = CooldownScope.GUILD
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val found = event.guild.findRoles(query)
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(found.multipleRoles(query))
        val requested = found[0]
        if(SQLColorMe.isRole(requested))
            return event.replyError("The role **${requested.name}** is already a ColorMe role!")
        SQLColorMe.addRole(requested)
        if(event.selfMember.canInteract(requested))
            event.replySuccess("The role **${requested.name}** was added as ColorMe!")
        else
            event.replyWarning("The role **${requested.name}** was added as ColorMe!\n" +
                    "Please be aware that due to role hierarchy positioning, I will not be able to give this role to members!\n" +
                    "To fix this, make sure my I have a role higher than `${requested.name}` on the roles list.")
        event.invokeCooldown()
    }

}

@MustHaveArguments("Specify the name of a role to remove!")
private class ColorMeRemoveCmd : Command()
{
    init {
        this.name = "Remove"
        this.fullname = "ColorMe Remove"
        this.arguments = "[Role]"
        this.help = "Removes a ColorMe role for the server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MANAGE_ROLES)
        this.category = Category.ADMIN
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val found = event.guild.findRoles(query).stream().filter { SQLColorMe.isRole(it) }.toList()
        if(found.isEmpty())
            return event.replyError(noMatch("roles", query))
        if(found.size>1)
            return event.replyError(found.multipleRoles(query))
        SQLColorMe.deleteRole(found[0])
        event.replySuccess("The role **${found[0].name}** was removed from ColorMe!")
    }
}