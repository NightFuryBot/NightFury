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

import me.kgustave.kson.KSONException
import spark.kotlin.RouteHandler
import spark.route.HttpMethod.post
import xyz.nightfury.api.APIJsonRoute
import xyz.nightfury.api.exceptions.BadRequestException
import xyz.nightfury.api.objects.Tag
import xyz.nightfury.api.util.CREATED
import xyz.nightfury.db.SQLGlobalTags

/**
 * ### Create Tag - `/api/tags/:scope`
 *
 * Creates a tag in the provided `:scope`.
 *
 * #### Request:
 * ```
 * GET https://nightfury.xyz/api/tags/301012120613552138
 *
 * {
 *   "name": "CoolTag",
 *   "content": "This is a cool tag"
 * }
 * ```
 *
 * #### Response:
 * ```
 * {
 *   "name": "CoolTag",
 *   "content": "This is a cool tag",
 *   "owner_id": 211393686628597761,
 *   "guild_id": null
 * }
 * ```
 *
 * // TODO URL Params
 *
 * // TODO Error responses
 *
 * @author Kaidan Gustave
 */
object CreateTag : APIJsonRoute.Adaptive<Tag>(post, "/$SCOPE_PARAM") {
    override fun RouteHandler.handle(): Tag {
        val scope = determineScope()

        // TODO Check for authorization
        val ownerId = 211393686628597761L

        val requestBody = try { this.requestBody } catch(e: KSONException) {
            throw BadRequestException("Unable to parse request body")
        }

        val name = "name".let {
            if(it !in requestBody) {
                noKey(it)
            } else {
                val value = requestBody.opt<Any>(it) ?: invalidType(it, "string", null)
                if(value !is String)
                    invalidType(it, String::class, value::class)

                // TODO Length checks

                return@let value
            }
        } as String

        val content = "content".let {
            if(it !in requestBody) {
                noKey(it)
            } else {
                val value = requestBody.opt<Any>(it) ?: invalidType(it, "string", null)
                if(value !is String)
                    invalidType(it, String::class, value::class)

                // TODO Length checks

                return@let value
            }
        } as String

        if(scope === null) {
            return handleGlobal(name, content, ownerId)
        } else {
            TODO("Implement local creation of tags")
        }
    }

    private fun RouteHandler.handleGlobal(name: String, content: String, ownerId: Long): Tag {
        SQLGlobalTags.addTag(name, ownerId, content)
        status(CREATED)
        return Tag(name, content, ownerId, null)
    }
}