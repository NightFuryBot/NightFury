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

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
abstract class SQLCollection<in E, out T>(val connection: Connection) {

    /*companion object
    {
        private fun insertArgs(statement: PreparedStatement, vararg args: Any) : PreparedStatement
        {
            args.forEachIndexed { index: Int, any: Any ->
                when (any) {
                    is String -> statement.setString(index + 1, any)
                    is Long -> statement.setLong(index + 1, any)
                    is Int -> statement.setInt(index + 1, any)
                    is Boolean -> statement.setBoolean(index + 1, any)
                }
            }
            return statement
        }
    }*/

    var getStatement : String = ""
    var addStatement : String = ""
    var removeStatement : String = ""
    var removeAllStatement : String = ""

    fun get(env: E, vararg args: Any) : Set<T> = try {
        connection prepare getStatement closeAfter { insert(*args) executeQuery { get(it, env) } }
    } catch (e: SQLException) { SQL.LOG.warn(e); emptySet() }
    /*fun get(env: E, vararg args: Any) : Set<T>
    {
        return try {
            insertArgs(connection.prepareStatement(getStatement), *args).use {
                val returns = it.executeQuery().use { get(it, env) }
                returns
            }
        } catch (e: SQLException) { SQL.LOG.warn(e); emptySet() }
    }*/

    abstract fun get(results: ResultSet, env: E) : Set<T>

    fun add(vararg args: Any)
    {
        try {
            connection prepare addStatement closeAfter { insert(*args).execute() }
            /*insertArgs(connection.prepareStatement(
                    addStatement, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE
            ), *args).use { it.execute() }*/
        } catch (e: SQLException) { SQL.LOG.warn(e) }
    }

    fun remove(vararg args: Any)
    {
        try {
            connection prepare removeStatement closeAfter { insert(*args).execute() }
            /*insertArgs(connection.prepareStatement(
                    removeStatement, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE
            ), *args).use { it.execute() }*/
        } catch (e: SQLException) { SQL.LOG.warn(e) }
    }

    fun removeAll(vararg args: Any)
    {
        try {
            connection prepare removeAllStatement closeAfter { insert(*args).execute() }
            /*insertArgs(connection.prepareStatement(
                    removeAllStatement, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE
            ), *args).use { it.execute() }*/
        } catch (e: SQLException) { SQL.LOG.warn(e) }
    }
}