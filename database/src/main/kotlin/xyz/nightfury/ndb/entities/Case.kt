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
package xyz.nightfury.ndb.entities

import xyz.nightfury.ndb.CasesHandler
import xyz.nightfury.ndb.get
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
data class Case(
    val number: Int,
    val guildId: Long,
    val messageId: Long,
    val modId: Long,
    val targetId: Long,
    val isOnUser: Boolean,
    val action: Case.Action,
    private var _reason: String? = null
) {
    var reason: String?
        get() = _reason
        set(value) {
            _reason = value
            CasesHandler.updateReason(this)
        }

    companion object {
        fun from(result: ResultSet): Case = Case(
            number = result["NUMBER"]!!,
            guildId = result["GUILD_ID"]!!,
            messageId = result["MESSAGE_ID"]!!,
            modId = result["MOD_ID"]!!,
            targetId = result["TARGET_ID"]!!,
            isOnUser = result["IS_ON_USER"]!!,
            action = Action.valueOf(result.getString("ACTION").toUpperCase()),
            _reason = result["REASON"]
        )
    }

    enum class Action {
        BAN,
        UNBAN,
        KICK,
        MUTE,
        UNMUTE,
        CLEAN,
        OTHER;
    }
}