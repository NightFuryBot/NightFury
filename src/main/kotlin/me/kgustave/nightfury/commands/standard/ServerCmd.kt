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

import club.minnced.kjda.builders.colorAwt
import club.minnced.kjda.builders.embed
import com.jagrosh.jdautilities.menu.orderedmenu.OrderedMenuBuilder
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.kjdautils.menu.*
import me.kgustave.nightfury.Category
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.commands.admin.ModeratorListBaseCmd
import me.kgustave.nightfury.extensions.*
import me.kgustave.nightfury.listeners.InvisibleTracker
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import java.time.format.DateTimeFormatter
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
class ServerCmd(waiter: EventWaiter, invisTracker: InvisibleTracker) : Command()
{
    init {
        this.name = "Server"
        this.aliases = arrayOf("guild")
        this.arguments = "<Info Category>"
        this.help = "Gets info on the server."
        this.helpBiConsumer = Command standardSubHelp
                        "What the command outputs is based on one of the sub-commands listed " +
                        "below. If no sub-command is specified, this will generate a menu where you can select which " +
                        "category to get info from."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
        this.children = arrayOf(
                ServerJoinsCmd(waiter),
                ModeratorListBaseCmd.ServerModeratorsCmd(),
                ServerOwnerCmd(invisTracker),
                ServerSettingsCmd(),
                ServerSettingsCmd()
        )
    }

    val builder : OrderedMenuBuilder = OrderedMenuBuilder()
            .useCancelButton { true }
            .description { "Choose a field to get info on:" }
            .timeout { delay { 20 } }
            .waiter  { waiter }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isNotEmpty())
            return event.replyError("**Invalid Information Category**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        val children = children

        with(builder)
        {
            choices {
                choice    { name { "Joins" }      action { children[0].run(event) } }
                choice    { name { "Moderators" } action { children[1].run(event) } }
                choice    { name { "Owner" }      action { children[2].run(event) } }
                if(Category.MODERATOR.test(event))
                choice    { name { "Settings" }   action { children[3].run(event) } }


            }
            user      { event.author }
            color     { event.selfMember.color }
            displayIn { event.channel }
        }
        event.invokeCooldown()
    }
}

@AutoInvokeCooldown
private class ServerOwnerCmd(private val invisTracker: InvisibleTracker) : Command()
{
    companion object
    {
        private val BULLET : String = "\uD83D\uDD39 "
        private val STREAMING_EMOTE_ID = 313956277132853248L
        private val ASTERISK_ESC = "\u002A"
        private val UNDERSCORE_ESC = "\u005F"
        private val TILDE_ESC = "\u007E"
        private val L_PARENTHESIS_ESC = "\u0028"
        private val R_PARENTHESIS_ESC = "\u0029"
        private val L_BRACKET_ESC = "\u005B"
        private val R_BRACKET_ESC = "\u005D"
        private fun cleanEscapes(string: String) : String = string
                .replace("*", ASTERISK_ESC)
                .replace("_", UNDERSCORE_ESC)
                .replace("~", TILDE_ESC)
                .replace("(", L_PARENTHESIS_ESC)
                .replace(")", R_PARENTHESIS_ESC)
                .replace("[", L_BRACKET_ESC)
                .replace("]", R_BRACKET_ESC)
    }

    init
    {
        this.name = "Owner"
        this.fullname = "Server Owner"
        this.help = "Gets info on the owner of this server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val member : Member = event.guild.owner
        val user : User = member.user

        event.reply(embed {
            title = "${if(user.isBot) event.jda.getEmoteById(230105988211015680L).asMention else "\u2139"} " +
                    "__Information on ${user.formattedName(false)}:__"
            thumbnail = if(user.avatarUrl == null) user.defaultAvatarUrl else user.avatarUrl
            append(BULLET).append("**ID:** ${user.id}")
            appendln()
            colorAwt = member.color
            if(member.nickname != null)
            {
                append(BULLET).append("**Nickname:** ${member.nickname}")
                appendln()
            }
            val roles = member.roles
            if(roles.isNotEmpty())
            {
                append(BULLET).append("**Role${if (roles.size > 1) "s" else ""}:** ")
                append("`${roles[0].name}`")
                for(i in 1 until roles.size)
                    append(", `${roles[i].name}`")
                appendln()
            }
            append(BULLET).append("**Status:** ")
            if(member.game!=null)
            {
                if(member.game.url!=null)
                {
                    append(event.jda.getEmoteById(STREAMING_EMOTE_ID).asMention)
                    append(" Streaming **[${cleanEscapes(member.game.name)}](${member.game.url})**")
                }
                else
                {
                    append(event.jda.getEmoteById(member.onlineStatus.emoteId).asMention)
                    append(" Playing **${cleanEscapes(member.game.name)}**")
                }
            }
            else if(member.onlineStatus == OnlineStatus.OFFLINE && invisTracker.isInvisible(member.user))
            {
                val lastTimeTyping = invisTracker.getLastTimeTyping(user)
                if(lastTimeTyping!=null)
                {
                    append(event.jda.getEmoteById(getEmoteIdFor(OnlineStatus.INVISIBLE)).asMention)
                    append(" *${OnlineStatus.INVISIBLE.name}* (Last seen $lastTimeTyping minutes ago)")
                }
                else
                {
                    append(event.jda.getEmoteById(member.onlineStatus.emoteId).asMention)
                    append(" *${member.onlineStatus.name}*")
                }
            }
            else
            {
                append(event.jda.getEmoteById(member.onlineStatus.emoteId).asMention)
                append(" *${member.onlineStatus.name}*")
            }
            appendln()
            append(BULLET).append("**Creation Date:** ").append(user.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
            appendln()
            append(BULLET).append("**Join Date:** ").append(member.joinDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            val joins = ArrayList(event.guild.members)
            joins.sortWith(Comparator.comparing(Member::getJoinDate))
            var index = joins.indexOf(member)
            append(" `[#").append(index+1).append("]`")
            appendln()
            append(BULLET).append("**Join Order:** ")
            appendln()
            index -= 3
            if(index < 0)
                index = 0
            if(joins[index] == member)
                append("**[${user.name}]()**")
            else
                append(joins[index].user.name)
            for(i in index + 1 until index + 7) {
                if(i>=joins.size)
                    break
                val m = joins[i]
                var name : String = m.user.name
                if(member == m)
                    name = "**[$name]()**"
                append(" > $name")
            }
        })
    }
}

@AutoInvokeCooldown
private class ServerJoinsCmd(waiter: EventWaiter) : Command()
{
    init {
        this.name = "Joins"
        this.fullname = "Server Joins"
        this.help = "Gets an ordered list of this server's join history."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.CHANNEL
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    val builder : PaginatorBuilder = PaginatorBuilder()
            .timeout          { delay { 20 } }
            .showPageNumbers  { true }
            .useNumberedItems { true }
            .waitOnSinglePage { true }
            .waiter           { waiter }

    override fun execute(event: CommandEvent)
    {
        val joins = ArrayList(event.guild.members)
        joins.sortWith(Comparator.comparing(Member::getJoinDate))
        val names = joins.map { it.user.formattedName(true) }
        with(builder)
        {
            text { -> "Joins for ${event.guild.name}" }
            items            { addAll(names) }
            finalAction      { it.delete().queue() }
            users            { arrayOf(event.author) }
            displayIn { event.channel }
        }
    }
}

private class ServerSettingsCmd : Command()
{
    init {
        this.name = "Settings"
        this.fullname = "Server Settings"
        this.aliases = arrayOf("config", "configurations")
        this.help = "Get info on the server's settings."
        this.guildOnly = true
        this.category = Category.MODERATOR
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val guild = event.guild
        event.reply(embed {
            author {
                value = "Settings for ${guild.name} (ID: ${guild.id})"
                image = guild.iconUrl
            }
            colorAwt = event.selfMember.color
            field {
                this.name = "Prefixes"
                this.value = buildString {
                    this.append("`${event.client.prefix}`")
                    event.client.manager.getPrefixes(guild).forEach { this.append(", `$it`") }
                }
                this.inline = true
            }
            field {
                val modRole = event.client.manager.getModRole(guild)
                this.name = "Moderator Role"
                this.value = if(modRole!=null) modRole.name else "None"
                this.inline = true
            }
            field {
                val modLog = event.client.manager.getModLog(guild)
                this.name = "Moderator Log"
                this.value = if(modLog!=null) modLog.asMention else "None"
                this.inline = true
            }
            field {
                val mutedRole = event.client.manager.getMutedRole(guild)
                this.name = "Muted Role"
                this.value = if(mutedRole!=null) mutedRole.name else "None"
                this.inline = true
            }
            field {
                this.name = "Cases"
                this.value = "${event.client.manager.getCases(event.guild).size} cases"
                this.inline = true
            }
        })
    }
}

@AutoInvokeCooldown
private class ServerStatsCmd : Command()
{
    init {
        this.name = "Stats"
        this.fullname = "Server Stats"
        this.help = "Gets server statistics and information."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.CHANNEL
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        event.reply(embed {
            title { "Stats for ${event.guild.name}" }
            url   { event.guild.iconUrl }
            thumbnail { event.guild.iconUrl }

            field {
                name = "Members"
                appendln("Owner: ${event.guild.owner.user.formattedName(false)}")
                appendln("Total: ${event.guild.members.size}")
                if(event.manager.hasModRole(event.guild)) {
                    val modRole = event.manager.getModRole(event.guild)
                    appendln("Moderators: ${event.guild.members.filter { it.roles.contains(modRole) }.size}")
                }
                appendln("Administrators: ${event.guild.members.filter { it.isAdmin }.size}")
            }

            field {
                name = "Text Channels"
                appendln("Total: ${event.guild.textChannels.size}")
                appendln("Visible: ${event.guild.textChannels.filter { event.member canView it }.size}")
                appendln("Hidden: ${event.guild.textChannels.filter { it.guild.publicRole canView it }.size}")
            }

            field {
                name = "Voice Channels"
                appendln("Total: ${event.guild.voiceChannels.size}")
                appendln("Unlocked: ${event.guild.voiceChannels.filter { event.member canJoin it }.size}")
                appendln("Default: ${event.guild.voiceChannels.filter { it.guild.publicRole canJoin it }.size}")
            }

            footer {
                value = "Created ${event.guild.creationTime.readableFormat}"
            }

            time { event.guild.creationTime }
        })
    }

}