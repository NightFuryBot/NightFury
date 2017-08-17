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
package me.kgustave.nightfury

import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.annotations.MustHaveArguments
import me.kgustave.nightfury.extensions.ArgumentPatterns
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import java.util.*
import java.util.function.BiConsumer

/**
 * @author Kaidan Gustave
 */
abstract class Command
{
    var name: String = "null"
        protected set(value) {field = value}

    var aliases: Array<String> = emptyArray()
        protected set(value) {field = value}

    var arguments: String = ""
        protected set(value) {field = value}

    var help: String = "no help available"
        protected set(value) {field = value}

    var devOnly: Boolean = false
        protected set(value) {field = value}

    var guildOnly: Boolean = true
        protected set(value) {field = value}

    var category: Category? = null
        protected set(value) {field = value}

    var helpBiConsumer: BiConsumer<CommandEvent, Command> = defaultSubHelp
        protected set(value) {field = value}

    var botPermissions: Array<Permission> = emptyArray()
        protected set(value) {field = value}

    var userPermissions: Array<Permission> = emptyArray()
        protected set(value) {field = value}

    var cooldown: Int = 0
        protected set(value) {field = value}

    var cooldownScope: CooldownScope = CooldownScope.USER_GUILD
        protected set(value) {field = value}

    var children: Array<Command> = emptyArray()
        protected set(value) {field = value}

    var fullname: String = "null"
        protected set(value) {field = value}

    companion object
    {
        private val BOT_PERM = "%s I need the %s permission in this %s!"
        private val USER_PERM = "%s You must have the %s permission in this %s to use that!"

        val SEE_HELP = "Use `%s%s help` for more information on this command!"

        val INVALID_ARGS_ERROR = "**Invalid Arguments!**\n%s"
        val INVALID_ARGS_HELP = "**Invalid Arguments!**\n$SEE_HELP"
        val TOO_FEW_ARGS_ERROR = "**Too Few Arguments!**\n%s"
        val TOO_FEW_ARGS_HELP = "**Too Few Arguments!**\n$SEE_HELP"

        private val defaultSubHelp = BiConsumer<CommandEvent, Command> {_,_ ->}
        fun standardSubHelp(explanation: String?, helpInDM : Boolean) : BiConsumer<CommandEvent, Command>
        {
            return BiConsumer {event, command ->
                val b = StringBuilder()
                val aliases = command.aliases
                val help = command.help
                val arguments = command.arguments
                val children = command.children
                val ownerId = event.client.devId
                val serverInvite = event.client.server
                b.append("Available help for **${command.name} command** in " +
                        "${if(event.isFromType(ChannelType.PRIVATE)) "DM" else "<#" + event.channel.id + ">"}\n")

                b.append("\n**Usage:** `")
                        .append(event.client.prefix)
                        .append((if(command.fullname!="null") command.fullname else command.name).toLowerCase())
                        .append(if(arguments.isNotEmpty()) " $arguments`" else "`")
                        .append("\n")

                if(aliases.isNotEmpty())
                {
                    b.append("\n**Alias${if(aliases.size>1) "es" else ""}:** `")
                    for(i in aliases.indices) {
                        b.append("${aliases[i]}`")
                        if(i != aliases.size - 1)
                            b.append(", `")
                    }
                    b.append("\n")
                }

                if(help != "no help available")
                    b.append("\n$help\n")
                if(explanation != null)
                    b.append("\n$explanation\n")

                if(children.isNotEmpty())
                {
                    b.append("\n**Sub-Commands:**\n\n")
                    var cat : Category? = null
                    for(c in children) {
                        if(cat!=c.category) {
                            if(!c.category!!.test(event))
                                continue
                            cat = c.category
                            if(cat!=null)
                                b.append("\n__${cat.title}__\n\n")
                        }
                        b.append("`").append(event.client.prefix).append(c.fullname.toLowerCase())
                                .append(if(c.arguments.isNotEmpty()) " ${c.arguments}" else "")
                                .append("` - ").append(c.help).append("\n")
                    }
                }

                val owner = event.jda.getUserById(ownerId)
                if(owner != null)
                    b.append("\n\nFor additional help, contact **")
                            .append(owner.name)
                            .append("**#")
                            .append(owner.discriminator)
                            .append(" or join his support server ")
                            .append(serverInvite)
                else
                    b.append("\n\nFor additional help, join my support server ")
                            .append(serverInvite)
                if(helpInDM) {
                    if(event.isFromType(ChannelType.TEXT))
                        event.reactSuccess()
                    event.replyInDm(b.toString())
                } else {
                    event.reply(b.toString())
                }
            }
        }
    }

    fun run(event: CommandEvent)
    {
        if(children.isNotEmpty() && event.args.isNotEmpty())
        {
            val parts = Arrays.copyOf<String>(event.args.split(Regex("\\s+"), 2).toTypedArray(), 2)
            children.forEach { child ->
                if(child.isForCommand(parts[0]))
                {
                    event.args = parts[1]?:""
                    return child.run(event)
                }
            }
        }

        if(devOnly && !event.isDev)
            return

        if(category!=null && !category!!.test(event)) return

        if(event.args.startsWith("help",true))
            if(helpBiConsumer !== defaultSubHelp)
                return helpBiConsumer.accept(event, this)

        if(guildOnly && !event.isFromType(ChannelType.TEXT))
            return terminate(event,"${event.client.error} This command cannot be used in Direct messages")

        if(event.channelType == ChannelType.TEXT)
        {
            for(p in botPermissions)
            {
                if(p.isChannel)
                {
                    if(p.name.startsWith("VOICE"))
                    {
                        val vc = event.member.voiceState.channel
                        if(vc == null)
                            return terminate(event, "${event.client.error} You must be in a voice channel to use that!")
                        else if(!event.selfMember.hasPermission(vc, p))
                            return terminate(event, BOT_PERM.format(event.client.error, p.name, "Voice Channel"))
                    }
                    else if(!event.selfMember.hasPermission(event.textChannel, p))
                        return terminate(event, BOT_PERM.format(event.client.error, p.name, "Channel"))
                }
                else if(!event.selfMember.hasPermission(event.textChannel, p))
                    return terminate(event, BOT_PERM.format(event.client.error, p.name, "Guild"))
            }

            for(p in userPermissions)
            {
                if(p.isChannel && !event.member.hasPermission(event.textChannel, p))
                    return terminate(event, USER_PERM.format(event.client.error, p.name, "Channel"))
                else if(!event.member.hasPermission(event.textChannel, p))
                    return terminate(event, USER_PERM.format(event.client.error, p.name, "Guild"))
            }
        }

        val key = if(cooldown > 0) getCooldownKey(event) else null
        if(key!=null)
        {
            val remaining = event.client.getRemainingCooldown(key)
            if(remaining > 0)
            {
                val error = getCooldownError(event)
                return terminate(event,
                        "${event.client.warning} That command is on cooldown for $remaining more seconds${
                        if (error.isEmpty()) "!"
                        else " $error"
                        }!"
                )
            }
        }

        this::class.annotations.forEach {
            if(it is AutoInvokeCooldown && key!=null)
                event.client.applyCooldown(key, cooldown)
            if(event.args.isEmpty() && it is MustHaveArguments) {
                return if(it.error.isNotEmpty())
                    event.replyError(TOO_FEW_ARGS_ERROR.format(it.error))
                else
                    event.replyError(TOO_FEW_ARGS_HELP.format(event.client.prefix,
                            (if(fullname!="null") fullname else name).toLowerCase())
                    )
            }
        }

        try {
            execute(event)
            event.client.listener.onCommandCompleted(event, this)
        } catch (e: Throwable) {
            event.client.listener.onException(event, this, e)
        }
        event.client.incrementUses(this)
    }

    abstract protected fun execute(event: CommandEvent)

    fun isForCommand(string: String) : Boolean
    {
        if(string.equals(name, true))
            return true
        else if(aliases.isNotEmpty())
            aliases.forEach { alias -> if(string.equals(alias, true)) return true }
        return false
    }

    fun CommandEvent.modSearch() : Pair<Long, String?>?
    {
        val targetId = ArgumentPatterns.targetIDWithReason.matcher(this.args)
        val targetMention = ArgumentPatterns.targetMentionWithReason.matcher(this.args)

        return when {
            targetId.matches()      -> Pair(targetId.group(1).trim().toLong(), targetId.group(2)?.trim())
            targetMention.matches() -> Pair(targetMention.group(1).trim().toLong(), targetMention.group(2)?.trim())
            else                    -> { this.replyError(INVALID_ARGS_HELP.format(this.prefixUsed, name)); null }
        }
    }

    fun CommandEvent.invokeCooldown()
    {
        val key = getCooldownKey(this)
        if(key != null) this.client.applyCooldown(key, cooldown)
    }

    fun getCooldownKey(event: CommandEvent): String?
    {
        when (cooldownScope) {
            CooldownScope.USER ->
                return cooldownScope.genKey(name, event.author.idLong)
            CooldownScope.USER_GUILD ->
                return if(event.guild != null)
                    cooldownScope.genKey(name, event.author.idLong, event.guild.idLong)
                else
                    CooldownScope.USER_CHANNEL.genKey(name, event.author.idLong, event.channel.idLong)
            CooldownScope.USER_CHANNEL ->
                return cooldownScope.genKey(name, event.author.idLong, event.channel.idLong)
            CooldownScope.GUILD ->
                return if(event.guild != null)
                    cooldownScope.genKey(name, event.guild.idLong)
                else
                    CooldownScope.CHANNEL.genKey(name, event.channel.idLong)
            CooldownScope.CHANNEL ->
                return cooldownScope.genKey(name, event.channel.idLong)
            CooldownScope.GLOBAL ->
                return cooldownScope.genKey(name, 0L)
            else -> return null
        }
    }

    fun getCooldownError(event: CommandEvent): String
    {
        return if((cooldownScope == CooldownScope.USER_GUILD || cooldownScope == CooldownScope.GUILD) && event.guild == null)
            CooldownScope.CHANNEL.errorFlair
        else
            cooldownScope.errorFlair
    }

    private fun terminate(event: CommandEvent, msg: String)
    {
        event.client.listener.onCommandTerminated(event, this, msg)
        event.client.incrementUses(this)
    }

}

enum class Category(val title: String, private val predicate: (CommandEvent) -> Boolean)
{
    // Primary Hierarchy
    MONITOR("Developer", { it.isDev }),
    SERVER_OWNER("Server Owner", { MONITOR.test(it) || it.member.isOwner }),
    ADMIN("Administrator", {
        SERVER_OWNER.test(it) || (it.isFromType(ChannelType.TEXT) && it.member.hasPermission(Permission.ADMINISTRATOR))
    }),
    MODERATOR("Moderator", {
        ADMIN.test(it) || (it.isFromType(ChannelType.TEXT) && with(it.manager.getModRole(it.guild)) {
            this!=null && it.member.roles.contains(this)
        })
    }),

    // Other Categories
    NSFW("NSFW", { Category.MONITOR.test(it) || (it.isFromType(ChannelType.TEXT) && it.textChannel.isNSFW) });

    fun test(event: CommandEvent) = predicate.invoke(event)
}

enum class CooldownScope constructor(private val format: String, internal val errorFlair: String)
{
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

    internal fun genKey(name: String, id: Long): String {
        return genKey(name, id, -1)
    }

    internal fun genKey(name: String, idOne: Long, idTwo: Long): String {
        return "$name|${when {
                this == GLOBAL -> format
                idTwo == -1L   -> format.format(idOne)
                else           -> format.format(idOne, idTwo)
            }
        }"
    }
}

abstract class NoBaseExecutionCommand : Command()
{
    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            event.replyError(Command.TOO_FEW_ARGS_HELP.format(event.client.prefix, this.name))
        else
            event.replyError(Command.INVALID_ARGS_HELP.format(event.client.prefix, this.name))
    }
}