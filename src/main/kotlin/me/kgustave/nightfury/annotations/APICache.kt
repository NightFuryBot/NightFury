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
package me.kgustave.nightfury.annotations

/**
 * Signifies that a command uses a cache (typically an API cache).
 *
 * This is used for targeting commands that use API caches that should be
 * cleared at regular intervals.
 *
 * Applying this is done via application to the Command [class][kotlin.reflect.KClass]
 * and a [function][kotlin.reflect.KFunction] to run to clear the cache.
 *
 * @author Kaidan Gustave
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class APICache