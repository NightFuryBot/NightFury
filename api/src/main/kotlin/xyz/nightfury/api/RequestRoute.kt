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

interface RequestRoute<out T: Any> : Route {
    companion object {
        val STANDARD_HEADERS = mapOf(
            "Access-Control-Allow-Origin" to "*"
        )
    }

    val method: Method
    val contentType get() = "${API.DEFAULT_CONTENT_TYPE}"

    fun RouteHandler.handleInternally(): T

    fun RouteHandler.populateHeaders() {}

    private fun RouteHandler.preHandle(): T {
        STANDARD_HEADERS.forEach { t, u -> response.header(t, u) }
        populateHeaders()
        return handleInternally()
    }

    override fun create() {
        when(method) {
            Method.GET -> get(path, accepts = contentType) { preHandle() }
            Method.POST -> post(path, accepts = contentType) { preHandle() }
            Method.PATCH -> patch(path, accepts = contentType) { preHandle() }
            Method.PUT -> put(path, accepts = contentType) { preHandle() }
            Method.DELETE -> delete(path, accepts = contentType) { preHandle() }
            Method.HEAD -> head(path, accepts = contentType) { preHandle() }
        }
    }
}