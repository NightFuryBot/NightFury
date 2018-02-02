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

import me.kgustave.kson.KSONObject
import me.kgustave.kson.KSONTokener
import me.kgustave.kson.kson
import spark.kotlin.*
import spark.route.HttpMethod
import xyz.nightfury.api.exceptions.HttpException
import xyz.nightfury.api.exceptions.InternalServerException
import xyz.nightfury.api.objects.KSONAdapter
import xyz.nightfury.api.util.NOT_IMPLEMENTED

/**
 * @author Kaidan Gustave
 */
interface APIJsonRoute<out T>: APIBodyRoute<KSONObject> {

    fun RouteHandler.handle(): T

    fun RouteHandler.handleException(e: Throwable): KSONObject {
        if(e is HttpException) {
            status(e.code)

            if(e.log) {
                API.LOG.warn(e.logMessage, this)
            }

            return e.toKSON()
        }

        // Purely for debugging when testing
        // this should never occur in production
        if(e is NotImplementedError) {
            status(NOT_IMPLEMENTED)
            return kson {
                "code" to NOT_IMPLEMENTED
                "message" to "Not implemented"
            }
        }

        API.LOG.error("An internal server error occurred", e)
        return InternalServerException(cause = e).toKSON()
    }

    val RouteHandler.requestBody: KSONObject
        get() = request.bodyAsBytes().inputStream().use { KSONObject(KSONTokener(it)) }

    override fun RouteHandler.populateHeaders() {
        response.header("Content-Type", API.DEFAULT_CONTENT_TYPE.toString())
    }

    abstract class Base
    constructor(override val method: HttpMethod, override val path: String): APIJsonRoute<KSONObject> {
        override fun RouteHandler.handleInternally(): KSONObject {
            return try { handle() } catch(e: Throwable) { handleException(e) }
        }
    }

    abstract class Adaptive<out A: KSONAdapter>
    constructor(override val method: HttpMethod, override val path: String): APIJsonRoute<A> {
        override fun RouteHandler.handleInternally(): KSONObject {
            return try { handle().toKSON() } catch(e: Throwable) { handleException(e) }
        }
    }
}