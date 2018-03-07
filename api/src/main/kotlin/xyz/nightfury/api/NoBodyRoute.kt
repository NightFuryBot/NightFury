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
package xyz.nightfury.api

import spark.kotlin.*
import xyz.nightfury.api.exceptions.HttpException
import xyz.nightfury.api.util.INTERNAL_SERVER_ERROR
import xyz.nightfury.api.util.NOT_IMPLEMENTED

interface NoBodyRoute : Route {
    val method: Method
    val contentType get() = "${API.DEFAULT_CONTENT_TYPE}"

    fun RouteHandler.handle()

    fun RouteHandler.handleException(e: Throwable)

    fun RouteHandler.handleInternally() {
        try { handle() } catch(t: Throwable) { handleException(t) }
    }

    override fun create() {
        when(method) {
            Method.GET -> get(path, accepts = contentType) { handleInternally() }
            Method.POST -> post(path, accepts = contentType) { handleInternally() }
            Method.PATCH -> patch(path, accepts = contentType) { handleInternally() }
            Method.PUT -> put(path, accepts = contentType) { handleInternally() }
            Method.DELETE -> delete(path, accepts = contentType) { handleInternally() }
            Method.HEAD -> head(path, accepts = contentType) { handleInternally() }
        }
    }

    abstract class Base(override val method: Method, override val path: String): NoBodyRoute {
        override fun RouteHandler.handleException(e: Throwable) {
            if(e is HttpException) {
                if(e.log) { API.LOG.warn(e.logMessage, this) }
                return status(e.code)
            }

            // Purely for debugging when testing
            // this should never occur in production
            if(e is NotImplementedError) {
                return status(NOT_IMPLEMENTED)
            }

            API.LOG.error("An internal server error occurred", e)
            status(INTERNAL_SERVER_ERROR)
        }
    }
}