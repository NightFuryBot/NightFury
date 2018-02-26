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
package xyz.nightfury.ndb

import xyz.nightfury.util.createLogger
import xyz.nightfury.util.requireState
import java.sql.*

/**
 * @author Kaidan Gustave
 */
object Database : AutoCloseable {
    private lateinit var _connection: Connection
    private val connection: Connection get() {
        requireState(::_connection.isInitialized) { "Connection has not been opened yet!" }
        return _connection
    }

    val LOG = createLogger(Database::class)

    fun connect(url: String, user: String, pass: String) {
        Class.forName("org.h2.Driver").getConstructor().newInstance()
        _connection = DriverManager.getConnection(url, user, pass)
        setup()
    }

    @Suppress("LoopToCallChain")
    private fun setup() {
        for(table in TableData.values()) {
            if(connection.hasTableNamed(table.name)) continue
            val statement = buildString {
                append("CREATE TABLE ${table.name}(")
                var first = true
                for(column in table.columns) {
                    if(!first) {
                        append(", ")
                    } else {
                        first = false
                    }
                    append(column.toString())
                }
                append(")")
            }

            connection.prepareStatement(statement).use {
                it.execute()
            }
        }
    }

    private inline fun <reified C: Connection> C.hasTableNamed(name: String): Boolean {
        return metaData.getTables(null, null, name, null).use { it.next() }
    }

    override fun close() {
        LOG.info("Closing JDBC Connection...")
        connection.close()
    }

    abstract class Table {
        protected val connection: Connection by lazy { Database.connection }

        protected inline fun <reified R> sql(block: Connection.() -> R): R? {
            try {
                return connection.block()
            } catch(e: SQLException) {
                Database.LOG.error("Encountered an SQLException", e)
            } catch(t: Throwable) {
                Database.LOG.error("Encountered an unhandled exception", t)
            }

            return null
        }

        protected inline fun <reified R> sql(default: () -> R, block: Connection.() -> R): R {
            return sql(block) ?: default()
        }

        protected inline fun <reified R> sql(default: R, block: Connection.() -> R): R {
            return sql(block) ?: default
        }
    }

    enum class TableData(vararg val columns: ColumnData) {
        GUILD_SETTINGS(
            ColumnData("GUILD_ID", "BIGINT"),
            ColumnData("ROLE_ME_LIMIT", "INT", nullable = true).withDefault("NULL")
        ),
        GUILD_ROLES(
            ColumnData("GUILD_ID", "BIGINT"),
            ColumnData("ROLE_ID", "BIGINT"),
            ColumnData("TYPE", "VARCHAR(200)")
        ),
        GUILD_CHANNELS(
            ColumnData("GUILD_ID", "BIGINT"),
            ColumnData("CHANNEL_ID", "BIGINT"),
            ColumnData("TYPE", "VARCHAR(200)")
        )
    }
}