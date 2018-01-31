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

import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.AutoInvokeCooldown
import xyz.nightfury.entities.embed
import xyz.nightfury.extensions.*
import xyz.nightfury.listeners.InvisibleTracker
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.annotations.HasDocumentation
import java.time.format.DateTimeFormatter
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@AutoInvokeCooldown
class InfoCmd(private val invisTracker: InvisibleTracker) : Command() {
    companion object {
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

    init {
        this.name = "Info"
        this.aliases = arrayOf("i", "information")
        this.arguments = "<User>"
        this.help = "Gets info on a user."
        this.cooldown = 5
        this.cooldownScope = CooldownScope.USER_GUILD
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent) {
        val query = event.args
        val temp : Member? = if(event.isFromType(ChannelType.TEXT)) {
            if(query.isEmpty())
                event.member
            else {
                val found = event.guild.findMembers(query)
                when {
                    found.isEmpty() -> null
                    found.size>1 -> return event.replyError(found.multipleMembers(query))
                    else -> found[0]
                }
            }
        } else null

        val user : User = when {
            temp!=null -> temp.user
            query.isEmpty() -> event.author
            else -> {
                val found =  event.jda.findUsers(query)
                when {
                    found.isEmpty() -> return event.replyError(noMatch("users", query))
                    found.size>1    -> return event.replyError(found.multipleUsers(query))
                    else            -> found[0]
                }
            }
        }

        val member : Member? = if(temp == null && event.isFromType(ChannelType.TEXT)) event.guild.getMember(user) else temp

        event.reply(embed {
            title = "${if(user.isBot) event.jda.getEmoteById(230105988211015680L)!!.asMention else "\u2139"} " + "__Information on ${user.formattedName(false)}:__"
            thumbnail = if(user.avatarUrl == null) user.defaultAvatarUrl else user.avatarUrl
            append(BULLET).append("**ID:** ${user.id}")
            appendln()
            if(member != null) {
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
                    val lastTimeTyping = invisTracker.getLastTimeTyping(member.user)
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
            }
            append(BULLET).append("**Creation Date:** ").append(user.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
            appendln()
            if(member != null) {
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
            }
        })
    }
}
