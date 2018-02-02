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
package xyz.nightfury.api.exceptions

import me.kgustave.kson.KSONObject
import me.kgustave.kson.kson
import xyz.nightfury.api.objects.KSONAdapter

/**
 * @author Kaidan Gustave
 */
@Suppress("CanBePrimaryConstructorProperty", "MemberVisibilityCanBePrivate")
open class HttpException(code: Int, message: String, log: Boolean, cause: Throwable? = null): Exception(), KSONAdapter {
    val code: Int = code
    final override val message = message
    val log = log
    override val cause: Throwable? = cause

    open val logMessage: String = "$code - $message"

    override fun toKSON(): KSONObject {
        return kson {
            this["code"] = code
            this["message"] = message
        }
    }
}