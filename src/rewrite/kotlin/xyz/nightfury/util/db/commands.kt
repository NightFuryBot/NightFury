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
import xyz.nightfury.ndb.entities.CommandSettings


inline fun <reified G: Guild> G.getSettings(command: Command): CommandSettings? {
    return CommandSettingsManager.getSettings(idLong, command.fullname)
}

inline fun <reified G: Guild> G.setCommandLevel(command: Command, level: Command.Level?) {
    getSettings(command)?.let { it.level = level?.name }
    ?: CommandSettings(idLong, command.fullname, level?.name, null).let {
        CommandSettingsManager.setSettings(it)
    }
}

inline fun <reified G: Guild> G.setCommandLimit(command: Command, limit: Int?) {
    getSettings(command)?.let { it.limitNumber = limit?.takeIf { it > 0 } }
    ?: CommandSettings(idLong, command.fullname, null, limit?.takeIf { it > 0 }).let {
        CommandSettingsManager.setSettings(it)
    }
}

inline fun <reified G: Guild> G.removeSettings(command: Command) {
    CommandSettingsManager.removeSettings(idLong, command.fullname)
}

inline fun <reified G: Guild> G.removeAllSettings() {
    CommandSettingsManager.removeAllSettings(idLong)
}

inline var CommandSettings.commandLevel: Command.Level?
    inline get() = this.level?.let { Command.Level.valueOf(it) }
    inline set(value) { this.level = value?.name }
