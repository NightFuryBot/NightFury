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
import spark.route.HttpMethod.get
import xyz.nightfury.api.APIJsonRoute
import xyz.nightfury.api.exceptions.NotFoundException
import xyz.nightfury.api.objects.Tag
import xyz.nightfury.api.util.OK
import xyz.nightfury.db.SQLGlobalTags
import xyz.nightfury.db.SQLLocalTags

/**
 * ### Get Tag - `GET /api/tags/:scope/:name`
 *
 * Gets a tag matching the `:name` (case-insensitive) from the provided `:scope`.
 *
 * #### Request:
 * ```
 * GET https://nightfury.xyz/api/tags/global/cooltag
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
 * #### URL Params:
 * 1) `:scope` The scope of the request.
 *    - For local tags, this must be a valid Discord Guild ID.
 *    - For global tags, this must be `global`
 * 2) `:name` The case-insensitive tag name to get.
 *
 * #### Error Responses
 * - **Bad Request (400)**: The `:scope` is not a valid Discord Guild ID or `global`.
 * - **Not Found (404)**: A tag with the provided `:name` was not found
 *                       (note this does not check the `global` scope if one is not
 *                       found with the provided Guild ID if specified).
 *
 * @author Kaidan Gustave
 */
object GetTag: APIJsonRoute.Adaptive<Tag>(get, "/$SCOPE_PARAM/$NAME_PARAM") {
    override fun RouteHandler.handle(): Tag {
        val guildId = determineScope()

        val tagName = params(NAME_PARAM)

        // Global tag request.
        if(guildId === null) {
            return handleGlobal(tagName)
        }

        // Local tag request.
        return handleLocal(guildId, tagName)
    }

    private fun RouteHandler.handleGlobal(tagName: String): Tag {
        if(!SQLGlobalTags.isTag(tagName)) {
            throw NotFoundException("Tag not found")
        }

        val name = SQLGlobalTags.getOriginalName(tagName)
        val content = SQLGlobalTags.getTagContent(tagName)
        val ownerId = SQLGlobalTags.getTagOwnerId(tagName)

        return constructTag(name, content, ownerId, null)
    }

    private fun RouteHandler.handleLocal(guildId: Long, tagName: String): Tag {
        if(!SQLLocalTags.isTag(tagName, guildId)) {
            throw NotFoundException("Tag not found")
        }

        val name = SQLLocalTags.getOriginalName(tagName, guildId)
        val content = SQLLocalTags.getTagContent(tagName, guildId)
        val ownerId = SQLLocalTags.getTagOwnerId(tagName, guildId)

        return constructTag(name, content, ownerId, guildId)
    }

    private fun RouteHandler.constructTag(name: String, content: String, ownerId: Long, guildId: Long?): Tag {
        status(OK)
        return Tag(name, content, ownerId, guildId)
    }
}