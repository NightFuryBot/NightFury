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
package me.kgustave.nightfury.extensions

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import me.kgustave.nightfury.entities.get
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.requests.restaction.pagination.AuditLogPaginationAction
import java.util.*

infix fun Guild.refreshMutedRole(role: Role)
{
    categories.forEach    { it muteRole role }
    textChannels.forEach  { it muteRole role }
    voiceChannels.forEach { it muteRole role }
}

infix fun Category.muteRole(role: Role)
{
    if(!guild.selfMember.hasPermission(Permission.MANAGE_PERMISSIONS))
        return
    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied != null)
    {
        val cannotWrite = denied.contains(Permission.MESSAGE_WRITE)
        val cannotAddReaction = denied.contains(Permission.MESSAGE_ADD_REACTION)
        val cannotSpeak = denied.contains(Permission.VOICE_SPEAK)
        if(cannotWrite && cannotAddReaction && cannotSpeak)
            return
        with(overrides.managerUpdatable) {
            if(!cannotWrite)
                deny(Permission.MESSAGE_WRITE)
            if(!cannotAddReaction)
                deny(Permission.MESSAGE_ADD_REACTION)
            if(!cannotSpeak)
                deny(Permission.VOICE_SPEAK)
            update().queue()
        }
    }
    else createPermissionOverride(role)
            .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION, Permission.VOICE_SPEAK).queue()
}

infix fun TextChannel.muteRole(role: Role)
{
    if(!guild.selfMember.hasPermission(Permission.MANAGE_PERMISSIONS))
        return
    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied != null)
    {
        val cannotWrite = denied.contains(Permission.MESSAGE_WRITE)
        val cannotAddReaction = denied.contains(Permission.MESSAGE_ADD_REACTION)
        if(cannotWrite && cannotAddReaction)
            return
        with(overrides.managerUpdatable) {
            if(!cannotWrite)
                deny(Permission.MESSAGE_WRITE)
            if(!cannotAddReaction)
                deny(Permission.MESSAGE_ADD_REACTION)
            update().queue()
        }
    }
    else createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue()
}

infix fun VoiceChannel.muteRole(role: Role)
{
    if(!guild.selfMember.hasPermission(Permission.MANAGE_PERMISSIONS))
        return
    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied != null)
    {
        val cannotSpeak = denied.contains(Permission.VOICE_SPEAK)
        if(cannotSpeak) return
        overrides.manager.deny(Permission.VOICE_SPEAK).queue()
    }
    else createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue()
}

fun Message.removeMenuReactions()
{
    if(!author.isSelf) return
    if(isFromType(ChannelType.TEXT) && member.hasPermission(textChannel,Permission.MESSAGE_MANAGE))
        clearReactions().queue()
    else reactions.forEach {
        if(it.isSelf) it.removeReaction(this.author).queue()
    }
}

infix fun Member.canView(channel: TextChannel) = getPermissions(channel).contains(Permission.MESSAGE_READ)
infix fun Role.canView(channel: TextChannel) = hasPermission(channel, Permission.MESSAGE_READ)
infix fun Member.canJoin(channel: VoiceChannel) = getPermissions(channel).contains(Permission.VOICE_CONNECT)
infix fun Role.canJoin(channel: VoiceChannel) = hasPermission(channel, Permission.VOICE_CONNECT)

infix inline fun AuditLogPaginationAction.limit(lazy: () -> Int) : AuditLogPaginationAction = limit(lazy())

infix inline fun AuditLogPaginationAction.action(lazy: () -> ActionType) : AuditLogPaginationAction = type(lazy())

fun MessageHistory.past(number: Int, breakIf: (List<Message>) -> Boolean = { false },
        catch: (Throwable) -> Unit = { throw it }, block: suspend (MutableList<Message>) -> Unit
) = launch(CommonPool) {
    require(number > 0) { "Cannot retrieve less than one past message!" }

    if(number<=100)
        return@launch block(retrievePast(number).get(context) ?: mutableListOf())

    val list = LinkedList<Message>()
    var left = number

    while(left > 100)
    {
        list.addAll(retrievePast(100).get(context) ?: break)
        left -= 100
        if(breakIf(list))
        {
            left = 0
            break
        }
    }

    if(left in 1..100)
        list.addAll(retrievePast(left).get(context) ?: emptyList())

    block(list)

}.invokeOnCompletion {
    if(it != null)
        catch(it)
}