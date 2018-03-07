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
package xyz.nightfury.util.db

import net.dv8tion.jda.core.entities.Guild
import xyz.nightfury.command.Command
import xyz.nightfury.ndb.CommandSettingsManager

inline fun <reified G: Guild> G.getCommandLevel(command: Command): Command.Level? {
    return CommandSettingsManager.getLevel(idLong, command.fullname)?.let { Command.Level.valueOf(it) }
}

inline fun <reified G: Guild> G.getCommandLimit(command: Command): Int? {
    return CommandSettingsManager.getLimit(idLong, command.fullname)
}

inline fun <reified G: Guild> G.setCommandLevel(command: Command, level: Command.Level?) {
    CommandSettingsManager.setLevel(idLong, command.fullname, level?.name)
}

inline fun <reified G: Guild> G.setCommandLimit(command: Command, limit: Int?) {
    CommandSettingsManager.setLimit(idLong, command.fullname, limit)
}

inline fun <reified G: Guild> G.removeAllCommandSettings() {
    CommandSettingsManager.removeAllSettings(idLong)
}
