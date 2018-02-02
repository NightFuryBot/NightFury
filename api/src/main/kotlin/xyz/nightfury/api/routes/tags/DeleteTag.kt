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

import me.kgustave.kson.KSONObject
import me.kgustave.kson.kson
import spark.kotlin.RouteHandler
import spark.route.HttpMethod.delete
import xyz.nightfury.api.APIJsonRoute
import xyz.nightfury.api.exceptions.NotFoundException
import xyz.nightfury.api.exceptions.UnauthorizedException
import xyz.nightfury.api.util.OK
import xyz.nightfury.db.SQLGlobalTags
import xyz.nightfury.db.SQLLocalTags

/**
 * @author Kaidan Gustave
 */
object DeleteTag : APIJsonRoute.Base(delete, "/$SCOPE_PARAM/$NAME_PARAM") {
    override fun RouteHandler.handle(): KSONObject {
        val name = params(NAME_PARAM)
        val guildId = determineScope()

        // TODO Check for authorization
        val ownerId = 211393686628597761L

        if(guildId === null) {
            handleGlobal(name, ownerId)
        } else {
            handleLocal(name, ownerId, guildId)
        }

        return kson { "code" to status() }
    }

    private fun RouteHandler.handleGlobal(name: String, ownerId: Long) {
        if(!SQLGlobalTags.isTag(name)) {
            throw NotFoundException("Tag not found")
        }

        // Not the owner of the tag
        if(SQLGlobalTags.getTagOwnerId(name) != ownerId) {
            throw UnauthorizedException()
        }

        // Delete the tag
        SQLGlobalTags.deleteTag(name, ownerId)

        status(OK)
    }

    private fun RouteHandler.handleLocal(name: String, ownerId: Long, guildId: Long) {
        if(!SQLLocalTags.isTag(name, guildId)) {
            throw NotFoundException("Tag not found")
        }

        // Not the owner of the tag
        if(SQLLocalTags.getTagOwnerId(name, guildId) != ownerId) {
            throw UnauthorizedException()
        }

        // Delete the tag
        SQLLocalTags.deleteTag(name, ownerId, guildId)

        status(OK)
    }
}