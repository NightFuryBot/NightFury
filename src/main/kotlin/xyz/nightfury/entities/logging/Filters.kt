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
package xyz.nightfury.entities.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply.*

class NormalFilter: Filter<ILoggingEvent>() {
    companion object {
        var level: LogLevel = LogLevel.INFO
    }

    override fun decide(event: ILoggingEvent) = if(level.covers(event.level.logLevel)) ACCEPT else DENY
}

val Level.logLevel: LogLevel
    get() = LogLevel.byLevel(this)

enum class LogLevel(val int: Int, val logbackLevel: Level) {
    ALL(0, Level.ALL),
    TRACE(1, Level.TRACE),
    DEBUG(2, Level.DEBUG),
    INFO(3, Level.INFO),
    WARN(4, Level.WARN),
    ERROR(5, Level.ERROR),
    OFF(6, Level.OFF);

    companion object {
        fun byLevel(level: Level): LogLevel = values().first { it.logbackLevel.levelInt == level.levelInt }
    }

    fun covers(other: LogLevel) = int <= other.int
}