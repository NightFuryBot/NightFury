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
package me.kgustave.nightfury.entities

import me.kgustave.nightfury.db.DatabaseManager
import me.kgustave.nightfury.utils.formatUserName
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User

/**
 * @author Kaidan Gustave
 */
class Case
{
    companion object
    {
        val FORMAT : String = "`[  CASE  ]` `[%d]` %s %s %s **%s** (ID: %d)\n" + "`[ REASON ]` %s"
    }

    var number : Int = 0
    var guildId : Long = 0L
    var messageId : Long = 0L
    var modId : Long = 0L
    var targetId : Long = 0L
    var isOnUser : Boolean = true
    var action : LogAction = LogAction.OTHER
    var reason : String = "No Reason Specified"

    fun toDBArgs() : Array<Any> = arrayOf(number,guildId,messageId,modId,targetId,isOnUser,action.act,reason)
}

enum class LogAction(val action: String, val act: String, val emoji: String)
{
    BAN("banned", "ban", "\uD83D\uDD28"),
    UNBAN("unbanned", "unban", ""),
    KICK("kicked", "kick", "\uD83D\uDC62"),
    MUTE("muted", "mute", "\uD83D\uDD07"),
    UNMUTE("unmuted", "unmute", "\uD83D\uDD08"),
    CLEAN("deleted %d messages in", "clean", ""),
    OTHER("","","");

    companion object {
        fun getActionByAct(act: String) : LogAction = when(act.toLowerCase()) {
            "ban" -> LogAction.BAN
            "unban" -> LogAction.UNBAN
            "kick" -> LogAction.KICK
            "mute" -> LogAction.MUTE
            "unmute" -> LogAction.UNMUTE
            "clean" -> LogAction.CLEAN
            else -> LogAction.OTHER
        }
    }
}

// TODO Remove when unnecessary
@Suppress("unused")
class ModLogger(val manager: DatabaseManager)
{
    fun newBan(mod: Member, target: User) = newBan(mod, target, null)
    fun newBan(mod: Member, target: User, reason: String?) {
        val case = Case()
        case.number = manager.getCases(mod.guild).size+1
        case.guildId = mod.guild.idLong
        case.modId = mod.user.idLong
        case.targetId = target.idLong
        case.isOnUser = true
        case.action = LogAction.BAN
        if(reason != null)
            case.reason = reason

        val log = manager.getModLog(mod.guild)
        if(log!=null && log.canTalk())
        {
            log.sendMessageFormat(
                    Case.FORMAT,
                    case.number,
                    case.action.emoji,
                    formatUserName(mod.user, true),
                    case.action.action,
                    target.name,
                    target.idLong,
                    case.reason
            ).queue({
                case.messageId = it.idLong
                manager.addCase(case)
            },{ })
        }
    }

    fun newUnban(mod: Member, target: User) = newUnban(mod, target, null)
    fun newUnban(mod: Member, target: User, reason: String?) {
        val case = Case()
        case.number = manager.getCases(mod.guild).size+1
        case.guildId = mod.guild.idLong
        case.modId = mod.user.idLong
        case.targetId = target.idLong
        case.isOnUser = true
        case.action = LogAction.UNBAN
        if(reason != null)
            case.reason = reason

        val log = manager.getModLog(mod.guild)
        if(log!=null && log.canTalk())
        {
            log.sendMessageFormat(
                    Case.FORMAT,
                    case.number,
                    case.action.emoji,
                    formatUserName(mod.user, true),
                    case.action.action,
                    target.name,
                    target.idLong,
                    case.reason
            ).queue({
                case.messageId = it.idLong
                manager.addCase(case)
            },{ })
        }
    }

    fun newKick(mod: Member, target: User) = newKick(mod, target, null)
    fun newKick(mod: Member, target: User, reason: String?) {
        val case = Case()
        case.number = manager.getCases(mod.guild).size+1
        case.guildId = mod.guild.idLong
        case.modId = mod.user.idLong
        case.targetId = target.idLong
        case.isOnUser = true
        case.action = LogAction.KICK
        if(reason != null)
            case.reason = reason

        val log = manager.getModLog(mod.guild)
        if(log!=null && log.canTalk())
        {
            log.sendMessageFormat(
                    Case.FORMAT,
                    case.number,
                    case.action.emoji,
                    formatUserName(mod.user, true),
                    case.action.action,
                    target.name,
                    target.idLong,
                    case.reason
            ).queue({
                case.messageId = it.idLong
                manager.addCase(case)
            },{ })
        }
    }

    fun newMute(mod: Member, target: User) = newMute(mod, target, null)
    fun newMute(mod: Member, target: User, reason: String?) {
        val case = Case()
        case.number = manager.getCases(mod.guild).size+1
        case.guildId = mod.guild.idLong
        case.modId = mod.user.idLong
        case.targetId = target.idLong
        case.isOnUser = true
        case.action = LogAction.MUTE
        if(reason != null)
            case.reason = reason

        val log = manager.getModLog(mod.guild)
        if(log!=null && log.canTalk())
        {
            log.sendMessageFormat(
                    Case.FORMAT,
                    case.number,
                    case.action.emoji,
                    formatUserName(mod.user, true),
                    case.action.action,
                    target.name,
                    target.idLong,
                    case.reason
            ).queue({
                case.messageId = it.idLong
                manager.addCase(case)
            },{ })
        }
    }

    fun newUnmute(mod: Member, target: User) = newUnmute(mod, target, null)
    fun newUnmute(mod: Member, target: User, reason: String?) {
        val case = Case()
        case.number = manager.getCases(mod.guild).size+1
        case.guildId = mod.guild.idLong
        case.modId = mod.user.idLong
        case.targetId = target.idLong
        case.isOnUser = true
        case.action = LogAction.UNMUTE
        if(reason != null)
            case.reason = reason

        val log = manager.getModLog(mod.guild)
        if(log!=null && log.canTalk())
        {
            log.sendMessageFormat(
                    Case.FORMAT,
                    case.number,
                    case.action.emoji,
                    formatUserName(mod.user, true),
                    case.action.action,
                    target.name,
                    target.idLong,
                    case.reason
            ).queue({
                case.messageId = it.idLong
                manager.addCase(case)
            },{ })
        }
    }

    fun newClean(mod: Member, target: TextChannel, number: Int) = newClean(mod, target, number, null)
    fun newClean(mod: Member, target: TextChannel, number: Int, reason: String?) {
        val case = Case()

        case.number = manager.getCases(mod.guild).size+1
        case.guildId = mod.guild.idLong
        case.modId = mod.user.idLong
        case.targetId = target.idLong
        case.isOnUser = false
        case.action = LogAction.CLEAN
        if(reason != null)
            case.reason = reason

        val log = manager.getModLog(mod.guild)
        if(log!=null && log.canTalk())
        {
            log.sendMessageFormat(
                    Case.FORMAT,
                    case.number,
                    case.action.emoji,
                    formatUserName(mod.user, true),
                    case.action.action.format(number),
                    target.name,
                    target.idLong,
                    case.reason
            ).queue({
                case.messageId = it.idLong
                manager.addCase(case)
            },{ })
        }
    }
}