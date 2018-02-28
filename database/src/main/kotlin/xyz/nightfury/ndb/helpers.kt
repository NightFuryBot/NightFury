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
@file:JvmName("DatabaseHelpers")
package xyz.nightfury.ndb

import org.h2.value.Value
import org.intellij.lang.annotations.Language
import java.sql.*

internal inline val LOG get() = Database.LOG

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Long?): T = apply {
    if(value === null) setNull(index, Value.LONG) else setLong(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: String?): T = apply {
    if(value == null) setNull(index, Value.STRING) else setString(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Int?): T = apply {
    if(value == null) setNull(index, Value.INT) else setInt(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Boolean?): T = apply {
    if(value == null) setNull(index, Value.BOOLEAN) else setBoolean(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Short?): T = apply {
    if(value == null) setNull(index, Value.SHORT) else setShort(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Timestamp?): T = apply {
    if(value == null) setNull(index, Value.TIMESTAMP_TZ) else setTimestamp(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Date?): T = apply {
    if(value == null) setNull(index, Value.DATE) else setDate(index, value)
}

internal inline operator fun <reified T: PreparedStatement> T.set(index: Int, value: Enum<*>?): T = apply {
    this[index] = value?.name
}

internal inline operator fun <reified T> ResultSet.get(column: String): T? {
    val any = getObject(column)
    if(wasNull()) {
        return null
    }
    return any as? T ?: throw IllegalArgumentException("Could not convert results from column " +
                                                                     "'${column.toUpperCase()}' to ${T::class}")
}

internal inline fun <reified R>
    Connection.statement(@Language("SQL") sql: String, block: PreparedStatement.() -> R): R {
    return prepareStatement(sql).use { it.block() }
}

internal inline fun Connection.execute(@Language("SQL") sql: String, block: PreparedStatement.() -> Unit) {
    prepareStatement(sql).use {
        it.block()
        it.execute()
    }
}

internal inline fun Connection.any(@Language("SQL") sql: String, block: PreparedStatement.() -> Unit): Boolean {
    return prepareStatement(sql).use {
        it.block()
        it.executeQuery().use {
            it.next()
        }
    }
}

internal inline fun <reified R> PreparedStatement.query(block: (ResultSet) -> R): R? {
    var results: ResultSet? = null
    try {
        results = executeQuery()
        if(results.next())
            return block(results)
    } finally {
        results?.close()
    }
    return null
}

internal inline fun <reified R> PreparedStatement.query(default: R, block: (ResultSet) -> R): R {
    var results: ResultSet? = null
    try {
        results = executeQuery()
        if(results.next())
            return block(results)
    } finally {
        results?.close()
    }
    return default
}

internal inline fun PreparedStatement.queryAll(block: (ResultSet) -> Unit) {
    var results: ResultSet? = null
    try {
        results = executeQuery()
        results.whileNext { block(results) }
    } finally {
        results?.close()
    }
}

inline fun <reified R: ResultSet> R.whileNext(block: () -> Unit) {
    while(this.next()) {
        block()
    }
}

inline fun <reified M: ResultSetMetaData> M.isColumnNullable(column: Int): Boolean {
    return isNullable(column) == ResultSetMetaData.columnNullable
}
