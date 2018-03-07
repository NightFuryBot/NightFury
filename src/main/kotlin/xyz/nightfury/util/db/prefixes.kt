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
import xyz.nightfury.ndb.PrefixesHandler

inline val <reified G: Guild> G.prefixes: List<String> inline get() {
    return PrefixesHandler.getPrefixes(idLong)
}

inline fun <reified G: Guild> G.hasPrefix(prefix: String): Boolean {
    return PrefixesHandler.hasPrefix(idLong, prefix)
}

inline fun <reified G: Guild> G.addPrefix(prefix: String) {
    PrefixesHandler.addPrefix(idLong, prefix)
}

inline fun <reified G: Guild> G.removePrefix(prefix: String) {
    PrefixesHandler.removePrefix(idLong, prefix)
}
