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
@file:Suppress("HasPlatformType")
package xyz.nightfury

import net.dv8tion.jda.core.Permission
import ninja.leaping.configurate.hocon.HoconConfigurationLoader

private typealias ConfigNode = ninja.leaping.configurate.commented.CommentedConfigurationNode

/**
 * @author Kaidan Gustave
 */
class BotConfig {

    val conf: ConfigNode = hocon {
        setSource { this@BotConfig::class.java.getResourceAsStream("/bot.conf").bufferedReader(Charsets.UTF_8) }
        parseOptions.allowMissing = false
        renderOptions.comments = true
    }.load()

    private val bot: ConfigNode = conf.getNode("bot")
    private val database: ConfigNode = conf.getNode("database")
    private val databaseOptions: ConfigNode = database.getNode("options")

    val prefix: String = bot.getNode("prefix").takeUnless { it.isVirtual }?.string ?: "|"
    val devId: Long = bot.getNode("devID").takeUnless { it.isVirtual }?.long
                      ?: fail("Developer ID Node (bot.devID) was not detected!")

    val token: String = bot.getNode("token").takeUnless { it.isVirtual }?.getList { it as String }?.joinToString(".")
                        ?: fail("Token Node (bot.token) was not detected!")

    val dbotsKey: String? = bot.getNode("keys", "dbots").takeUnless { it.isVirtual }
        ?.getList { it as String }?.joinToString(".")
    val dbotslistKey: String? = bot.getNode("keys", "dbotslist").takeUnless { it.isVirtual }
        ?.getList { it as String }?.joinToString(".")
    val ytKey: String? = bot.getNode("keys", "youtube").takeUnless { it.isVirtual }?.string

    val databaseURL: String = run {
        if(database.getNode("url").isVirtual)
            throw ConfigException("Database URL node (database.url) was not detected!")

        val base = "${database.getNode("url", "prefix").string}${database.getNode("url", "path").string}"

        buildString {
            append(base)
            if(databaseOptions.isVirtual)
                return@buildString
            databaseOptions.childrenMap.forEach { key, value ->
                append(";")
                append(key)
                append("=")
                append(value.value.toString())
            }
        }
    }

    val databaseUser: String = database.getNode("login", "user")?.takeUnless { it.isVirtual }?.string
                               ?: fail("Database User Login Node (database.login.user) was not detected!")
    val databasePass: String = database.getNode("login", "pass")?.takeUnless { it.isVirtual }?.string
                               ?: fail("Database Password Login Node (database.login.pass) was not detected!")

    // Constants

    val success: String = "\uD83D\uDC32"
    val warning: String = "\uD83D\uDC22"
    val error: String = "\uD83D\uDD25"

    val server: String = "https://discord.gg/xkkw54u"

    val permissions: Array<Permission> = arrayOf(
        Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ATTACH_FILES,
        Permission.MESSAGE_ADD_REACTION,

        Permission.MANAGE_PERMISSIONS,
        Permission.MANAGE_ROLES,
        Permission.MANAGE_CHANNEL,
        Permission.NICKNAME_MANAGE,
        Permission.MESSAGE_MANAGE,

        Permission.KICK_MEMBERS,
        Permission.BAN_MEMBERS,

        Permission.VIEW_AUDIT_LOGS
    )

    private inline fun hocon(block: HoconConfigurationLoader.Builder.() -> Unit): HoconConfigurationLoader
        = HoconConfigurationLoader.builder().also(block).build()

    private inline fun <reified T> fail(message: String): T {
        throw ConfigException(message)
    }
}

class ConfigException(override val message: String, override val cause: Throwable? = null) : RuntimeException()