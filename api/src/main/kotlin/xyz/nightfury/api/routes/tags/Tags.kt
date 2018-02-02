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
package xyz.nightfury.api.routes.tags

import spark.kotlin.RouteHandler
import xyz.nightfury.api.API
import xyz.nightfury.api.APIRouteGroup
import xyz.nightfury.api.exceptions.BadRequestException
import xyz.nightfury.api.util.length
import kotlin.reflect.KClass

internal const val SCOPE_PARAM = ":scope"
internal const val NAME_PARAM = ":name"
internal const val GLOBAL_SCOPE_KEY = "global"

/**
 * @author Kaidan Gustave
 */
object Tags : APIRouteGroup(parent = API, path = "/tags") {
    override fun initChildren() {
        + GetTag
        + CreateTag
        + DeleteTag
        + EditTag
    }
}

/**
 * Determines the scope of the request based on a `:scope` parameter.
 *
 * This returns `null` for requests to `global` scope.
 */
internal fun RouteHandler.determineScope(): Long? {
    val scope = params(SCOPE_PARAM)

    // Global tag request.
    if(scope.equals(GLOBAL_SCOPE_KEY, ignoreCase = true)) {
        return null
    }

    val guildId = try { scope.toLong() } catch(e: NumberFormatException) {
        // At this point it's not "global" or a valid long.
        throw BadRequestException("Scope must be a valid Discord Guild ID or \"$GLOBAL_SCOPE_KEY\"")
    }

    // Not a valid snowflake length.
    if(guildId.length in 17..20) {
        // The length being too long here means that they gave us a valid LONG
        // but not a valid SNOWFLAKE (at least format wise). We treat the error
        // differently than if it wasn't a valid long because they still made an
        // attempt, so we tell them that it is not a valid snowflake.
        throw BadRequestException("Scope must be a valid Discord Guild ID.")
    }

    return guildId
}


internal fun noKey(key: String): Nothing {
    throw BadRequestException("No '$key' property of JSON request body was found!")
}

internal fun invalidType(key: String, expected: KClass<*>, found: KClass<*>): Nothing {
    invalidType(key, expected.simpleName, found.simpleName)
}

internal fun invalidType(key: String, expected: String?, found: String?): Nothing {
    throw BadRequestException("Expected ${expected?.toLowerCase()} value from '$key' " +
                              "but found ${found?.toLowerCase()}")
}
