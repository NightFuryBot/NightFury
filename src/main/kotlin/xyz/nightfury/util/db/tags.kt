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
@file:Suppress("Unused")
package xyz.nightfury.util.db

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.ndb.entities.DBTag
import xyz.nightfury.ndb.entities.Tag
import xyz.nightfury.ndb.tags.GlobalTagsHandler
import xyz.nightfury.ndb.tags.LocalTagsHandler

inline val <reified G: Guild> G.tags: List<Tag> inline get() {
    return LocalTagsHandler.getTags(idLong)
}

inline val <reified M: Member> M.tags: List<Tag> inline get() {
    return LocalTagsHandler.getTags(guild.idLong, user.idLong)
}

inline val <reified U: User> U.tags: List<Tag> inline get() {
    return GlobalTagsHandler.getTags(idLong)
}

inline fun <reified J: JDA> J.getTagByName(name: String): Tag? {
    return GlobalTagsHandler.getTagByName(name)
}

inline fun <reified G: Guild> G.getTagByName(name: String): Tag? {
    return LocalTagsHandler.getTagByName(name, idLong) ?: jda.getTagByName(name)
}

inline fun <reified G: Guild> G.addTag(tag: Tag) {
    LocalTagsHandler.addTag(tag)
}

inline fun <reified M: Member> M.addTag(name: String, content: String) {
    guild.addTag(DBTag(name, content, user.idLong, guild.idLong))
}

inline fun <reified J: JDA> J.addTag(tag: Tag) {
    GlobalTagsHandler.addTag(tag)
}

inline fun <reified U: User> U.addTag(name: String, content: String) {
    jda.addTag(DBTag(name, content, idLong, null))
}

inline fun <reified G: Guild> G.isTag(name: String): Boolean {
    return LocalTagsHandler.isTag(name, idLong)
}

inline fun <reified J: JDA> J.isTag(name: String): Boolean {
    return GlobalTagsHandler.isTag(name)
}
