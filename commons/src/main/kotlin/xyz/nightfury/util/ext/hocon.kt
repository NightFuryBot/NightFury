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
package xyz.nightfury.util.ext

import ninja.leaping.configurate.commented.CommentedConfigurationNode as HoconNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader as HoconLoader

/**
 * @author Kaidan Gustave
 */
inline fun hocon(block: HoconLoader.Builder.() -> Unit): HoconNode {
    return HoconLoader.builder().also(block).build().load()
}

inline fun <reified N: HoconNode> N.node(vararg path: String): HoconNode? {
    return getNode(*path)?.takeIf { !it.isVirtual }
}

inline operator fun <reified T> HoconNode.get(vararg path: String): T? {
    return node(*path)?.getValue { value -> value as? T }
}

inline fun <reified T> HoconNode.get(vararg path: String, default: T): T {
    return get<T>(*path) ?: default
}

inline fun <reified T> HoconNode.getList(vararg path: String, crossinline transform: (Any) -> T): List<T>? {
    return node(*path)?.getList { transform(it) }
}