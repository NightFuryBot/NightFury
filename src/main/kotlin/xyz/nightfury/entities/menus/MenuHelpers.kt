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
package xyz.nightfury.entities.menus

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User

internal inline fun <reified M: Menu> M.isValidUser(user: User, guild: Guild? = null): Boolean {
    if(user.isBot)
        return false
    if(users.isEmpty() && roles.isEmpty())
        return true
    if(users.contains(user))
        return true
    if(guild == null)
        return false
    return guild.getMember(user)?.roles?.any { roles.contains(it) } == true
}