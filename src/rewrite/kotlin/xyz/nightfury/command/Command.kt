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
@file:Suppress("MemberVisibilityCanBePrivate", "LeakingThis", "CanBeParameter")
package xyz.nightfury.command

import net.dv8tion.jda.core.Permission
import xyz.nightfury.NightFury
import xyz.nightfury.command.Command.CooldownScope.*
import xyz.nightfury.command.standard.StandardGroup
import xyz.nightfury.util.commandArgs
import xyz.nightfury.util.db.getCommandLevel
import xyz.nightfury.util.db.isMod
import xyz.nightfury.util.ext.isAdmin
import java.util.LinkedList
import kotlin.reflect.full.findAnnotation

/**
 * @author Kaidan Gustave
 */
abstract class Command(val group: Command.Group, val parent: Command?): Comparable<Command> {
    companion object {
        const val BOT_PERM = "${NightFury.ERROR} I need the %s permission in this %s!"
        const val MISSING_ARGUMENTS = "Missing Arguments"
        const val UNEXPECTED_ERROR = "An unexpected error occurred, please try again later!"
    }

    abstract val name: String

    open val aliases = emptyArray<String>()
    open val arguments = ""
    open val help = "No help available."
    open val devOnly = group.devOnly
    open val guildOnly = group.guildOnly
    open val botPermissions = emptyArray<Permission>()
    open val cooldown = 0
    open val cooldownScope = USER
    open val children = emptyArray<Command>()
    open val hasAdjustableLevel = true

    open val fullname: String get() = "${parent?.let { "${it.fullname} " } ?: ""}$name"
    open val defaultLevel: Command.Level get() = parent?.defaultLevel ?: group.defaultLevel

    private val autoCooldown by lazy { this::class.findAnnotation<AutoCooldown>()?.mode ?: AutoCooldownMode.OFF }
    private val noArgumentError by lazy {
        val annotation = this::class.findAnnotation<MustHaveArguments>() ?: return@lazy null
        val error = annotation.error
        return@lazy "${NightFury.ERROR} ${error.replace("%name", fullname).replace("%arguments", arguments)}"
    }

    constructor(parent: Command): this(parent.group, parent)

    constructor(group: Command.Group): this(group, null) {
        group.commands += this
    }

    suspend fun run(ctx: CommandContext) {
        if(children.isNotEmpty() && ctx.args.isNotEmpty()) {
            val parts = ctx.args.split(commandArgs, 2)
            children.forEach {
                if(it.isForCommand(parts[0])) {
                    ctx.args = if(parts.size>1) parts[1] else ""
                    return it.run(ctx)
                }
            }
        }

        if(devOnly && !ctx.isDev)
            return

        if(guildOnly && !ctx.isGuild)
            return

        if(!group.check(ctx))
            return

        val level = ctx.level

        if(!level.guildOnly || ctx.isGuild) {
            if(!level.test(ctx))
                return
        }

        if(ctx.isGuild) {
            for(p in botPermissions) {
                if(p.isChannel) {
                    if(p.name.startsWith("VOICE")) {
                        val vc = ctx.member.voiceState.channel
                        if(vc === null) {
                            return ctx.terminate("${NightFury.ERROR} You must be in a voice channel to use that!")
                        } else if(!ctx.selfMember.hasPermission(vc, p)) {
                            return ctx.terminate(BOT_PERM.format(p.name, "Voice Channel"))
                        }
                    }
                } else if(!ctx.selfMember.hasPermission(ctx.textChannel, p)) {
                    return ctx.terminate(BOT_PERM.format(p.name, "Guild"))
                }
            }
        }

        val key = ctx.takeIf { cooldown > 0 }?.cooldownKey?.also { key ->
            val remaining = ctx.client.getRemainingCooldown(key)
            if(remaining > 0) {
                val scope = ctx.correctScope
                return ctx.terminate("${NightFury.WARNING} That command is on cooldown for $remaining more " +
                                     "seconds${if(scope.errSuffix.isEmpty()) "" else " ${scope.errSuffix}"}!")
            }
        }

        noArgumentError?.let { noArgumentError ->
            if(ctx.args.isEmpty()) {
                noArgumentError.takeIf { it.isNotEmpty() }?.let {
                    return ctx.terminate(
                        "${NightFury.ERROR} **$MISSING_ARGUMENTS!**\n" +
                        it.replace("%prefix", ctx.client.prefix)
                    )
                }

                return ctx.terminate(
                    "${NightFury.ERROR} **$MISSING_ARGUMENTS!**\n" +
                    "Use `${ctx.client.prefix}$fullname help` for more info on this command!"
                )
            }
        }

        key?.takeIf { autoCooldown == AutoCooldownMode.BEFORE }?.let { ctx.client.applyCooldown(key, cooldown) }

        try {
            execute(ctx)
        } catch(t: Throwable) {
            ctx.client.mode.onException(ctx, this, t)
            return ctx.replyError(UNEXPECTED_ERROR)
        }

        key?.takeIf { autoCooldown == AutoCooldownMode.AFTER }?.let { ctx.client.applyCooldown(key, cooldown) }

        ctx.client.mode.onCommandCompleted(ctx, this)
    }

    protected abstract suspend fun execute(ctx: CommandContext)

    fun isForCommand(string: String): Boolean {
        if(string.equals(name, true))
            return true
        aliases.forEach {
            if(string.equals(it, true))
                return true
        }
        return false
    }

    fun findChild(args: String): Command? {
        if(children.isEmpty() || args.isEmpty())
            return this

        val parts = args.split(commandArgs, 2)
        children.forEach {
            if(it.isForCommand(parts[0]))
                return it.findChild(if(parts.size > 1) parts[1] else "")
        }

        return null
    }

    protected inline val CommandContext.level: Level inline get() {
        return if(isGuild && hasAdjustableLevel) {
            guild.getCommandLevel(this@Command) ?: defaultLevel
        } else defaultLevel
    }

    protected fun CommandContext.invokeCooldown() = client.applyCooldown(cooldownKey, cooldown)

    protected fun CommandContext.missingArgs(block: (() -> String)? = null) {
        error(MISSING_ARGUMENTS) {
            block?.invoke() ?: "See `${client.prefix}$fullname help` for more information on this command!"
        }
    }

    protected fun CommandContext.invalidArgs(block: (() -> String)? = null) {
        error("Invalid Arguments") {
            block?.invoke() ?: "See `${client.prefix}$fullname help` for more information on this command!"
        }
    }

    private fun CommandContext.terminate(text: String) {
        client.mode.onCommandTerminated(this, this@Command, text)
        client.incrementUses(this@Command)
    }

    private inline val CommandContext.cooldownKey: String inline get() {
        return when(cooldownScope) {
            USER -> cooldownScope.genKey(name, author.idLong)
            USER_GUILD -> {
                if(isGuild) return cooldownScope.genKey(name, author.idLong, guild.idLong)
                return USER_CHANNEL.genKey(name, author.idLong, channel.idLong)
            }
            USER_CHANNEL -> cooldownScope.genKey(name, author.idLong, channel.idLong)
            GUILD -> {
                if(isGuild) return cooldownScope.genKey(name, guild.idLong)
                return CHANNEL.genKey(name, channel.idLong)
            }
            CHANNEL -> cooldownScope.genKey(name, channel.idLong)
            GLOBAL -> cooldownScope.genKey(name, 0L)
        }
    }

    private inline val CommandContext.correctScope: CooldownScope inline get() {
        if(!isGuild) {
            return when(cooldownScope) {
                USER_GUILD, GUILD -> CHANNEL
                else -> cooldownScope
            }
        }
        return cooldownScope
    }

    override fun compareTo(other: Command): Int {
        return group.compareTo(other.group).takeIf { it != 0 } ?: fullname.compareTo(other.fullname, true)
    }

    // Inner Classes

    abstract class Group(val name: String): Comparable<Group> {
        abstract val defaultLevel: Level
        abstract val guildOnly: Boolean
        abstract val devOnly: Boolean
        val commands = LinkedList<Command>()

        open fun check(ctx: CommandContext): Boolean = true

        override fun compareTo(other: Group): Int {
            if(name == StandardGroup.name) {
                return 1 // Standard will always be at the top of the list
            }
            return defaultLevel.compareTo(other.defaultLevel)
        }
    }

    enum class Level(val guildOnly: Boolean = false, val test: suspend (CommandContext) -> Boolean = { true }) {
        STANDARD(),
        MODERATOR(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isAdmin || ctx.member.isMod }),
        ADMINISTRATOR(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isAdmin }),
        SERVER_OWNER(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isOwner }),
        SHENGAERO(test = { ctx -> ctx.isDev });
    }

    enum class CooldownScope constructor(private val format: String, val errSuffix: String) {
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
}
