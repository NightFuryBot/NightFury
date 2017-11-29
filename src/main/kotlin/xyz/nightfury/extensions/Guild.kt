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
package xyz.nightfury.extensions

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.requests.restaction.pagination.AuditLogPaginationAction
import xyz.nightfury.entities.succeed
import java.util.*

infix fun Guild.refreshMutedRole(role: Role) {
    categories.forEach    { it muteRole role }
    textChannels.forEach  { it muteRole role }
    voiceChannels.forEach { it muteRole role }
}

infix fun Category.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(MANAGE_PERMISSIONS))
        return
    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied != null) {
        val cannotWrite = denied.contains(MESSAGE_WRITE)
        val cannotAddReaction = denied.contains(MESSAGE_ADD_REACTION)
        val cannotSpeak = denied.contains(VOICE_SPEAK)
        if(cannotWrite && cannotAddReaction && cannotSpeak)
            return
        with(overrides.managerUpdatable) {
            if(!cannotWrite)
                deny(MESSAGE_WRITE)
            if(!cannotAddReaction)
                deny(MESSAGE_ADD_REACTION)
            if(!cannotSpeak)
                deny(VOICE_SPEAK)
            update().queue()
        }
    } else createPermissionOverride(role).setDeny(MESSAGE_WRITE, MESSAGE_ADD_REACTION, VOICE_SPEAK).queue()
}

infix fun TextChannel.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(MANAGE_PERMISSIONS))
        return
    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied != null) {
        val cannotWrite = denied.contains(MESSAGE_WRITE)
        val cannotAddReaction = denied.contains(MESSAGE_ADD_REACTION)
        if(cannotWrite && cannotAddReaction)
            return
        with(overrides.managerUpdatable) {
            if(!cannotWrite)
                deny(MESSAGE_WRITE)
            if(!cannotAddReaction)
                deny(MESSAGE_ADD_REACTION)
            update().queue()
        }
    } else createPermissionOverride(role).setDeny(MESSAGE_WRITE, MESSAGE_ADD_REACTION).queue()
}

infix fun VoiceChannel.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(MANAGE_PERMISSIONS))
        return
    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied != null) {
        val cannotSpeak = denied.contains(VOICE_SPEAK)
        if(cannotSpeak)
            return
        overrides.manager.deny(VOICE_SPEAK).queue()
    } else createPermissionOverride(role).setDeny(MESSAGE_WRITE, MESSAGE_ADD_REACTION).queue()
}

fun Message.removeMenuReactions() {
    if(!author.isSelf || !isFromType(ChannelType.TEXT))
        return
    if(member.hasPermission(textChannel,MESSAGE_MANAGE)) {
        clearReactions().queue()
    } else reactions.forEach {
        if(it.isSelf)
            it.removeReaction(this.author).queue()
    }
}

infix fun Member.canView(channel: TextChannel) = getPermissions(channel).contains(MESSAGE_READ)
infix fun Role.canView(channel: TextChannel) = hasPermission(channel, MESSAGE_READ)
infix fun Member.canJoin(channel: VoiceChannel) = getPermissions(channel).contains(VOICE_CONNECT)
infix fun Role.canJoin(channel: VoiceChannel) = hasPermission(channel, VOICE_CONNECT)

infix inline fun AuditLogPaginationAction.limit(lazy: () -> Int) : AuditLogPaginationAction = limit(lazy())
infix inline fun AuditLogPaginationAction.action(lazy: () -> ActionType) : AuditLogPaginationAction = type(lazy())

suspend fun MessageHistory.getPast(number: Int, breakIf: suspend (List<Message>) -> Boolean = { false }): MutableList<Message> {
    require(number > 0) { "Minimum of one message must be retrieved" }

    if(number <= 100)
        return retrievePast(number).succeed() ?: mutableListOf()

    val list = LinkedList<Message>()
    var left = number

    while(left > 100) {
        list += retrievePast(100).succeed() ?: break
        left -= 100
        if(breakIf(list)) {
            left = 0
            break
        }
    }

    if(left in 1..100)
        retrievePast(left).succeed()?.let { list += it }

    return list
}