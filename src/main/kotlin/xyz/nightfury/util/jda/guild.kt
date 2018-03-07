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
package xyz.nightfury.util.jda

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*

fun Guild.refreshMutedRole(role: Role) {
    categories.forEach    { it.muteRole(role) }
    textChannels.forEach  { it.muteRole(role) }
    voiceChannels.forEach { it.muteRole(role) }
}

fun Category.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS))
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

fun TextChannel.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS))
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

fun VoiceChannel.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS))
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

infix fun Member.canView(channel: TextChannel): Boolean = hasPermission(channel, MESSAGE_READ)
infix fun Role.canView(channel: TextChannel): Boolean = hasPermission(channel, MESSAGE_READ)
infix fun Member.canJoin(channel: VoiceChannel): Boolean = hasPermission(channel, VOICE_CONNECT)
infix fun Role.canJoin(channel: VoiceChannel): Boolean = hasPermission(channel, VOICE_CONNECT)
