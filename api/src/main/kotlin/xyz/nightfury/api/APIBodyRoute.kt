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

/**
 * @author Kaidan Gustave
 */
interface APIBodyRoute<out T: Any> : AppendableRoute {
    companion object {
        val STANDARD_HEADERS = mapOf(
            "Access-Control-Allow-Origin" to "*"
        )
    }

    val method: HttpMethod
    val contentType: String
        get() = API.DEFAULT_CONTENT_TYPE.toString()

    fun RouteHandler.handleInternally(): T

    fun RouteHandler.populateHeaders() {}

    private fun RouteHandler.preHandle(): T {
        STANDARD_HEADERS.forEach { t, u -> response.header(t, u) }
        populateHeaders()
        return handleInternally()
    }

    override fun append() {
        when(method) {
            HttpMethod.get -> get(path, accepts = contentType) { preHandle() }
            HttpMethod.post -> post(path, accepts = contentType) { preHandle() }
            HttpMethod.patch -> patch(path, accepts = contentType) { preHandle() }
            HttpMethod.put -> put(path, accepts = contentType) { preHandle() }
            HttpMethod.delete -> delete(path, accepts = contentType) { preHandle() }
            HttpMethod.head -> head(path, accepts = contentType) { preHandle() }
            HttpMethod.trace -> trace(path, accepts = contentType) { preHandle() }
            HttpMethod.connect -> connect(path, accepts = contentType) { preHandle() }
            HttpMethod.options -> options(path, accepts = contentType) { preHandle() }
            HttpMethod.before -> before(path, accepts = contentType) { preHandle() }
            HttpMethod.after -> after(path, accepts = contentType) { preHandle() }
            else -> throw UnsupportedOperationException("Method '$method' is not supported!")
        }
    }
}