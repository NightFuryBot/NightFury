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

import xyz.nightfury.entities.menus.OrderedMenu
import xyz.nightfury.entities.menus.Paginator
import xyz.nightfury.entities.menus.EventWaiter
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.AutoInvokeCooldown
import xyz.nightfury.commands.admin.ModeratorListBaseCmd
import xyz.nightfury.entities.embed
import xyz.nightfury.listeners.InvisibleTracker
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.*
import xyz.nightfury.entities.promise
import xyz.nightfury.extensions.*
import java.time.format.DateTimeFormatter
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
class ServerCmd(waiter: EventWaiter, invisTracker: InvisibleTracker) : Command() {
    init {
        this.name = "Server"
        this.aliases = arrayOf("guild")
        this.arguments = "<Info Category>"
        this.help = "Gets info on the server."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE)
        this.children = arrayOf(
                ServerJoinsCmd(waiter),
                ModeratorListBaseCmd.ServerModeratorsCmd(),
                ServerOwnerCmd(invisTracker),
                ServerSettingsCmd(),
                ServerStatsCmd()
        )
    }

    val builder: OrderedMenu.Builder = OrderedMenu.Builder()
            .useCancelButton { true }
            .description { "Choose a field to get info on:" }
            .timeout { delay { 20 } }
            .allowTextInput { false }
            .finalAction { it.delete().promise() }
            .waiter  { waiter }

    override fun execute(event: CommandEvent) {
        if(event.args.isNotEmpty())
            return event.replyError("**Invalid Information Category**\n" +
                    SEE_HELP.format(event.client.prefix, name))
        val children = children

        with(builder) {
            clearChoices()
            for(child in children) {
                if(event.level.test(event)) {
                    choice(child.name) {
                        it.delete().promise() then { child.run(event) }
                    }
                }
            }
            user      { event.author }
            color     { event.selfMember.color }
            displayIn { event.channel }
        }
        event.invokeCooldown()
    }
}

@AutoInvokeCooldown
private class ServerOwnerCmd(private val invisTracker: InvisibleTracker) : Command() {
    companion object {
        private const val BULLET : String = "\uD83D\uDD39 "
        private const val STREAMING_EMOTE_ID = 313956277132853248L
        private const val ASTERISK_ESC = "\u002A"
        private const val UNDERSCORE_ESC = "\u005F"
        private const val TILDE_ESC = "\u007E"
        private const val L_PARENTHESIS_ESC = "\u0028"
        private const val R_PARENTHESIS_ESC = "\u0029"
        private const val L_BRACKET_ESC = "\u005B"
        private const val R_BRACKET_ESC = "\u005D"
        private fun cleanEscapes(string: String) : String = string
                .replace("*", ASTERISK_ESC)
                .replace("_", UNDERSCORE_ESC)
                .replace("~", TILDE_ESC)
                .replace("(", L_PARENTHESIS_ESC)
                .replace(")", R_PARENTHESIS_ESC)
                .replace("[", L_BRACKET_ESC)
                .replace("]", R_BRACKET_ESC)
    }

    init {
        this.name = "Owner"
        this.fullname = "Server Owner"
        this.help = "Gets info on the owner of this server."
        this.guildOnly = true
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val member : Member = event.guild.owner
        val user : User = member.user

        event.reply(embed {
            title = "${if(user.isBot) event.jda.getEmoteById(230105988211015680L)!!.asMention else "\u2139"} " + "__Information on ${user.formattedName(false)}:__"
            thumbnail = if(user.avatarUrl == null) user.defaultAvatarUrl else user.avatarUrl
            append(BULLET).append("**ID:** ${user.id}")
            appendln()
            color { member.color }
            if(member.nickname != null) {
                append(BULLET).append("**Nickname:** ${member.nickname}")
                appendln()
            }
            val roles = member.roles
            if(roles.isNotEmpty()) {
                append(BULLET).append("**Role${if(roles.size > 1) "s" else ""}:** ")
                append("`${roles[0].name}`")
                for(i in 1 until roles.size) append(", `${roles[i].name}`")
                appendln()
            }
            append(BULLET).append("**Status:** ")
            val game = member.game
            if(game != null) {
                if(game.url != null) {
                    append(event.jda.getEmoteById(STREAMING_EMOTE_ID)!!.asMention)
                    append(" Streaming **[${cleanEscapes(game.name)}](${game.url})**")
                } else {
                    append(event.jda.getEmoteById(member.onlineStatus.emoteId)!!.asMention)
                    append(" Playing **${cleanEscapes(game.name)}**")
                }
            } else if(member.onlineStatus == OnlineStatus.OFFLINE && invisTracker.isInvisible(member.user)) {
                val lastTimeTyping = invisTracker.getLastTimeTyping(user)
                if(lastTimeTyping != null) {
                    append(event.jda.getEmoteById(OnlineStatus.INVISIBLE.emoteId)!!.asMention)
                    append(" *${OnlineStatus.INVISIBLE.name}* (Last seen $lastTimeTyping minutes ago)")
                } else {
                    append(event.jda.getEmoteById(member.onlineStatus.emoteId)!!.asMention)
                    append(" *${member.onlineStatus.name}*")
                }
            } else {
                append(event.jda.getEmoteById(member.onlineStatus.emoteId)!!.asMention)
                append(" *${member.onlineStatus.name}*")
            }
            appendln()
            append(BULLET).append("**Creation Date:** ").append(user.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
            appendln()
            append(BULLET).append("**Join Date:** ").append(member.joinDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            val joins = ArrayList(event.guild.members)
            joins.sortWith(Comparator.comparing(Member::getJoinDate))
            var index = joins.indexOf(member)
            append(" `[#").append(index + 1).append("]`")
            appendln()
            append(BULLET).append("**Join Order:** ")
            appendln()
            index -= 3
            if(index < 0) index = 0
            if(joins[index] == member) append("**[${user.name}]()**")
            else append(joins[index].user.name)
            for(i in index + 1 until index + 7) {
                if(i >= joins.size) break
                val m = joins[i]
                var name: String = m.user.name
                if(member == m) name = "**[$name]()**"
                append(" > $name")
            }
        })
    }
}

@AutoInvokeCooldown
private class ServerJoinsCmd(waiter: EventWaiter) : Command() {
    init {
        this.name = "Joins"
        this.fullname = "Server Joins"
        this.help = "Gets an ordered list of this server's join history."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.CHANNEL
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    val builder : Paginator.Builder = Paginator.Builder()
            .timeout          { delay { 20 } }
            .showPageNumbers  { true }
            .numberItems      { true }
            .waitOnSinglePage { true }
            .waiter           { waiter }

    override fun execute(event: CommandEvent) {
        val joins = ArrayList(event.guild.members)
        joins.sortWith(Comparator.comparing(Member::getJoinDate))
        val names = joins.map { it.user.formattedName(true) }
        with(builder) {
            text        { _,_ -> "Joins for ${event.guild.name}" }
            items       { addAll(names) }
            finalAction { it.delete().queue() }
            user        { event.author }
            displayIn   { event.channel }
        }
    }
}

private class ServerSettingsCmd : Command() {
    init {
        this.name = "Settings"
        this.fullname = "Server Settings"
        this.aliases = arrayOf("config", "configurations")
        this.help = "Get info on the server's settings."
        this.guildOnly = true
        this.category = Category.MODERATOR
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val guild = event.guild
        event.reply(embed {
            author {
                value = "Settings for ${guild.name} (ID: ${guild.id})"
                image = guild.iconUrl
            }
            color { event.selfMember.color }
            field {
                this.name = "Prefixes"
                this.value = buildString {
                    this.append("`${event.client.prefix}`")
                    SQLPrefixes.getPrefixes(guild).forEach { this.append(", `$it`") }
                }
                this.inline = true
            }
            field {
                val modRole = SQLModeratorRole.getRole(guild)
                this.name = "Moderator Role"
                this.value = modRole?.name ?: "None"
                this.inline = true
            }
            field {
                val modLog = SQLModeratorLog.getChannel(guild)
                this.name = "Moderation Log"
                this.value = modLog?.name ?: "None"
                this.inline = true
            }
            field {
                val mutedRole = SQLMutedRole.getRole(guild)
                this.name = "Muted Role"
                this.value = mutedRole?.name ?: "None"
                this.inline = true
            }
            field {
                this.name = "Cases"
                this.value = "${SQLCases.getCases(event.guild).size} cases"
                this.inline = true
            }
        })
    }
}

@AutoInvokeCooldown
private class ServerStatsCmd : Command() {
    init {
        this.name = "Stats"
        this.fullname = "Server Stats"
        this.help = "Gets server statistics and information."
        this.guildOnly = true
        this.cooldown = 10
        this.cooldownScope = CooldownScope.CHANNEL
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        event.reply(embed {
            title { "Stats for ${event.guild.name}" }
            event.guild.iconUrl?.let {
                url { it }
                thumbnail { it }
            }
            color { event.member.color }

            field {
                name = "Members"
                appendln("Total: ${event.guild.members.size}")
                if(SQLModeratorRole.hasRole(event.guild)) {
                    val modRole = SQLModeratorRole.getRole(event.guild)
                    appendln("Moderators: ${event.guild.members.filter { it.roles.contains(modRole) }.size}")
                }
                appendln("Administrators: ${event.guild.members.filter { it.isAdmin }.size}")
                appendln("Bots: ${event.guild.members.filter { it.user.isBot }.size}")
                this.inline = true
            }

            field {
                name = "Text Channels"
                appendln("Total: ${event.guild.textChannels.size}")
                appendln("Visible: ${event.guild.textChannels.filter { event.member canView it }.size}")
                appendln("Hidden: ${event.guild.textChannels.size - event.guild.textChannels.filter { event.member canView it }.size}")
                this.inline = true
            }

            field {
                name = "Voice Channels"
                appendln("Total: ${event.guild.voiceChannels.size}")
                appendln("Unlocked: ${event.guild.voiceChannels.filter { event.member canJoin it }.size}")
                appendln("Locked: ${event.guild.voiceChannels.size - event.guild.voiceChannels.filter { event.member canJoin it }.size}")
                this.inline = true
            }

            footer {
                value = "Created ${event.guild.creationTime.readableFormat}"
            }
        })
    }

}
