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
package xyz.nightfury.ndb.entities

import xyz.nightfury.ndb.tags.GlobalTagsHandler
import xyz.nightfury.ndb.tags.LocalTagsHandler

interface Tag {
    val name: String
    var content: String
    val ownerId: Long?
    val guildId: Long?
    val isGlobal: Boolean get() = guildId === null
    val isOverride: Boolean get() = ownerId === null

    fun delete() {
        if(isGlobal) {
            GlobalTagsHandler.removeTag(this)
        } else {
            LocalTagsHandler.removeTag(this)
        }
    }

    fun override()
}