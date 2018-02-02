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
import spark.route.HttpMethod.patch
import xyz.nightfury.api.APIJsonRoute
import xyz.nightfury.api.exceptions.NotFoundException
import xyz.nightfury.api.exceptions.UnauthorizedException
import xyz.nightfury.api.objects.Tag
import xyz.nightfury.api.util.OK
import xyz.nightfury.db.SQLGlobalTags

/**
 * @author Kaidan Gustave
 */
object EditTag : APIJsonRoute.Adaptive<Tag>(patch, "/$SCOPE_PARAM/$NAME_PARAM") {
    override fun RouteHandler.handle(): Tag {
        val name = params(NAME_PARAM)
        val guildId = determineScope()

        // TODO Check for authorization
        val ownerId = 211393686628597761L

        val content = "content".let {
            val requestBody = requestBody
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

        if(guildId === null) {
            return handleGlobal(name, content, ownerId)
        } else {
            TODO("Implement local tag editing")
        }
    }

    private fun RouteHandler.handleGlobal(name: String, content: String, ownerId: Long): Tag {
        if(!SQLGlobalTags.isTag(name)) {
            throw NotFoundException("Tag not found")
        }

        // Not the owner of the tag
        if(SQLGlobalTags.getTagOwnerId(name) != ownerId) {
            throw UnauthorizedException()
        }

        // Delete the tag
        SQLGlobalTags.editTag(name, content, ownerId)

        status(OK)
        return Tag(SQLGlobalTags.getOriginalName(name), content, ownerId, null)
    }
}