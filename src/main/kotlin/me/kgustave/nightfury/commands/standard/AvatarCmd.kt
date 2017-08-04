package me.kgustave.nightfury.commands.standard

import club.minnced.kjda.builders.colorAwt
import club.minnced.kjda.builders.embed
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.extensions.findMembers
import me.kgustave.nightfury.extensions.findUsers
import me.kgustave.nightfury.utils.formattedName
import me.kgustave.nightfury.utils.multipleMembersFound
import me.kgustave.nightfury.utils.multipleUsersFound
import me.kgustave.nightfury.utils.noMatch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType

/**
 * @author Kaidan Gustave
 */
class AvatarCmd : Command()
{
    init {
        this.name = "Avatar"
        this.arguments = "<User>"
        this.help = "Gets a user's avatar."
        this.cooldown = 5
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
        this.botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun execute(event: CommandEvent)
    {
        val user = if(event.isFromType(ChannelType.TEXT)) {
            if(event.args.isNotEmpty()) {
                event.guild.findMembers(event.args).apply {
                    if(this.isEmpty())
                        return event.replyError(noMatch("members", event.args))
                    if(this.size > 1)
                        return event.replyError(multipleMembersFound(event.args, this))
                }[0].user
            } else { event.author }
        } else {
            if(event.args.isNotEmpty()) {
                event.jda.findUsers(event.args).apply {
                    if(this.isEmpty())
                        return event.replyError(noMatch("users", event.args))
                    if(this.size > 1)
                        return event.replyError(multipleUsersFound(event.args, this))
                }[0]
            } else { event.author }
        }

        event.reply(embed {
            title { "Avatar For ${user.formattedName(true)}" }
            if(event.isFromType(ChannelType.TEXT)) {
                val member = event.guild.getMember(user)
                if(member!=null) colorAwt = member.color
                else             colorAwt = event.selfMember.color
            }
            ("${user.effectiveAvatarUrl}?size=1024").apply { url { this } image { this } }
        })
    }
}