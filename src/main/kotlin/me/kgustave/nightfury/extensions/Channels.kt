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

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*

fun TextChannel.muteRole(role: Role)
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


fun VoiceChannel.muteRole(role: Role)
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