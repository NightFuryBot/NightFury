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
package xyz.nightfury.api.oauth2

import com.jagrosh.jdautilities.oauth2.Scope
import com.jagrosh.jdautilities.oauth2.session.Session as OAuth2Session
import com.jagrosh.jdautilities.oauth2.session.SessionController
import com.jagrosh.jdautilities.oauth2.session.SessionData
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Kaidan Gustave
 */
object NightFurySessionController : SessionController<NightFurySessionController.Session> {
    private val sessions = ConcurrentHashMap<String, Session>()

    override fun getSession(identifier: String): Session? {
        return sessions[identifier]
    }

    override fun createSession(data: SessionData): Session {
        val id = UUID.randomUUID()
        val session = Session(data)
        sessions["$id"] = session
        return session
    }

    data class Session(
        private val _scopes: Array<Scope>,
        private val _expiration: OffsetDateTime,
        private val _accessToken: String,
        private val _tokenType: String,
        private val _refreshToken: String
    ) : OAuth2Session {
        constructor(data: SessionData): this(
            data.scopes,
            data.expiration,
            data.accessToken,
            data.tokenType,
            data.refreshToken
        )

        override fun getScopes(): Array<Scope> = _scopes
        override fun getExpiration(): OffsetDateTime = _expiration
        override fun getAccessToken(): String = _accessToken
        override fun getTokenType(): String = _tokenType
        override fun getRefreshToken(): String = _refreshToken

        override fun equals(other: Any?): Boolean {
            if(this === other) return true
            if(other !is Session) return false

            if(!Arrays.equals(_scopes, other._scopes)) return false
            if(_expiration != other._expiration) return false
            if(_accessToken != other._accessToken) return false
            if(_tokenType != other._tokenType) return false
            if(_refreshToken != other._refreshToken) return false

            return true
        }

        override fun hashCode(): Int {
            var result = Arrays.hashCode(_scopes)
            result = 31 * result + _expiration.hashCode()
            result = 31 * result + _accessToken.hashCode()
            result = 31 * result + _tokenType.hashCode()
            result = 31 * result + _refreshToken.hashCode()
            return result
        }
    }
}