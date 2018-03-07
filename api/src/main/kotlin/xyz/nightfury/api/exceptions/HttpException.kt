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

import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject
import xyz.nightfury.api.util.JsonAdaptive

/**
 * @author Kaidan Gustave
 */
open class HttpException(
    val code: Int,
    message: String,
    val log: Boolean,
    cause: Throwable? = null
): Exception(message, cause), JsonAdaptive {

    open val logMessage: String = "$code - $message"

    override fun toJson(): JSObject = jsonObject {
        this["code"] = code
        this["message"] = message
    }
}