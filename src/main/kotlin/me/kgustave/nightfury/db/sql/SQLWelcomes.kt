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

import net.dv8tion.jda.core.entities.Guild
import java.sql.Connection
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
class SQLWelcomeMessage(connection: Connection) : SQLSingleton<Guild, String>(connection)
{
    init
    {
        this.getStatement = "SELECT welcome FROM welcomes WHERE guild_id = ?"
        this.setStatement = "INSERT INTO welcomes (guild_id, welcome) VALUES (?, ?)"
        this.updateStatement = "UPDATE welcomes SET welcome = ? WHERE guild_id = ?"
        this.resetStatement = "REMOVE FROM welcomes WHERE guild_id = ?"
    }

    override fun get(results: ResultSet, env: Guild): String? = if(results.next()) results.getString("welcome") else null
}

class SQLWelcomeChannel(connection: Connection) : SQLChannel(connection, "welcomes")