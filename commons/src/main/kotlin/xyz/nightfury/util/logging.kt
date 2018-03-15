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
package xyz.nightfury.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun createLogger(name: String): Logger = LoggerFactory.getLogger(name)
fun <T: Any> createLogger(klazz: KClass<T>): Logger = LoggerFactory.getLogger(klazz.java)

inline fun <reified L: Logger> L.info(block: () -> String) = info(block())

inline fun <reified L: Logger> L.warn(block: () -> String) = warn(block())

inline fun <reified L: Logger> L.error(block: () -> String) = error(block())

inline fun <reified L: Logger> L.debug(block: () -> String) = debug(block())

