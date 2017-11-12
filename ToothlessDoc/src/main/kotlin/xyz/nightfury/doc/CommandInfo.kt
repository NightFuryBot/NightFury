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
@file:Suppress("Unused")
package xyz.nightfury.doc

import org.intellij.lang.annotations.Language

/**
 * @author Kaidan Gustave
 */
@[MustBeDocumented
  Target(AnnotationTarget.CLASS)
  Retention(AnnotationRetention.RUNTIME)
  SinceKotlin("1.2")]
annotation class CommandInfo(
    // We use an array because all commands require at least one name.
    val name: Array<out String>,
    @Language("Markdown") val description: String,
    @Language("Markdown") val requirements: Array<out String> = []
)