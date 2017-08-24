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
import net.dv8tion.jda.core.entities.TextChannel
import java.sql.Connection
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
abstract class SQLChannels(connection: Connection, type: String) : SQLCollection<Guild, TextChannel>(connection) {

    override val getStatement = "SELECT $CHANNEL_ID FROM $CHANNELS WHERE $GUILD_ID = ? AND $TYPE = '$type'"
    override val addStatement = "INSERT INTO $CHANNELS ($GUILD_ID, $CHANNEL_ID, $TYPE) VALUES (?, ?, '$type')"
    override val removeStatement = "DELETE FROM $CHANNELS WHERE $GUILD_ID = ? AND $CHANNEL_ID = ? AND $TYPE = '$type'"
    override val removeAllStatement = "DELETE FROM $CHANNELS WHERE $GUILD_ID = ? AND $TYPE = '$type'"

    override fun get(results: ResultSet, env: Guild): Set<TextChannel>
    {
        val channels = HashSet<TextChannel>()
        while (results.next())
        {
            val channel = env.getTextChannelById(results.getLong(CHANNEL_ID))
            if(channel != null)
                channels.add(channel)
        }
        return channels
    }
}

abstract class SQLChannel(connection: Connection, type: String) : SQLSingleton<Guild, TextChannel>(connection) {

    override val getStatement = "SELECT $CHANNEL_ID FROM $CHANNELS WHERE $GUILD_ID = ? AND $TYPE = '$type'"
    override val setStatement = "INSERT INTO $CHANNELS ($GUILD_ID, $CHANNEL_ID, $TYPE) VALUES (?, ?, '$type')"
    override val updateStatement = "UPDATE $CHANNELS SET $CHANNEL_ID = ? WHERE $TYPE = '$type'"
    override val resetStatement = "DELETE FROM $CHANNELS WHERE $GUILD_ID = ? AND $TYPE = '$type'"

    override fun get(results: ResultSet, env: Guild) = if(results.next())
        env.getTextChannelById(results.getLong(CHANNEL_ID))
    else null
}

class SQLIgnoredChannels(connection: Connection) : SQLChannels(connection, "ignored")
class SQLModeratorLog(connection: Connection) : SQLChannel(connection, "modlog")

private val CHANNELS = "channels"      // Table Name
private val GUILD_ID = "guild_id"      // Long
private val CHANNEL_ID = "channel_id"  // Long
private val TYPE = "type"              // varchar(20)