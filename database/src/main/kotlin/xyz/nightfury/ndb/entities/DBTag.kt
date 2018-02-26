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

/**
 * @author Kaidan Gustave
 */
class DBTag(
    override val name: String,
    content: String,
    ownerId: Long?,
    override val guildId: Long?
): Tag {
    override var ownerId: Long? = ownerId
        private set(value) {
            field = value
            if(value === null) {
                if(isGlobal) {
                    GlobalTagsHandler.editTag(this)
                } else {
                    LocalTagsHandler.editTag(this)
                }
            }
        }

    override var content: String = content
        set(value) {
            field = value
            if(isGlobal) {
                GlobalTagsHandler.editTag(this)
            } else {
                LocalTagsHandler.editTag(this)
            }
        }

    override fun override() {
        ownerId = null
        if(isGlobal) {
            GlobalTagsHandler.editTag(this)
        } else {
            LocalTagsHandler.editTag(this)
        }
    }

    override fun hashCode(): Int = "$name$guildId$ownerId".hashCode()
    override fun equals(other: Any?): Boolean {
        if(other !is Tag)
            return false

        return name.equals(other.name, true) &&
               (guildId == null || guildId == other.guildId) &&
               (ownerId == null || ownerId == other.ownerId)
    }
}