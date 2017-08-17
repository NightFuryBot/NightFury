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
@file:Suppress("unused")
package me.kgustave.nightfury.resources

import me.kgustave.nightfury.Command

/**
 * @author Kaidan Gustave
 */
class CommandMap(private vararg val commands : Command) : Collection<Command>
{
    private val index = HashMap<String, Int>()
    override val size: Int = commands.size

    init {
        commands.forEachIndexed { index, command ->
            this.index.put(command.name.toLowerCase(), index)
            command.aliases.forEach { this.index.put(it.toLowerCase(), index) }
        }
    }

    operator fun get(name: String) = getCommandByName(name)
    operator fun contains(name: String) = containsName(name)

    fun containsName(name: String) = index.contains(name.toLowerCase())
    fun containsAllNames(names: Collection<String>) = index.keys.containsAll(names.map { it.toLowerCase() })

    fun getCommandByName(name: String) = if(containsName(name)) { commands[index[name.toLowerCase()]!!] } else null

    override fun contains(element: Command) = commands.contains(element)
    override fun containsAll(elements: Collection<Command>) : Boolean {
        elements.forEach { if(!commands.contains(it)) return false }
        return true
    }
    override fun isEmpty() = commands.isEmpty() && index.isEmpty()
    override fun iterator() = commands.iterator()
}