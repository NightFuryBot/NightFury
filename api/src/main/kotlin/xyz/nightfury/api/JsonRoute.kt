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

import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject
import spark.kotlin.RouteHandler
import xyz.nightfury.api.exceptions.HttpException
import xyz.nightfury.api.exceptions.InternalServerException
import xyz.nightfury.api.util.JsonAdaptive
import xyz.nightfury.api.util.NOT_IMPLEMENTED

/**
 * @author Kaidan Gustave
 */
interface JsonRoute<out T> : RequestRoute<JSObject> {
    fun RouteHandler.handle(): T

    fun RouteHandler.handleException(t: Throwable): JSObject {
        if(t is HttpException) {
            status(t.code)

            if(t.log) {
                API.LOG.warn(t.logMessage, this)
            }

            return t.toJson()
        }

        // Purely for debugging when testing
        // this should never occur in production
        if(t is NotImplementedError) {
            status(NOT_IMPLEMENTED)
            return jsonObject {
                this["code"] = NOT_IMPLEMENTED
                this["message"] = "Not implemented"
            }
        }

        API.LOG.error("An internal server error occurred", t)
        return InternalServerException(cause = t).toJson()
    }

    abstract class Base(
        override val method: Method,
        override val path: String
    ): JsonRoute<JSObject> {
        override fun RouteHandler.handleInternally(): JSObject {
            return try { handle() } catch(t: Throwable) { handleException(t) }
        }
    }

    abstract class Adaptive<out A: JsonAdaptive>(
        override val method: Method,
        override val path: String
    ): JsonRoute<A> {
        override fun RouteHandler.handleInternally(): JSObject {
            return try { handle().toJson() } catch(t: Throwable) { handleException(t) }
        }
    }
}