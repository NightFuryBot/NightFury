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
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import java.sql.Connection

/**
 * @author Kaidan Gustave
 */
class SQLRolePersist(private val connection: Connection)
{
    private val get = "SELECT ROLE_IDS FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?"
    private val add = "INSERT INTO ROLE_PERSIST (GUILD_ID, USER_ID, ROLE_IDS) VALUES (?, ?, ?)"
    private val remove = "DELETE FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?"
    private val removeAll = "DELETE FROM ROLE_PERSIST WHERE GUILD_ID = ?"

    fun getRolePersist(member: Member): Set<Role> {
        val set = HashSet<Role>()
        using(connection.prepareStatement(get))
        {
            this[1] = member.guild.idLong
            this[2] = member.user.idLong

            using(executeQuery())
            {
                if(next())
                    getString("ROLE_IDS").split(Regex("\\|")).mapNotNullTo(set) { member.guild.getRoleById(it) }
            }
        }
        return set
    }

    fun setRolePersist(member: Member) {
        if(getRolePersist(member).isNotEmpty())
            removeRolePersist(member)

        using(connection.prepareStatement(add))
        {
            this[1] = member.guild.idLong
            this[2] = member.user.idLong
            this[3] = member.roles.joinToString(separator = "|") { it.idLong.toString() }
            execute()
        }
    }

    fun removeRolePersist(member: Member) {
        using(connection.prepareStatement(remove))
        {
            this[1] = member.guild.idLong
            this[2] = member.user.idLong
            execute()
        }
    }

    fun removeAllRolePersist(guild: Guild) {
        using(connection.prepareStatement(removeAll))
        {
            this[1] = guild.idLong
            execute()
        }
    }
}