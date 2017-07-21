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

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * @author Kaidan Gustave
 */
abstract class Command
{
    var name: String = "null"
        protected set(value) {field = value}

    var aliases: Array<String> = emptyArray()
        protected set(value) {field = value}

    var arguments: Argument = Argument("")
        protected set(value) {field = value}

    var help: String = "no help available"
        protected set(value) {field = value}

    var devOnly: Boolean = false
        protected set(value) {field = value}

    var guildOnly: Boolean = true
        protected set(value) {field = value}

    var category: Category? = null
        protected set(value) {field = value}

    var helpBiConsumer: ((CommandEvent, Command) -> Unit)? = null
        protected set(value) {field = value}

    var botPermissions: Array<Permission> = emptyArray()
        protected set(value) {field = value}

    var userPermissions: Array<Permission> = emptyArray()
        protected set(value) {field = value}

    var cooldown: Int = 0
        protected set(value) {field = value}

    var cooldownScope: CooldownScope = CooldownScope.USER
        protected set(value) {field = value}

    var children: Array<Command> = emptyArray()
        protected set(value) {field = value}

    var fullname: String = name
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

        fun standardSubHelp(explanation: String?, helpInDM : Boolean) : (CommandEvent, Command) -> Unit
        {
            return {event, command ->
                val b = StringBuilder()
                val ownerId = event.client.ownerID
                val serverInvite = event.client.server
                val aliases = command.aliases
                val children = command.children
                b.append("Available help for **${command.name} command** in " +
                        "${if(event.isFromType(ChannelType.PRIVATE)) "DM" else "<#" + event.channel.id + ">"}\n\n")

                if(aliases.isNotEmpty()) {
                    b.append("**Aliases:** `")
                    for(i in aliases.indices) {
                        b.append("${aliases[i]}`")
                        if(i != aliases.size - 1)
                            b.append(", `")
                    }
                    b.append("\n")
                }

                if(explanation != null)
                    b.append("\n$explanation\n")

                b.append("\n**Sub-Commands:**\n")
                for(child in children) {
                    val category = child.category
                    if(event.isOwner || category == null || category.test(event))
                        b.append("\n`").append(child.name)
                                .append(if(child.arguments.args.isEmpty()) "`" else " ${child.arguments}`")
                                .append(" ")
                                .append(child.help)
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

        if(devOnly && !event.isOwner)
            return

        if(category!=null && !category!!.test(event)) return

        if(event.args.startsWith("help",true) && helpBiConsumer!=null)
            return helpBiConsumer!!.invoke(event, this)

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

        if(!arguments.test(event))
        {
            val error = arguments.error
            if(error!=null)
            {
                if(error==Command.INVALID_ARGS_HELP)
                    return event.replyError(INVALID_ARGS_HELP.format(event.prefixUsed, fullname))
                return event.replyError(error)
            }
        }

        if(cooldown > 0)
        {
            val key = getCooldownKey(event)
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
                event.client.applyCooldown(key, cooldown)
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

    private fun terminate(event: CommandEvent, msg: String)
    {
        event.client.listener.onCommandTerminated(event, this, msg)
        event.client.incrementUses(this)
    }

    fun isForCommand(string: String) : Boolean
    {
        if(string.equals(name, true))
            return true
        else if(aliases.isNotEmpty())
            aliases.forEach { alias -> if(string.equals(alias, true)) return true }
        return false
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
        if((cooldownScope == CooldownScope.USER_GUILD || cooldownScope == CooldownScope.GUILD) && event.guild == null)
            return CooldownScope.CHANNEL.errorFlair
        else
            return cooldownScope.errorFlair
    }
}

class Argument(val args: String, val error: String?, val pattern: Pattern?)
{
    constructor(args: String) : this(args, null, null)
    constructor(args: String, pattern: Pattern) : this(args, Command.INVALID_ARGS_HELP, pattern)

    fun test(event: CommandEvent) = pattern?.matcher(event.args)?.matches()?:true

    override fun toString(): String = args
}

class Category(val name: String, private val predicate: Predicate<CommandEvent>?)
{
    companion object
    {
        val OWNER: Category = Category("Owner", { event -> event.isOwner })
        val SERVER_OWNER: Category =  Category("Server Owner", { event ->
            OWNER.test(event) || event.member.isOwner
        })
        val ADMIN: Category = Category("Administrator", { event ->
            SERVER_OWNER.test(event)
                    || (event.isFromType(ChannelType.TEXT)
                    && event.member.hasPermission(Permission.ADMINISTRATOR))

        })
        val MODERATOR: Category = Category("Moderator", { event : CommandEvent ->
            ADMIN.test(event)
                    || (event.isFromType(ChannelType.TEXT)
                    && event.client.manager.getModRole(event.guild)!=null
                    && event.member.roles.contains(event.client.manager.getModRole(event.guild)))
        })
    }

    constructor(name: String, predicate: (CommandEvent) -> Boolean) : this(name, Predicate(predicate))

    fun test(event: CommandEvent) = predicate != null && predicate.test(event)
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
        if(this == GLOBAL)
            return name + "|" + format
        else if(idTwo == -1L)
            return name + "|" + String.format(format, idOne)
        else
            return name + "|" + String.format(format, idOne, idTwo)
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