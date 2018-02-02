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
import spark.route.HttpMethod
import xyz.nightfury.api.exceptions.HttpException
import xyz.nightfury.api.util.INTERNAL_SERVER_ERROR
import xyz.nightfury.api.util.NOT_IMPLEMENTED

/**
 * @author Kaidan Gustave
 */
interface APINoBodyRoute : AppendableRoute {
    val method: HttpMethod
    val contentType: String
        get() = API.DEFAULT_CONTENT_TYPE.toString()

    fun RouteHandler.handle()

    fun RouteHandler.handleException(e: Throwable)

    override fun append() {
        when(method) {
            HttpMethod.get -> get(path, accepts = contentType) { handle() }
            HttpMethod.post -> post(path, accepts = contentType) { handle() }
            HttpMethod.patch -> patch(path, accepts = contentType) { handle() }
            HttpMethod.put -> put(path, accepts = contentType) { handle() }
            HttpMethod.delete -> delete(path, accepts = contentType) { handle() }
            HttpMethod.head -> head(path, accepts = contentType) { handle() }
            HttpMethod.trace -> trace(path, accepts = contentType) { handle() }
            HttpMethod.connect -> connect(path, accepts = contentType) { handle() }
            HttpMethod.options -> options(path, accepts = contentType) { handle() }
            HttpMethod.before -> before(path, accepts = contentType) { handle() }
            HttpMethod.after -> after(path, accepts = contentType) { handle() }
            else -> throw UnsupportedOperationException("Method '$method' is not supported!")
        }
    }

    abstract class Base(override val method: HttpMethod, override val path: String): APINoBodyRoute {
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