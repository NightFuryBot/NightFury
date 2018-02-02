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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.nightfury.api.objects

import me.kgustave.kson.KSONObject
import me.kgustave.kson.kson

/**
 * ### The Tag Object
 *
 * Tags are neat little "mini-commands" that users can create and use with the `tag` command.
 *
 * +-----------+--------+-------------------------------------------------------------------------+
 * | Key       | Type   | Description                                                             |
 * +-----------+--------+-------------------------------------------------------------------------+
 * | name      | string | The name of the tag.                                                    |
 * | content   | string | The content of the tag.                                                 |
 * | owner_id  | long?  | The Discord ID of the tags owner, `null` if it's locally overriden.     |
 * | guild_id  | long?  | The Discord ID of the Guild this tag belongs to, `null` if it's global. |
 * +-----------+--------+-------------------------------------------------------------------------+
 *
 * An example tag object might look something like this:
 * ```
 * {
 *   "name": "CoolTag",
 *   "content": "This is a cool tag"
 *   "owner_id": 211393686628597761,
 *   "guild_id": 301012120613552138
 * }
 * ```
 *
 * @author Kaidan Gustave
 */
data class Tag
constructor(val name: String, val content: String, val ownerId: Long, val guildId: Long?): KSONAdapter {
    override fun toKSON(): KSONObject = kson {
        "name" to name
        "content" to content
        "owner_id" to ownerId
        "guild_id" to guildId
    }
}