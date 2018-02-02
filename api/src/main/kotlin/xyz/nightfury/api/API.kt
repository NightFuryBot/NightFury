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

import org.apache.http.entity.ContentType
import spark.Spark
import xyz.nightfury.api.routes.HeadAll
import xyz.nightfury.api.routes.tags.Tags
import xyz.nightfury.extensions.createLogger

/**
 * @author Kaidan Gustave
 */
object API : APIRouteGroup(null, path = "/api") {
    val DEFAULT_CONTENT_TYPE = ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8)!!

    val LOG = createLogger(API::class)

    private var port = 8080

    override fun initChildren() {
        + HeadAll
        + Tags
    }

    fun port(port: Int) {
        this.port = port
        LOG.debug("Changed target port to $port")
    }

    fun init() {
        LOG.info("Initializing NightFury API...")
        Spark.port(port)
        this.append()
    }
}