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
package xyz.nightfury.resources

import kotlin.reflect.KProperty

/**
 * @author Kaidan Gustave
 */
class OneTimeInit<in I: Any, T: Any>(val default: T?) {

    lateinit var variable: T

    operator fun getValue(instance: I, property: KProperty<*>): T {
        if(!::variable.isInitialized) {
            return requireNotNull(default?.also { setValue(instance, property, it) }) {
                "Cannot get OneTimeInit delegated property without first initializing!"
            }
        }
        return variable
    }

    operator fun setValue(instance: I, property: KProperty<*>, value: T) {
        require(!::variable.isInitialized) { "Cannot instantiate OneTimeInit variable more than once!" }
        variable = value
    }
}

inline fun <reified I: Any, reified T: Any> onlyInitializingOnce(default: T? = null) = OneTimeInit<I, T>(default)