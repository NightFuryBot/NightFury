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
package xyz.nightfury.db

import org.h2.value.Value
import xyz.nightfury.util.createLogger
import java.sql.*

object SQL {
    val LOG = createLogger(SQL::class)
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Long?): T {
    if(value ==  null)
        setNull(index, Value.LONG)
    else
        setLong(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: String?): T {
    if(value == null)
        setNull(index, Value.STRING)
    else
        setString(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Int?): T {
    if(value == null)
        setNull(index, Value.INT)
    else
        setInt(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Boolean?): T {
    if(value == null)
        setNull(index, Value.BOOLEAN)
    else
        setBoolean(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Short?): T {
    if(value == null)
        setNull(index, Value.SHORT)
    else
        setShort(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Enum<*>?): T {
    setString(index, value?.name)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Timestamp?): T {
    if(value == null)
        setNull(index, Value.TIMESTAMP_TZ)
    else
        setTimestamp(index, value)
    return this
}

inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Date?): T {
    if(value == null)
        setNull(index, Value.DATE)
    else
        setDate(index, value)
    return this
}

inline operator fun <reified R> ResultSet.get(columnName: String): R? = (getObject(columnName) ?: null) as? R

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
): R = using(closeable, onError, block) ?: default
