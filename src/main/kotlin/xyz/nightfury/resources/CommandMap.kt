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
package xyz.nightfury.resources

import xyz.nightfury.Command

/**
 * An implementation of [Collection] used for mapping [Commands][Command] to their
 * specific [names][Command.name] and [aliases][Command.aliases].
 *
 * The "mapping" is made possible by an indexed backing, maintained by a [HashMap]
 * where Command names and aliases are the [keys][MutableMap.keys] and the [integer][Int]
 * index of these Commands in the initial [commands] [Array] are [values][MutableMap.values].
 *
 * Because of the nature of NightFury's Command system, modifications cannot be made
 * to whatever command store is used, thus [MutableCollection] was left in favor of
 * it's unmodifiable counterpart.
 *
 * It was also chosen to forgo implementation of [Map], as the means of retrieving
 * Commands from the CommandMap is through specifying a String name and does not
 * require the initial Command Array to be exposed outside of this Collection.
 *
 * @param  commands The backing [Array] of [Commands][Command].
 *
 * @author Kaidan Gustave
 */
class CommandMap(private vararg val commands : Command) : Collection<Command> {
    private val map = HashMap<String, Int>()

    /** The size of the initial [Command] [Array]. */
    override val size: Int = commands.size

    init {
        commands.forEachIndexed { index, command ->
            map.put(command.name.toLowerCase(), index)
            command.aliases.forEach { map.put(it.toLowerCase(), index) }
        }
    }

    /**
     * `get` operator shortcut for [getCommandByName].
     *
     * **This is case-insensitive!**
     *
     * @param  name The name of the [Command] to get.
     *
     * @return A [Command] with a matching [name], or
     * `null` if no Command matches.
     */
    operator fun get(name: String): Command? = getCommandByName(name)

    /**
     * `contains` operator shortcut for [containsName].
     *
     * **This is case-insensitive!**
     *
     * @param  name The name of the [Command] to check for.
     *
     * @return `true` if this [CommandMap] contains a
     * [Command] with a matching [name], `false` otherwise.
     */
    operator fun contains(name: String): Boolean = containsName(name)

    /**
     * Gets a [Command] with a matching [name], or `null` if no
     * Command matches.
     *
     * **This is case-insensitive!**
     *
     * @param  name The name of the [Command] to get.
     *
     * @return A [Command] with a matching [name], or `null` if
     * no Command matches.
     */
    fun getCommandByName(name: String): Command? = if(containsName(name)) {
        commands.getOrNull(map.getOrDefault(name.toLowerCase(), -1))
    } else null

    /**
     * Returns `true` if and only if the backing [map] index
     * contains a matching [name], `false` otherwise.
     *
     * **This is case-insensitive!**
     *
     * @param  name The name of the [Command] to check for.
     *
     * @return `true` if this [CommandMap] contains a
     * [Command] with a matching [name], `false` otherwise.
     */
    fun containsName(name: String): Boolean = map.contains(name.toLowerCase())

    /**
     * Returns `true` if and only if the backing [map] index
     * contains every and all [names], `false` otherwise.
     *
     * **This is case-insensitive!**
     *
     * @param  names The names of the [Commands][Command] to
     * check for.
     *
     * @return `true` if this [CommandMap] contains all
     * [Commands][Command] with matching [names], `false`
     * otherwise.
     */
    fun containsAllNames(names: Collection<String>): Boolean {
        names.forEach { if(!map.contains(it)) return false }
        return true
    }

    ///////////////
    // OVERRIDES //
    ///////////////

    override fun contains(element: Command): Boolean = commands.contains(element)

    override fun containsAll(elements: Collection<Command>): Boolean {
        elements.forEach { if(!commands.contains(it)) return false }
        return true
    }

    override fun isEmpty(): Boolean = commands.isEmpty() && map.isEmpty()

    override fun iterator(): Iterator<Command> = commands.iterator()
}