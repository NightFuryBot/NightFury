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
import xyz.nightfury.ndb.CustomCommandsHandler

inline val <reified G: Guild> G.customCommands: List<Pair<String, String>> inline get() {
    return CustomCommandsHandler.getAllCommands(idLong)
}

inline fun <reified G: Guild> G.isCustomCommand(name: String): Boolean {
    return CustomCommandsHandler.isCommand(idLong, name)
}

inline fun <reified G: Guild> G.getCustomCommand(name: String): String? {
    return CustomCommandsHandler.getCommandContent(idLong, name)
}

inline fun <reified G: Guild> G.setCustomCommand(name: String, content: String) {
    CustomCommandsHandler.setCommandContent(idLong, name, content)
}

inline fun <reified G: Guild> G.removeCustomCommand(name: String) {
    CustomCommandsHandler.removeCommand(idLong, name)
}
