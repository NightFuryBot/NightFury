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
@file:Suppress("ObjectPropertyName")
package xyz.nightfury.api

import com.jagrosh.jdautilities.oauth2.OAuth2Client
import net.dv8tion.jda.core.JDA
import org.apache.http.entity.ContentType
import spark.Spark
import xyz.nightfury.api.oauth2.NightFurySessionController
import xyz.nightfury.api.oauth2.NightFuryStateController
import xyz.nightfury.api.routes.HeadAll
import xyz.nightfury.util.createLogger
import xyz.nightfury.util.hocon
import xyz.nightfury.util.node
import xyz.nightfury.util.resourceStreamOf

/**
 * @author Kaidan Gustave
 */
object API : RouteGroup(null, "/api"){
    val DEFAULT_CONTENT_TYPE = ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8)!!
    val LOG = createLogger(API::class)

    private lateinit var _jda: JDA
    private lateinit var _oAuth2: OAuth2Client
    private var port = 8080

    private val jda: JDA get() = _jda

    val oAuth2: OAuth2Client get() = _oAuth2

    override fun initChildren() {
        + HeadAll
    }

    fun port(port: Int) {
        this.port = port
        LOG.debug("Changed target port to $port")
    }

    fun init(jda: JDA) {
        LOG.info("Initializing NightFury API...")

        val config = hocon {
            setSource { this::class.resourceStreamOf("/api.conf")?.bufferedReader(Charsets.UTF_8) }
        }

        val clientId = checkNotNull(config.node("api", "oauth2", "client", "id")?.long) {
            "Unable to get OAuth2 Client ID from api.conf!"
        }

        val secret = checkNotNull(config.node("api", "oauth2", "client", "secret")?.string) {
            "Unable to get OAuth2 Client Secret from api.conf!"
        }

        _oAuth2 = OAuth2Client.Builder()
            .setClientId(clientId)
            .setClientSecret(secret)
            .setStateController(NightFuryStateController)
            .setSessionController(NightFurySessionController)
            .build()

        Spark.port(port)
        _jda = jda
        this.create()
    }
}