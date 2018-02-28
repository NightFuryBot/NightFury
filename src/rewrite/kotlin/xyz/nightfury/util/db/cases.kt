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
@file:Suppress("Unused")
package xyz.nightfury.util.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import xyz.nightfury.ndb.CasesHandler
import xyz.nightfury.ndb.entities.Case

inline val <reified G: Guild> G.lastCaseNumber: Int inline get() {
    return CasesHandler.getLastCaseNumber(idLong)
}

inline val <reified G: Guild> G.cases: List<Case> inline get() {
    return CasesHandler.getCases(idLong)
}

inline val <reified M: Member> M.cases: List<Case> inline get() {
    return CasesHandler.getCasesByModId(guild.idLong, user.idLong)
}

inline val <reified M: Member> M.casesWithoutReason: List<Case> inline get() {
    return CasesHandler.getCasesWithoutReasonByModId(guild.idLong, user.idLong)
}

inline fun <reified G: Guild> G.addCase(case: Case) {
    CasesHandler.addCase(case)
}

inline fun <reified G: Guild> G.getCase(number: Int): Case? {
    return CasesHandler.getCaseByNumber(idLong, number)
}
