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
@Suppress("RedundantUnitReturnType")
abstract class SQLSingleton<in E, out T>(val connection: Connection)
{
    abstract val getStatement : String
    abstract val setStatement : String
    abstract val updateStatement : String
    abstract val resetStatement : String

    abstract fun get(results: ResultSet, env: E) : T?

    fun has(vararg  args : Any) = try {
        connection prepare getStatement closeAfter { insert(*args) executeQuery { it.next() } }
    } catch (e: SQLException) { SQL.LOG.warn(e); false }

    fun get(env: E, vararg args: Any) = try {
        connection prepare getStatement closeAfter { insert(*args) executeQuery { get(it, env) } }
    } catch (e: SQLException) { SQL.LOG.warn(e); null }

    fun set(vararg args: Any) : Unit = try {
        connection prepare setStatement closeAfter { insert(*args).execute() }
    } catch (e: SQLException) { SQL.LOG.warn(e) }

    fun update(vararg args: Any) : Unit = try {
        connection prepare updateStatement closeAfter { insert(*args).execute() }
    } catch (e: SQLException) { SQL.LOG.warn(e) }

    fun reset(vararg args: Any) : Unit = try {
        connection prepare resetStatement closeAfter { insert(*args).execute() }
    } catch (e: SQLException) { SQL.LOG.warn(e) }
}