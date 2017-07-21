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
package me.kgustave.nightfury.entities

/**
 * @author Kaidan Gustave
 */
@Suppress("unused")
class Tag constructor()
{
    constructor(name: String, content: String, ownerId: Long, location: String, isGlobal: Boolean) : this()
    {
        this.name = name
        this.content = content
        this.ownerId = ownerId
        this.location = location
        this.isGlobal = isGlobal
    }

    var name : String = ""
    var content : String = ""
    var ownerId : Long = 0L
    var location : String = "global"
        set(value) {
            isGlobal = value.toLowerCase() == "global"
            field = value
        }
    var isGlobal : Boolean = true
        private set(value) {field = value}

    fun toDBArgs() : Array<Any> = arrayOf(name, content, ownerId, location, isGlobal)
}

@Suppress("unused")
class CustomCommand(val tag: Tag, val guildId: Long)