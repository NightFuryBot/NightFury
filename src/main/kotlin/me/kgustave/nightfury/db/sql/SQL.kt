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
package me.kgustave.nightfury.db.sql

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
object SQL {
    val LOG : Logger = LoggerFactory.getLogger("SQL")
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Long): T {
    setLong(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: String): T {
    setString(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Int): T {
    setInt(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Boolean): T {
    setBoolean(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Short): T {
    setShort(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Enum<*>): T {
    setString(index, value.name)
    return this
}

inline fun <reified T : AutoCloseable, R> using(
        closeable: T,
        onError: String = "An SQLException was thrown",
        block: T.() -> R
): R? = try {
    closeable.use(block)
} catch (e: SQLException) {
    SQL.LOG.error(onError, e)
    null
}

inline fun <reified T: AutoCloseable, R> using(
        closeable: T,
        default: R,
        onError: String = "An SQLException was thrown",
        block: T.() -> R?
): R = using(closeable, onError, block)?:default

