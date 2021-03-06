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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.nightfury

import xyz.nightfury.annotations.AutoInvokeCooldown
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLModeratorRole
import xyz.nightfury.db.SQLMusicWhitelist
import xyz.nightfury.resources.Arguments
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.db.SQLLevel
import xyz.nightfury.resources.onlyInitializingOnce
import java.io.InputStreamReader
import kotlin.reflect.full.findAnnotation

/**
 * @author Kaidan Gustave
 */
abstract class Command {
    var name: String = "null"
        protected set

    var aliases: Array<String> = emptyArray()
        protected set

    var arguments: String = ""
        protected set

    var help: String = "No help available."
        protected set

    var devOnly: Boolean = false
        protected set

    var guildOnly: Boolean = true
        protected set

    var category: Category? = null
        protected set

    var defaultLevel: CommandLevel by onlyInitializingOnce(default = CommandLevel.fromCategory(category))
        protected set

    var botPermissions: Array<Permission> = emptyArray()
        protected set

    var userPermissions: Array<Permission> = emptyArray()
        protected set

    var cooldown: Int = 0
        protected set

    var cooldownScope: CooldownScope = CooldownScope.USER_GUILD
        protected set

    var children: Array<Command> = emptyArray()
        protected set

    var fullname: String = "null"
        get() = if(field == "null") name else field
        protected set

    val docs: String? by lazy {
        this::class.findAnnotation<HasDocumentation>() ?: return@lazy null

        javaClass.getResourceAsStream(("/${category?.title?.toLowerCase()?.replace(Arguments.commandArgs, "-") ?: "standard"}" +
                                      "/${fullname.replace(Arguments.commandArgs, "-").toLowerCase()}.md"))?.use {
            InputStreamReader(it).use {
                it.readText().split(Regex("\n\n")).joinToString(separator = "\n\n") {
                    it.replace(Regex("\\s*\n\\s*"), "\n").replace("\n", " ").replace("<br> ", "\n")
                }
            }
        }
    }

    var documentation: String? = null
        get() = field ?: docs?.also { field = it }
        protected set

    private val noArgError: String? by lazy { this::class.findAnnotation<MustHaveArguments>()?.error }

    private val autoInvokeCooldown: Boolean by lazy { this::class.findAnnotation<AutoInvokeCooldown>() != null }

    companion object {
        private const val BOT_PERM = "%s I need the %s permission in this %s!"
        private const val USER_PERM = "%s You must have the %s permission in this %s to use that!"

        const val SEE_HELP = "Use `%s%s help` for more information on this command!"

        const val INVALID_ARGS_ERROR = "**Invalid Arguments!**\n%s"
        const val INVALID_ARGS_HELP = "**Invalid Arguments!**\n$SEE_HELP"
        const val TOO_FEW_ARGS_ERROR = "**Too Few Arguments!**\n%s"
        const val TOO_FEW_ARGS_HELP = "**Too Few Arguments!**\n$SEE_HELP"

        fun sendSubHelp(event: CommandEvent, command: Command) {
            val helpMessage = buildString {
                val aliases = command.aliases
                val help = command.help
                val arguments = command.arguments
                val children = command.children.filter {
                    if(event.guild == null)
                        it.category?.test(event) ?: !it.guildOnly
                    else
                        SQLLevel.getLevel(event.guild, it).test(event)
                }
                val ownerId = event.client.devId
                val serverInvite = event.client.server

                append("__Available help for **${command.name} Command** in " +
                       "${if(event.isFromType(ChannelType.PRIVATE)) "DM" else "<#" + event.channel.id + ">"}__\n")

                append("\n**Usage:** `")
                append(event.client.prefix)
                append((if(command.fullname != "null") command.fullname else command.name).toLowerCase())
                append(if(arguments.isNotEmpty()) " $arguments`" else "`")

                if(aliases.isNotEmpty()) {
                    append("\n**Alias${if(aliases.size>1) "es" else ""}:** `")
                    for(i in aliases.indices) {
                        append("${aliases[i]}`")
                        if(i != aliases.size - 1)
                            append(", `")
                    }
                }

                if(help != "No help available.")
                    append("\n**Function:** `$help`\n")

                command.docs?.let { append("\n$it\n") }

                if(children.isNotEmpty()) {
                    append("\n**Sub-Commands:**\n\n")
                    var cat: Category? = null
                    for((i, c) in children.withIndex()) {
                        if(cat != c.category) {
                            if(c.category?.test(event) == false)
                                continue

                            cat = c.category

                            if(cat != null) {
                                if(i != 0)
                                    append("\n")
                                append("__${cat.title}__\n\n")
                            }
                        }
                        append("`").append(event.client.prefix).append(c.fullname.toLowerCase())
                        append(if(c.arguments.isNotEmpty()) " ${c.arguments}" else "")
                        append("` - ").append(c.help)

                        if(i + 1 < children.size)
                            append("\n")
                    }
                }

                val owner = event.jda.getUserById(ownerId)
                if(owner != null) {
                    append("\n\nFor additional help, contact **")
                    append(owner.name)
                    append("**#")
                    append(owner.discriminator)
                    append(" or join his support server ")
                } else {
                    append("\n\nFor additional help, join my support server ")
                }
                append(serverInvite)
            }

            if(event.isFromType(ChannelType.TEXT))
                event.reactSuccess()

            event.replyInDm(helpMessage)
        }
    }

    fun run(event: CommandEvent) {
        if(children.isNotEmpty() && event.args.isNotEmpty()) {
            val parts = event.args.split(Arguments.commandArgs, 2)
            children.forEach {
                if(it.isForCommand(parts[0])) {
                    event.args = if(parts.size>1) parts[1] else ""
                    return it.run(event)
                }
            }
        }

        if(devOnly && !event.isDev)
            return

        if(event.isFromType(ChannelType.TEXT)) {
            // Level has been set differently
            if(!event.level.test(event))
                return
        }

        category?.let { if(!it.test(event)) return }

        if(event.args.startsWith("help",true))
            return sendSubHelp(event, this)

        if(guildOnly && !event.isFromType(ChannelType.TEXT))
            return event terminate "${event.client.error} This command cannot be used in Direct messages"

        if(event.channelType == ChannelType.TEXT) {
            for(p in botPermissions) {
                if(p.isChannel) {
                    if(p.name.startsWith("VOICE")) {
                        val vc = event.member.voiceState.channel
                        if(vc == null)
                            return event terminate "${event.client.error} You must be in a voice channel to use that!"
                        else if(!event.selfMember.hasPermission(vc, p))
                            return event terminate BOT_PERM.format(event.client.error, p.name, "Voice Channel")
                    }
                    else if(!event.selfMember.hasPermission(event.textChannel, p))
                        return event terminate BOT_PERM.format(event.client.error, p.name, "Channel")
                }
                else if(!event.selfMember.hasPermission(event.textChannel, p))
                    return event terminate BOT_PERM.format(event.client.error, p.name, "Guild")
            }

            for(p in userPermissions) {
                if(p.isChannel && !event.member.hasPermission(event.textChannel, p))
                    return event terminate USER_PERM.format(event.client.error, p.name, "Channel")
                else if(!event.member.hasPermission(event.textChannel, p))
                    return event terminate USER_PERM.format(event.client.error, p.name, "Guild")
            }
        }

        val key = if(cooldown > 0) event.cooldownKey else null
        if(key!=null) {
            val remaining = event.client.getRemainingCooldown(key)
            if(remaining > 0) {
                val error = event.cooldownError
                return event terminate "${event.client.warning} That command is on cooldown " +
                        "for $remaining more seconds${if (error.isEmpty()) "!" else " $error"}!"
            }
        }

        if(autoInvokeCooldown && key != null) {
            event.client.applyCooldown(key, cooldown)
        }

        if(event.args.isEmpty()) {
            val error = noArgError // Custom getters/setters and delegates don't smart cast
            if(error != null) {
                return if(error.isNotEmpty()) {
                    event.replyError(TOO_FEW_ARGS_ERROR.format(error))
                } else {
                    event.replyError(TOO_FEW_ARGS_HELP.format(event.client.prefix, fullname.toLowerCase()))
                }
            }
        }

        try {
            execute(event)
            event.client.listener.onCommandCompleted(event, this)
        } catch (e: Throwable) {
            if(e is NotImplementedError) {
                event.replyError("The command requested has not been implemented yet!")
            } else {
                event.client.listener.onException(event, this, e)
            }
        }

        event.client.incrementUses(this)
    }

    protected abstract fun execute(event: CommandEvent)

    fun isForCommand(string: String): Boolean {
        if(string.equals(name, true))
            return true
        else if(aliases.isNotEmpty())
            aliases.forEach { alias -> if(string.equals(alias, true)) return true }
        return false
    }

    fun CommandEvent.modSearch(): Pair<Long, String?>? {
        val targetId = Arguments.targetIDWithReason.matchEntire(args)
        val targetMention = Arguments.targetMentionWithReason.matchEntire(args)

        val groups = when {
            targetId != null -> targetId.groupValues
            targetMention != null -> targetMention.groupValues
            else -> {
                replyError(INVALID_ARGS_HELP.format(client.prefix, name))
                return null
            }
        }

        return groups[1].trim().toLong() to groups[2].trim().takeIf { it.isNotEmpty() }
    }

    fun CommandEvent.invokeCooldown() = client.applyCooldown(cooldownKey, cooldown)

    val CommandEvent.level: CommandLevel
        get() = SQLLevel.getLevel(guild, this@Command)

    private val CommandEvent.cooldownKey: String
        get() = when(cooldownScope) {
            CooldownScope.USER -> cooldownScope.genKey(name, author.idLong)
            CooldownScope.USER_GUILD ->
                if(event.isFromType(ChannelType.TEXT))
                    cooldownScope.genKey(name, author.idLong, guild.idLong)
                else CooldownScope.USER_CHANNEL.genKey(name, author.idLong, channel.idLong)
            CooldownScope.USER_CHANNEL -> cooldownScope.genKey(name, author.idLong, channel.idLong)
            CooldownScope.GUILD ->
                if(event.isFromType(ChannelType.TEXT))
                    cooldownScope.genKey(name, guild.idLong)
                else CooldownScope.CHANNEL.genKey(name, channel.idLong)
            CooldownScope.CHANNEL -> cooldownScope.genKey(name, channel.idLong)
            CooldownScope.GLOBAL -> cooldownScope.genKey(name, 0L)
        }

    private val CommandEvent.cooldownError: String
        get() {
            if(guild == null) {
                if(cooldownScope == CooldownScope.USER_GUILD || cooldownScope == CooldownScope.GUILD) {
                    return CooldownScope.CHANNEL.errSuffix
                }
            }

            return cooldownScope.errSuffix
        }

    private infix fun CommandEvent.terminate(msg: String) {
        client.listener.onCommandTerminated(this, this@Command, msg)
        client.incrementUses(this@Command)
    }

    fun findChild(args: String): Command? {
        if(children.isEmpty() || args.isEmpty())
            return this

        val parts = args.split(Arguments.commandArgs, 2)
        children.forEach {
            if(it.isForCommand(parts[0]))
                return it.findChild(if(parts.size>1) parts[1] else "")
        }

        return null
    }
}

enum class CommandLevel(private val predicate: (CommandEvent) -> Boolean) {
    STANDARD({ true }),
    MODERATOR({ ADMIN test it || SQLModeratorRole.getRole(it.guild).run { this != null && it.member.roles.contains(this) } }),
    ADMIN({ SERVER_OWNER test it || it.member.hasPermission(Permission.ADMINISTRATOR) }),
    SERVER_OWNER({ SHENGAERO test it || it.member.isOwner }),
    SHENGAERO({ it.isDev });

    infix fun test(event: CommandEvent) = predicate.invoke(event)

    companion object {
        fun fromCategory(category: Category?) = when(category) {
            null -> STANDARD
            Category.SHENGAERO -> SHENGAERO
            Category.SERVER_OWNER -> SERVER_OWNER
            Category.ADMIN -> ADMIN
            Category.MODERATOR -> MODERATOR
            else -> STANDARD
        }

        fun fromArguments(args: String) = when(args.toLowerCase()) {
            "owner" -> SERVER_OWNER
            "admin" -> ADMIN
            "mod", "mods", "moderator" -> MODERATOR
            "all", "standard", "public" -> STANDARD
            else -> null
        }
    }
}

enum class Category(val title: String,
                    private val predicate: (CommandEvent) -> Boolean) {
    // Primary Hierarchy
    SHENGAERO("Developer", { it.isDev }),

    SERVER_OWNER("Server Owner", { SHENGAERO.test(it) || (it.isFromType(ChannelType.TEXT) && it.member.isOwner) }),

    ADMIN("Administrator", { SERVER_OWNER.test(it) || (it.isFromType(ChannelType.TEXT) &&
                                                       it.member.hasPermission(Permission.ADMINISTRATOR)) }),

    MODERATOR("Moderator", { ADMIN.test(it) || (it.isFromType(ChannelType.TEXT) &&
                                                SQLModeratorRole.getRole(it.guild).run { this != null && it.member.roles.contains(this) })
    }),

    // Other Categories

    MUSIC("Music", { SHENGAERO test it || (it.isFromType(ChannelType.TEXT) && SQLMusicWhitelist.isGuild(it.guild)) }),
    NSFW("NSFW", { SHENGAERO test it || (it.isFromType(ChannelType.TEXT) && it.textChannel.isNSFW) });

    infix fun test(event: CommandEvent) = predicate.invoke(event)
}

enum class CooldownScope constructor(private val format: String, internal val errSuffix: String) {
    /** `U:(UserID)` */
    USER("U:%d", ""),
    /** `C:(ChannelID)` */
    CHANNEL("C:%d", "in this channel"),
    /** `U:(UserID)|C:(ChannelID)` */
    USER_CHANNEL("U:%d|C:%d", "in this channel"),
    /**
     * `G:(GuildID)`
     *
     * Defaults to [CHANNEL] in DM's
     */
    GUILD("G:%d", "in this server"),
    /**
     * `U:(UserID)|C:(GuildID)`
     *
     * Defaults to [USER_CHANNEL] in DM's
     */
    USER_GUILD("U:%d|G:%d", "in this server"),
    /** `globally` */
    GLOBAL("Global", "globally");

    internal fun genKey(name: String, id: Long) = genKey(name, id, -1)

    internal fun genKey(name: String, idOne: Long, idTwo: Long) = "$name|${when {
        this == GLOBAL -> format
        idTwo == -1L   -> format.format(idOne)
        else           -> format.format(idOne, idTwo)
    }}"
}

abstract class NoBaseExecutionCommand : Command() {
    override fun execute(event: CommandEvent) {
        if(event.args.isEmpty())
            event.replyError(TOO_FEW_ARGS_HELP.format(event.client.prefix, this.name))
        else
            event.replyError(INVALID_ARGS_HELP.format(event.client.prefix, this.name))
    }
}

