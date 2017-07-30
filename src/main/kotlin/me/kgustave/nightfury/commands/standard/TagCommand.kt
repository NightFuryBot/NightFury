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
package me.kgustave.nightfury.commands.standard

import club.minnced.kjda.promise
import com.jagrosh.jdautilities.waiter.EventWaiter
import me.kgustave.nightfury.*
import me.kgustave.nightfury.annotations.AutoInvokeCooldown
import me.kgustave.nightfury.db.sql.SQLGlobalTags
import me.kgustave.nightfury.db.sql.SQLLocalTags
import me.kgustave.nightfury.extensions.Find
import me.kgustave.nightfury.extensions.waiting.paginator
import me.kgustave.nightfury.jagtag.TagErrorException
import me.kgustave.nightfury.utils.formatUserName
import me.kgustave.nightfury.utils.multipleMembersFound
import me.kgustave.nightfury.utils.multipleUsersFound
import me.kgustave.nightfury.utils.noMatch
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User

/**
 * @author Kaidan Gustave
 */
class TagCommand(waiter: EventWaiter) : Command()
{
    init {
        this.name = "tag"
        this.aliases = arrayOf("t")
        this.arguments = Argument("[tag name] <tag args>")
        this.help = "calls a tag"
        this.helpBiConsumer = Command.standardSubHelp(
                null,
                true
        )
        this.guildOnly = false
        this.children = arrayOf(
                TagCreateGlobalCmd(),
                TagCreateCmd(),
                TagDeleteCmd(),
                TagEditCmd(),
                TagListCmd(waiter),
                TagOwnerCmd(),
                TagRawCmd(),

                TagOverrideCmd()
        )
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_ERROR.format("Try specifying a tag name in the format `${event.prefixUsed}tag [tag name]`."))
        val parts = event.args.split(Regex("\\s+"),2)
        val name = if(event.client.getCommandByName(parts[0])!=null)
            return event.reply("*You remember Monitor's words: Not everything is a tag!*")
        else parts[0]
        val args = if(parts.size>1) parts[1] else ""
        if(event.isFromType(ChannelType.TEXT)) {
            val content : String = if(event.localTags.isTag(name, event.guild)) {
                event.localTags.getTagContent(name, event.guild)
            } else if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""
            if(content.isEmpty())
                return event.replyError("**No Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.prefixUsed,this.name)}")
            else try {
                event.reply(
                        event.client.parser.put("args", args.trim())
                                .put("user", event.author)
                                .put("guild", event.guild)
                                .put("channel", event.textChannel)
                                .parse(content)
                )
            } catch (e : TagErrorException) {
                if(e.message!=null) event.replyError(e.message)
                else                event.replyError("Tag matching \"$name\" could not be processed for an unknown reason!")
            }
        } else {
            val content : String = if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""

            if(content.isEmpty())
                return event.replyError("**No Global Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.prefixUsed,this.name)}")
            else try {
                event.reply(
                        event.client.parser.put("args", args)
                                .put("user", event.author)
                                .parse(content)
                )
            } catch (e : TagErrorException) {
                if(e.message!=null) event.replyError(e.message)
                else                event.replyError("Tag matching \"$name\" could not be processed for an unknown reason!")
            }
        }
        event.client.parser.clear()
    }
}

private class TagCreateCmd : Command()
{
    init {
        this.name = "create"
        this.fullname = "tag create"
        this.arguments = Argument("<tag name> <tag content>")
        this.help = "creates a new local tag"
        this.cooldown = 150
        this.cooldownScope = CooldownScope.USER_GUILD
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length>50)
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(event.client.getCommandByName(parts[0])!=null)
            return event.replyError("**Illegal Tag Name!**\n" +
                    "Tags may not have names that match command names!")
        else parts[0]

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a tag!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        if(event.localTags.isTag(name, event.guild) || event.globalTags.isTag(name))
            return event.replyError("Tag named \"$name\" already exists!")
        else {
            event.localTags.addTag(name, event.author.idLong, content, event.guild)
            event.replySuccess("Successfully created local tag \"**$name**\" on ${event.guild.name}!")
            invokeCooldown(event)
        }
    }
}

private class TagCreateGlobalCmd : Command()
{
    init {
        this.name = "createglobal"
        this.fullname = "tag createglobal"
        this.arguments = Argument("<tag name> <tag content>")
        this.help = "creates a new global tag"
        this.cooldown = 240
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length>50)
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(event.client.getCommandByName(parts[0])!=null)
            return event.replyError("**Illegal Tag Name!**\n" +
                    "Tags may not have names that match command names!")
        else parts[0]

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a tag!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        if((event.isFromType(ChannelType.TEXT) && event.localTags.isTag(name, event.guild)) || event.globalTags.isTag(name))
            return event.replyError("Tag named \"$name\" already exists!")
        else {
            event.globalTags.addTag(name,event.author.idLong,content)
            event.replySuccess("Successfully created global tag \"**$name**\"!")
            invokeCooldown(event)
        }
    }
}

private class TagDeleteCmd : Command()
{
    init {
        this.name = "delete"
        this.fullname = "tag delete"
        this.arguments = Argument("[tag name]")
        this.help = "deletes a tag you own"
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val name = event.args.split(Regex("\\s+"))[0]
        if(event.isFromType(ChannelType.TEXT)) {
            if(!event.localTags.isTag(name, event.guild)) {
                if(!event.globalTags.isTag(name))
                    event.replyError("Tag named \"$name\" does not exist!")
                else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                    event.globalTags.deleteTag(name, event.author.idLong)
                    event.replySuccess("Successfully deleted local tag \"**$name**\"!")
                } else {
                    event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                            SEE_HELP.format(event.prefixUsed, fullname))
                }
            } else if(event.localTags.getTagOwnerId(name,event.guild)==event.author.idLong) {
                event.localTags.deleteTag(name, event.author.idLong, event.guild)
                event.replySuccess("Successfully deleted local tag \"**$name**\"!")
            } else {
                event.replyError("**You cannot delete the local tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.prefixUsed, fullname))
            }
        } else {
            if(!event.globalTags.isTag(name))
                event.replyError("Tag named \"$name\" does not exist!")
            else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                event.globalTags.deleteTag(name, event.author.idLong)
                event.replySuccess("Successfully deleted local tag \"**$name**\"!")
            } else {
                event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.prefixUsed, fullname))
            }
        }
    }
}

private class TagEditCmd : Command()
{
    init {
        this.name = "edit"
        this.fullname = "tag edit"
        this.arguments = Argument("<tag name> <tag content>")
        this.help = "edits a tag you own"
        this.cooldown = 180
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length<=50)
            parts[0]
        else
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))

        val newContent = if(parts.size==1)
            return event.replyError("**You must specify content when editing a tag!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        if(event.isFromType(ChannelType.TEXT)) {
            if(!event.localTags.isTag(name, event.guild)) {
                if(!event.globalTags.isTag(name)) {
                    event.replyError("Tag named \"$name\" does not exist!")
                } else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                    event.globalTags.editTag(newContent, name, event.author.idLong)
                    event.replySuccess("Successfully edit global tag \"**$name**\"!")
                    invokeCooldown(event)
                } else {
                    event.replyError("**You cannot edit the global tag \"$name\" because you are not it's owner!**\n" +
                            SEE_HELP.format(event.prefixUsed, fullname))
                }
            } else if(event.localTags.getTagOwnerId(name, event.guild)==event.author.idLong) {
                event.localTags.editTag(newContent, name, event.author.idLong, event.guild)
                event.replySuccess("Successfully edit local tag \"**$name**\"!")
                invokeCooldown(event)
            } else {
                event.replyError("**You cannot edit the local tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.prefixUsed, fullname))
            }
        } else {
            if(!event.globalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else if(event.globalTags.getTagOwnerId(name)==event.author.idLong) {
                event.globalTags.editTag(newContent, name, event.author.idLong)
                event.replySuccess("Successfully edit local tag \"**$name**\"!")
                invokeCooldown(event)
            } else {
                event.replyError("**You cannot edit the global tag \"$name\" because you are not it's owner!**\n" +
                        SEE_HELP.format(event.prefixUsed, fullname))
            }
        }
    }
}

@AutoInvokeCooldown
private class TagListCmd(val waiter: EventWaiter) : Command()
{
    init {
        this.name = "list"
        this.fullname = "tag list"
        this.arguments = Argument("<user>")
        this.help = "gets all the tags owned by a user"
        this.guildOnly = false
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
    }

    override fun execute(event: CommandEvent)
    {
        val query = event.args
        val temp : Member? = if(event.isFromType(ChannelType.TEXT)) {
            if(query.isEmpty()) {
                event.member
            } else {
                val found = Find.members(query, event.guild)
                if(found.isEmpty()) null
                else if(found.size>1) return event.replyError(multipleMembersFound(query, found))
                else found[0]
            }
        } else null

        val user : User = if(temp!=null) {
            temp.user
        } else if(query.isEmpty()) {
            event.author
        } else {
            val found = Find.users(query, event.jda)
            if(found.isEmpty()) return event.replyError(noMatch("users", query))
            else if(found.size>1) return event.replyError(multipleUsersFound(query, found))
            else found[0]
        }
        val member : Member? = if(temp == null && event.isFromType(ChannelType.TEXT)) event.guild.getMember(user) else temp


        val localTags = (if(member!=null) event.localTags.getAllTags(member.user.idLong,event.guild) else emptySet()).map { "$it (Local)" }
        val globalTags = event.globalTags.getAllTags(user.idLong).map { "$it (Global)" }

        if(localTags.isEmpty() && globalTags.isEmpty())
            event.replyError("${if(event.author==user) "You do" else "${formatUserName(user, true)} does"} not have any tags!")

        paginator(waiter, event.channel)
        {
            text             { "Tags owned by ${formatUserName(user, true)}" }
            timeout          { 20 }
            if(localTags.isNotEmpty())
                items        { addAll(localTags) }
            items            { addAll(globalTags) }
            finalAction      { it.editMessage(it).queue(); event.linkMessage(it) }
            showPageNumbers  { true }
            useNumberedItems { true }
            waitOnSinglePage { false }
        }
    }
}

private class TagOwnerCmd : Command()
{
    init {
        this.name = "owner"
        this.aliases = arrayOf("creator")
        this.fullname = "tag owner"
        this.arguments = Argument("[tag name]")
        this.help = "gets the owner of a tag"
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty()) return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))

        val name = event.args.split(Regex("\\s+"))[0]
        val ownerId : Long
        val isLocal : Boolean = if(event.isFromType(ChannelType.TEXT)) {
            if(event.localTags.isTag(name, event.guild)) {
                ownerId = event.localTags.getTagOwnerId(name, event.guild)
                true
            } else if(event.globalTags.isTag(name)) {
                ownerId = event.globalTags.getTagOwnerId(name)
                false
            } else return event.replyError("Tag named \"$name\" does not exist!")
        } else if(event.globalTags.isTag(name)) {
            ownerId = event.globalTags.getTagOwnerId(name)
            false
        } else return event.replyError("Tag named \"$name\" does not exist!")

        // If this happens... Uh... Let's just put this here incase :/
        if(ownerId==0L) return event.replyError("Tag named \"$name\" does not exist!")
        // Cover overrides
        if(isLocal && ownerId==1L) {
            invokeCooldown(event)
            return event.replyWarning("Local tag named \"$name\" belongs to the server.")
        }

        val str = if(isLocal) "local tag \"${event.localTags.getOriginalName(name, event.guild)}\""
        else "global tag \"${event.globalTags.getOriginalName(name)}\""

        event.jda.retrieveUserById(ownerId).promise() then {
            if(it == null) event.replyError("The owner of $str was improperly retrieved!")
            else           event.replySuccess("The $str is owned by ${formatUserName(it, true)}${
            if(!event.jda.users.contains(it)) " (ID: ${it.id})." else "."
            }")
            invokeCooldown(event)
        } catch {
            event.replyError("The owner of $str could not be retrieved for an unexpected reason!")
        }
    }
}

private class TagRawCmd : Command()
{
    init {
        this.name = "raw"
        this.fullname = "tag raw"
        this.arguments = Argument("[tag name]")
        this.help = "gets the raw, non-parsed form of a tag"
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_ERROR.format("Try specifying a tag name in the format `${event.prefixUsed}$fullname [tag name]`."))
        val parts = event.args.split(Regex("\\s+"),2)
        val name = parts[0]
        if(event.isFromType(ChannelType.TEXT))
        {
            val content : String = if(event.localTags.isTag(name, event.guild)) {
                event.localTags.getTagContent(name, event.guild)
            } else if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""
            if(content.isEmpty())
                return event.replyError("**No Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.prefixUsed,this.fullname)}")
            else event.reply("```\n$content```")
        }
        else
        {
            val content : String = if(event.globalTags.isTag(name)) {
                event.globalTags.getTagContent(name)
            } else ""

            if(content.isEmpty())
                return event.replyError("**No Global Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.prefixUsed,this.fullname)}")
            else event.reply("```\n$content```")
        }
    }
}

private class TagOverrideCmd : Command()
{
    init {
        this.name = "override"
        this.fullname = "tag override"
        this.arguments = Argument("<tag name> <tag content>")
        this.help = "overrides a local tag"
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(TOO_FEW_ARGS_HELP.format(event.prefixUsed, fullname))
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length<=50)
            parts[0]
        else
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))

        val newContent = if(parts.size==1)
            return event.replyError("**You must specify content when overriding a tag!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        if(!event.localTags.isTag(name,event.guild)) {
            if(!event.globalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else {
                val ownerId = event.globalTags.getTagOwnerId(name)
                val member = event.guild.getMemberById(ownerId)
                if(member!=null && (member.isOwner || event.member.canInteract(member)))
                    return event.replyError("I cannot override the global tag \"**$name**\" because " +
                            "you are not able to interact with them due to role hierarchy placement!")
                event.localTags.overrideTag(newContent, event.globalTags.getOriginalName(name), ownerId, event.guild)
                event.replySuccess("Successfully overrode global tag \"**$name**\"!")
            }
        } else {
            val member = event.guild.getMemberById(event.globalTags.getTagOwnerId(name))
            if(member!=null && (member.isOwner || event.member.canInteract(member)))
                return event.replyError("I cannot override the global tag \"**$name**\" because " +
                        "you are not able to interact with them due to role hierarchy placement!")
            event.localTags.addTag(event.globalTags.getOriginalName(name), 1L, newContent, event.guild)
            event.replySuccess("Successfully overrode global tag \"**$name**\"!")
        }
    }
}

val CommandEvent.localTags : SQLLocalTags
    get() {return this.client.manager.localTags}
val CommandEvent.globalTags : SQLGlobalTags
    get() {return this.client.manager.globalTags}