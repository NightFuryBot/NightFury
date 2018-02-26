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
package xyz.nightfury.ndb

import xyz.nightfury.ndb.entities.Case
import xyz.nightfury.ndb.internal.*

/**
 * @author Kaidan Gustave
 */
object CasesHandler: Database.Table() {
    private const val GET_CASES =        "SELECT * FROM CASES WHERE GUILD_ID = ? ORDER BY NUMBER"
    private const val GET_CASE_BY_NUM =  "SELECT * FROM CASES WHERE GUILD_ID = ? AND NUMBER = ?"
    private const val GET_CASES_MOD_ID = "SELECT * FROM CASES WHERE GUILD_ID = ? AND NUMBER = ? ORDER BY NUMBER"
    private const val ADD_CASE =         "INSERT INTO CASES (NUMBER, GUILD_ID, MESSAGE_ID, MOD_ID, TARGET_ID, IS_ON_USER, ACTION, REASON) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    private const val UPDATE_REASON =    "UPDATE CASES SET REASON = ? WHERE GUILD_ID = ? AND NUMBER = ?"

    fun getCases(guildId: Long): List<Case> = sql({ emptyList() }) {
        val cases = ArrayList<Case>()
        statement(GET_CASES) {
            this[1] = guildId
            queryAll { cases += Case.from(it) }
        }
        return cases
    }

    fun getCaseByNumber(guildId: Long, number: Int): Case? = sql {
        statement(GET_CASE_BY_NUM) {
            this[1] = guildId
            this[2] = number
            query { Case.from(it) }
        }
    }

    fun getCasesByModId(guildId: Long, userId: Long): List<Case> = sql({ emptyList() }) {
        val cases = ArrayList<Case>()
        statement(GET_CASES_MOD_ID) {
            this[1] = guildId
            this[2] = userId
            queryAll { cases += Case.from(it) }
        }
        return cases
    }

    fun addCase(case: Case) = sql {
        execute(ADD_CASE) {
            this[1] = case.number
            this[2] = case.guildId
            this[3] = case.messageId
            this[4] = case.modId
            this[5] = case.targetId
            this[6] = case.isOnUser
            this[7] = case.action
            this[8] = case.reason
        }
    }

    fun updateReason(case: Case) = sql {
        execute(UPDATE_REASON) {
            this[1] = case.reason
            this[2] = case.guildId
            this[3] = case.number
        }
    }
}