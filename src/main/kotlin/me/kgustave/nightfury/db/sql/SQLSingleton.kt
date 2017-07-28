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
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
abstract class SQLSingleton<in E, out T>(val connection: Connection) {

    companion object
    {
        private fun insertArgs(statement: PreparedStatement, vararg args: Any) : PreparedStatement
        {
            args.forEachIndexed { index, any ->
                if(any is String)
                    statement.setString(index+1, any)
                else if(any is Long)
                    statement.setLong(index+1, any)
                else if(any is Int)
                    statement.setInt(index+1, any)
                else if(any is Boolean)
                    statement.setBoolean(index+1, any)
            }
            return statement
        }
    }

    var getStatement : String = ""
    var setStatement : String = ""
    var updateStatement : String = ""
    var resetStatement : String = ""

    fun get(env: E, vararg args: Any) : T?
    {
        return try {
            val statement = insertArgs(connection.prepareStatement(getStatement), *args)
            val returns = statement.executeQuery().use { results -> get(results, env) }
            statement.close()
            returns
        } catch (e: SQLException) { SQL.LOG.warn(e); null }
    }

    abstract fun get(results: ResultSet, env: E) : T?

    fun set(vararg args: Any)
    {
        try {
            val statement = insertArgs(connection.prepareStatement(setStatement,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE), *args)
            statement.execute()
            statement.close()
        } catch (e: SQLException) { SQL.LOG.warn(e) }
    }

    fun update(vararg args: Any)
    {
        try {
            val statement = insertArgs(connection.prepareStatement(updateStatement,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE), *args)
            statement.execute()
            statement.close()
        } catch (e: SQLException) { SQL.LOG.warn(e) }
    }

    fun reset(vararg args: Any)
    {
        try {
            val statement = insertArgs(connection.prepareStatement(resetStatement,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE), *args)
            statement.execute()
            statement.close()
        } catch (e: SQLException) { SQL.LOG.warn(e) }
    }
}