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

import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * @author Kaidan Gustave
 */
object Database : Closeable {
    @Suppress("ObjectPropertyName")
    private lateinit var _connection: Connection

    internal val connection: Connection
        get() {
            if(!Database::_connection.isInitialized)
                throw UninitializedPropertyAccessException("Connection has not been opened yet!")
            return _connection
        }

    fun connect(url: String, user: String, pass: String) {
        Class.forName("org.h2.Driver").getConstructor().newInstance()

        _connection = DriverManager.getConnection(url, user, pass)

        for(data in TableData.values()) {
            SQL.LOG.debug("Checking for $data")
            if(connection.hasTableNamed(data.name))
                continue
            try {
                // Automatically builds a statement out of parameter data
                val statement = "CREATE TABLE ${data.name} ${data.parameters.joinToString(",", "(", ")")}"
                using(connection.prepareStatement(statement)) { execute() }
                SQL.LOG.info("Created ${data.name} Table!")
            } catch(e: SQLException) {
                connection.close()
                throw SQLException("Failed to set up table! $data", e)
            }
        }
    }

    override fun close() {
        SQL.LOG.info("Closing JDBC Connection...")
        connection.close()
    }

    private fun Connection.hasTableNamed(name: String): Boolean {
        return metaData.getTables(null, null, name, null).use { it.next() }
    }

    @[Suppress("Unused") Throws(SQLException::class)]
    fun evaluate(string: String) = using(connection.prepareStatement(string), false) { execute() }

    enum class TableData(vararg val parameters: String) {
        CASES("NUMBER INT",     "GUILD_ID BIGINT",      "MESSAGE_ID BIGINT",    "MOD_ID BIGINT",
              "TARGET_ID BIGINT", "IS_ON_USER BOOLEAN", "ACTION VARCHAR(200)", "REASON VARCHAR(200)"),

        CHANNELS("GUILD_ID BIGINT", "CHANNEL_ID BIGINT", "TYPE VARCHAR(200)"),

        PREFIXES("GUILD_ID BIGINT", "PREFIX VARCHAR(200)"),

        ROLES("GUILD_ID BIGINT", "ROLE_ID BIGINT", "TYPE VARCHAR(20)"),

        ROLE_PERSIST("GUILD_ID BIGINT", "USER_ID BIGINT", "ROLE_IDS VARCHAR(2000)"),

        GLOBAL_TAGS("NAME VARCHAR(50)", "OWNER_ID BIGINT", "CONTENT VARCHAR(1900)"),

        LOCAL_TAGS("NAME VARCHAR(50)", "GUILD_ID BIGINT", "OWNER_ID BIGINT", "CONTENT VARCHAR(1900)"),

        CUSTOM_COMMANDS("NAME VARCHAR(50)", "CONTENT VARCHAR(1900)", "GUILD_ID BIGINT"),

        WELCOMES("GUILD_ID BIGINT", "CHANNEL_ID BIGINT", "MESSAGE VARCHAR(1900)"),

        COMMAND_LIMITS("GUILD_ID BIGINT", "COMMAND_NAME VARCHAR(100)", "LIMIT_NUMBER INT"),

        ENABLES("GUILD_ID BIGINT", "ENABLE_TYPE VARCHAR(40)", "STATUS BOOLEAN"),

        GUILDS("GUILD_ID BIGINT", "TYPE VARCHAR(20)"),

        MUSIC_SETTINGS("GUILD_ID BIGINT", "VOICE_CHANNEL_ID BIGINT", "TEXT_CHANNEL_ID BIGINT", "NP_IN_TOPIC BOOLEAN"),

        COMMAND_LEVELS("GUILD_ID BIGINT", "COMMAND VARCHAR(200)", "LEVEL VARCHAR(50)"),

        PROFILES("USER_ID BIGINT",
                 "TIME_ZONE_ID VARCHAR(70) DEFAULT NULL",
                 "GITHUB VARCHAR(100) DEFAULT NULL",
                 "WEBSITE VARCHAR(100) DEFAULT NULL",
                 "TWITCH VARCHAR(100) DEFAULT NULL",
                 "TITLE VARCHAR(50) DEFAULT NULL",
                 "ABOUT VARCHAR(1800) DEFAULT NULL",
                 "BIRTHDAY DATE DEFAULT NULL"),

        STARBOARD_ENTRIES("STARRED_ID BIGINT", "ENTRY_ID BIGINT DEFAULT NULL", "STARBOARD_ID BIGINT", "GUILD_ID BIGINT", "USER_ID BIGINT"),

        STARBOARD_SETTINGS("THRESHOLD INT DEFAULT 5", "MAX_AGE INT DEFAULT 72", "GUILD_ID BIGINT");

        override fun toString(): String = "TABLE: $name ${parameters.joinToString(prefix = "(", postfix = ")", limit = 4)}"
    }
}
