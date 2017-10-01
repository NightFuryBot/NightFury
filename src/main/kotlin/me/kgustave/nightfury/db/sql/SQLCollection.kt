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
abstract class SQLCollection<in E, out T>(val connection: Connection)
{
    abstract val getStatement : String
    abstract val addStatement : String
    abstract val removeStatement : String
    abstract val removeAllStatement : String

    abstract fun get(results: ResultSet, env: E) : Set<T>

    fun get(env: E, vararg args: Any) : Set<T> = try {
        connection prepare getStatement closeAfter { insert(*args) executeQuery { get(it, env) } }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e); emptySet() }

    fun add(vararg args: Any) : Unit = try {
        connection prepare addStatement closeAfter { insert(*args).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }

    fun remove(vararg args: Any) : Unit  = try {
        connection prepare removeStatement closeAfter { insert(*args).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }

    fun removeAll(vararg args: Any) : Unit = try {
        connection prepare removeAllStatement closeAfter { insert(*args).execute() }
    } catch (e: SQLException) { SQL.LOG.warn("SQLException",e) }
}