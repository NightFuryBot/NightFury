/*
 * Copyright 2017 Kaidan Gustave
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
@file:Suppress("unused")
package xyz.nightfury.extensions

import net.dv8tion.jda.core.OnlineStatus
import sun.util.calendar.ZoneInfo
import java.sql.Date
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.*

val OnlineStatus.emoteId : Long
    get() = when(this) {
        OnlineStatus.ONLINE -> 313956277808005120L
        OnlineStatus.IDLE -> 313956277220802560L
        OnlineStatus.DO_NOT_DISTURB -> 313956276893646850L
        OnlineStatus.OFFLINE -> 313956277237710868L
        OnlineStatus.INVISIBLE -> 313956277107556352L
        OnlineStatus.UNKNOWN -> 313956277107556352L
    }

infix fun Int.randomNextInt(int: Int): Int {
    require( this >=0 ) { "Cannot use negative numbers as receiver in random range!" }
    require(this < int) {         "Parameter must be greater than receiver!"         }

    return this + (Math.random() * (int - this + 1)).toInt()
}

// Good for checking if lateinit vars are initialized
// And who said that checking if a non-null is null was useless?
@Suppress("SENSELESS_COMPARISON", "UNUSED")
inline fun <reified T: Any> checkInitialized(any: T)
    = try { any != null } catch (e: UninitializedPropertyAccessException) { false }

fun OffsetDateTime.toTimestamp(): Timestamp = Timestamp.from(toInstant())
fun OffsetDateTime.toDate(): Date = Date.valueOf(toLocalDate())
fun Timestamp.toOffsetDateTime(offset: ZoneOffset = ZoneOffset.UTC): OffsetDateTime = toLocalDateTime().atOffset(offset)
fun Date.toOffsetDateTime(): OffsetDateTime = toLocalDate().atTime(OffsetTime.MIN)

// We don't use ZoneInfo.getTimeZone(String) directly
// for native nullability issues.
fun timeZone(identifier: String?): TimeZone? = ZoneInfo.getTimeZone(identifier)

fun thread(start: Boolean = true,
           isDaemon: Boolean = false,
           contextClassLoader: ClassLoader? = null,
           name: String? = null,
           priority: Int = -1,
           runnable: Runnable): Thread {

    val thread = Thread(runnable)

    if(isDaemon)
        thread.isDaemon = true
    if(priority > 0)
        thread.priority = priority
    if(name != null)
        thread.name = name
    if(contextClassLoader != null)
        thread.contextClassLoader = contextClassLoader
    if(start)
        thread.start()
    return thread
}