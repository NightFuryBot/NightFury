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
package xyz.nightfury.command.standard

import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.util.jda.embed
import xyz.nightfury.util.*
import xyz.nightfury.util.jda.findMembers
import xyz.nightfury.util.jda.findUsers
import java.time.format.DateTimeFormatter
import java.util.Comparator
import kotlin.math.max

/**
 * @author Kaidan Gustave
 */
class InfoCommand: Command(StandardGroup) {
    companion object {
        private const val BULLET = "\uD83D\uDD39 "
        private const val STREAMING_EMOTE_ID = 313956277132853248L


        private val OnlineStatus.emoteId : Long
            get() = when(this) {
                OnlineStatus.ONLINE -> 313956277808005120L
                OnlineStatus.IDLE -> 313956277220802560L
                OnlineStatus.DO_NOT_DISTURB -> 313956276893646850L
                OnlineStatus.OFFLINE -> 313956277237710868L
                OnlineStatus.INVISIBLE -> 313956277107556352L
                OnlineStatus.UNKNOWN -> 313956277107556352L
            }

        // Kept here for usage in the 'server' command
        fun infoEmbed(ctx: CommandContext, user: User, member: Member?): MessageEmbed = embed {
            title = "${if(user.isBot) ctx.jda.getEmoteById(230105988211015680L)!!.asMention else "\u2139"} " + "__Information on ${user.formattedName(false)}:__"
            thumbnail = if(user.avatarUrl == null) user.defaultAvatarUrl else user.avatarUrl
            append(BULLET).appendln("**ID:** ${user.id}")
            member?.let {
                color { member.color }
                // Nickname
                member.nickname?.let { nickname ->
                    append(BULLET).append("**Nickname:** $nickname")
                    appendln()
                }
                // Roles
                val roles = member.roles
                if(roles.isNotEmpty()) {
                    append(BULLET).append("**Role${if(roles.size > 1) "s" else ""}:** ")
                    append("`${roles[0].name}`")
                    for(i in 1 until roles.size) {
                        append(", `${roles[i].name}`")
                    }
                    appendln()
                }
                // Status
                append(BULLET).append("**Status:** ")
                val game = member.game
                if(game !== null) {
                    val url = game.url
                    if(url !== null) {
                        append("${ctx.jda.getEmoteById(STREAMING_EMOTE_ID)!!.asMention} ")
                        append("Streaming **[${game.name}]($url)**")
                    } else {
                        append("${ctx.jda.getEmoteById(member.onlineStatus.emoteId)!!.asMention} ")
                        append("Playing **${game.name}**")
                    }
                } else {
                    append(ctx.jda.getEmoteById(member.onlineStatus.emoteId)!!.asMention)
                    append(" *${member.onlineStatus.name}*")
                }
                appendln()
            }
            // Creation Date
            append(BULLET).append("**Creation Date:** ")
            appendln(user.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE))

            member?.let {
                append(BULLET).append("**Join Date:** ")
                append(member.joinDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                val joins = ctx.guild.memberCache.sortedWith(Comparator.comparing(Member::getJoinDate))
                var index = joins.indexOf(member)
                append(" `[#").append(index + 1).appendln("]`")
                append(BULLET).appendln("**Join Order:** ")
                index = max(index - 3, 0)
                joins[index].let { m -> append(m.user.name.modifyIf(member == m) { "**[$it]()**" }) }
                @Suppress("LoopToCallChain") for(i in index + 1 until index + 7) {
                    if(i >= joins.size) break
                    val m = joins[i]
                    val name = m.user.name.modifyIf(member == m) { "**[$it]()**" }
                    append(" > $name")
                }
            }
        }
    }

    override val name = "Info"
    override val aliases = arrayOf("I", "UserInfo")
    override val arguments = "<User>"
    override val help = "Gets info on a user."
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        val temp = ctx.takeIf { ctx.isGuild }?.let {
            if(query.isEmpty())
                return@let ctx.member
            val members = ctx.guild.findMembers(query)
            return@let when {
                members.isEmpty() -> null
                members.size > 1 -> return ctx.replyError(members.multipleMembers(query))
                else -> members[0]
            }
        }

        val user = temp?.user ?: ctx.author.takeIf { query.isEmpty() } ?: ctx.jda.findUsers(query).let { users ->
            return@let when {
                users.isEmpty() -> return ctx.replyError(xyz.nightfury.util.noMatch("users", query))
                users.size > 1 -> return ctx.replyError(users.multipleUsers(query))
                else -> users[0]
            }
        }

        val member = temp ?: ctx.takeIf { ctx.isGuild }?.guild?.getMember(user)

        val embed = infoEmbed(ctx, user, member)

        ctx.reply(embed)
    }
}
