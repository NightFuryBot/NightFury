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
package xyz.nightfury.commands.standard

import xyz.nightfury.entities.menus.Paginator
import xyz.nightfury.entities.menus.EventWaiter
import xyz.nightfury.*
import xyz.nightfury.annotations.AutoInvokeCooldown
import xyz.nightfury.annotations.MustHaveArguments
import xyz.nightfury.db.SQLGlobalTags
import xyz.nightfury.db.SQLLocalTags
import xyz.nightfury.entities.promise
import xyz.nightfury.extensions.*
import xyz.nightfury.jagtag.TagErrorException
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import xyz.nightfury.Category
import xyz.nightfury.Command
import xyz.nightfury.CommandEvent
import xyz.nightfury.CooldownScope
import xyz.nightfury.annotations.HasDocumentation
import xyz.nightfury.resources.Arguments

/**
 * @author Kaidan Gustave
 */
@HasDocumentation
@MustHaveArguments("Specify the name of a tag to call.")
class TagCommand(waiter: EventWaiter): Command() {
    init {
        this.name = "Tag"
        this.aliases = arrayOf("t")
        this.arguments = "[Tag Name] <Tag Args>"
        this.help = "Calls a tag."
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

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Arguments.commandArgs,2)

        val name = if(event.client.commands[parts[0]] != null)
            // The tag is a command name, so we inform the user that it's a command not a tag and end
            return event.reply("You remember Monitor's words: *\"Not everything is a tag!\"*")
        else parts[0]

        // Always have at minimum an empty string
        val args = if(parts.size>1) parts[1] else ""

        if(event.isFromType(ChannelType.TEXT)) {
            val content : String = when {
                SQLLocalTags.isTag(name, event.guild) -> SQLLocalTags.getTagContent(name, event.guild)
                SQLGlobalTags.isTag(name) -> SQLGlobalTags.getTagContent(name)

                else -> ""
            }
            // If the content is empty no tag was found, so tell them there is no tag and end
            if(content.isEmpty())
                return event.replyError("**No Tag Found Matching \"$name\"**\n" +
                                        SEE_HELP.format(event.client.prefix,this.name))

            try {
                // Parse the content and run
                event.reply(event.client.parser.clear()
                        .put("args", args.trim())
                        .put("user", event.author)
                        .put("guild", event.guild)
                        .put("channel", event.textChannel)
                        .parse(content))
            } catch (e : TagErrorException) {
                // Errors that happen will be caught and sent separately to provide concise info on what went wrong
                if(e.message!=null)
                    event.replyError(e.message)
                else event.replyError("Tag matching \"$name\" could not be processed for an unknown reason!")
            }
        } else {
            // Not from a guild, no need to check for local tags
            val content : String = if(SQLGlobalTags.isTag(name)) SQLGlobalTags.getTagContent(name) else ""

            // If the content is empty no tag was found, so tell them there is no tag and end
            if(content.isEmpty())
                return event.replyError("**No Global Tag Found Matching \"$name\"**\n" +
                                        SEE_HELP.format(event.client.prefix,this.name))
            else try {
                // Parse content and run
                event.reply(event.client.parser.clear()
                        .put("args", args)
                        .put("user", event.author)
                        .parse(content))
            } catch (e : TagErrorException) {
                // Errors that happen will be caught and sent separately to provide concise info on what went wrong
                if(e.message!=null)
                    event.replyError(e.message)
                else event.replyError("Tag matching \"$name\" could not be processed for an unknown reason!")
            }
        }
    }
}

@MustHaveArguments
private class TagCreateCmd : Command() {
    init {
        this.name = "Create"
        this.fullname = "Tag Create"
        this.arguments = "[Tag Name] [Tag Content]"
        this.help = "Creates a new local Tag."
        this.documentation =
            "Local tags are only available to the server they are created on.\n" +
            "If there is already a global tag with the name specified when using this " +
            "command, a local tag cannot be created, however a moderator or administrator " +
            "may use the `Override` sub-command to create a local version as a replacement.\n\n" +

            "Tag names cannot exceed 50 characters in length and cannot contain whitespace.\n" +
            "Tag content cannot exceed 1900 characters.\n\n" +

            "*If you discover any NSFW, racist, or in any other way 'harmful' tags, please report " +
            "them immediately!*"
        this.cooldown = 150
        this.cooldownScope = CooldownScope.USER_GUILD
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Regex("\\s+"),2)

        val error = when {
            parts[0].length>50 ->
                "**Tag names cannot exceed 50 characters in length!**\n" +
                SEE_HELP.format(event.client.prefix, fullname)
            event.client.commands[parts[0]]!=null ->
                "**Illegal Tag Name!**\n" +
                "Tags may not have names that match standard command names!"
            parts.size==1 ->
                "**You must specify content when creating a Tag!**\n" +
                SEE_HELP.format(event.client.prefix, fullname)
            parts[1].length>1900 ->
                "**Tag content cannot exceed 1900 characters in length!**\n" +
                SEE_HELP.format(event.client.prefix, fullname)
            else -> null
        }

        if(error!=null) return event.replyError(error)

        val name = parts[0]
        val content = parts[1]

        // Fails if:
        // There exists a local tag by the name specified
        // There exists a global tag by the name specified
        if(SQLLocalTags.isTag(name, event.guild) || SQLGlobalTags.isTag(name))
            return event.replyError("Tag named \"$name\" already exists!")

        SQLLocalTags.addTag(name, event.author.idLong, content, event.guild)
        event.replySuccess("Successfully created local tag \"**$name**\" on ${event.guild.name}!")
        event.invokeCooldown()
    }
}

@MustHaveArguments
private class TagCreateGlobalCmd : Command() {
    init {
        this.name = "CreateGlobal"
        this.fullname = "Tag CreateGlobal"
        this.arguments = "[Tag Name] [Tag Content]"
        this.help = "Creates a new global tag."
        this.documentation =
            "Global tags are available to all servers.\n" +
            "If there is already a global tag with the name specified when using this " +
            "command, a global tag cannot be created, however a local override can be made " +
            "on any server by a moderator or administrator using the `Override` sub-command.\n\n" +

            "Tag names cannot exceed 50 characters in length and cannot contain whitespace.\n" +
            "Tag content cannot exceed 1900 characters.\n\n" +

            "*If you discover any NSFW, racist, or in any other way 'harmful' tags, please report " +
            "them immediately!*"
        this.cooldown = 240
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Regex("\\s+"),2)

        when {
            parts[0].length>50                    -> "**Tag names cannot exceed 50 characters in length!**\n" +
                                                     SEE_HELP.format(event.client.prefix, fullname)

            event.client.commands[parts[0]]!=null -> "**Illegal Tag Name!**\n" +
                                                     "Tags may not have names that match standard command names!"

            parts.size==1                         -> "**You must specify content when creating a Tag!**\n" +
                                                     SEE_HELP.format(event.client.prefix, fullname)

            parts[1].length>1900                  -> "**Tag content cannot exceed 1900 characters in length!**\n" +
                                                     SEE_HELP.format(event.client.prefix, fullname)

            else -> null
        }?.let { return event.replyError(it) }

        val name = parts[0]
        val content = parts[1]

        // Fails if:
        // From a TextChannel and there exists a local tag by the name specified
        // From a PrivateChannel and there exists a global tag by the name specified
        if((event.isFromType(ChannelType.TEXT) && SQLLocalTags.isTag(name, event.guild)) || SQLGlobalTags.isTag(name))
            return event.replyError("Tag named \"$name\" already exists!")

        SQLGlobalTags.addTag(name, event.author.idLong, content)
        event.replySuccess("Successfully created global tag \"**$name**\"!")
        event.invokeCooldown()
    }
}

@MustHaveArguments
private class TagDeleteCmd : Command() {
    init {
        this.name = "Delete"
        this.fullname = "Tag Delete"
        this.arguments = "[Tag Name]"
        this.help = "Deletes a tag you own."
        this.documentation =
            "It's worth noting that if a user owns both the local and global version " +
            "of a tag when using this command on a server, the priority when deleting goes " +
            "to the *local* version, not the global one."
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val name = event.args.split(Regex("\\s+"))[0]
        if(event.isFromType(ChannelType.TEXT)) {
            if(!SQLLocalTags.isTag(name, event.guild)) {
                if(!SQLGlobalTags.isTag(name))
                    event.replyError("Tag named \"$name\" does not exist!")
                else if(SQLGlobalTags.getTagOwnerId(name)==event.author.idLong) {
                    SQLGlobalTags.deleteTag(name, event.author.idLong)
                    event.replySuccess("Successfully deleted local tag \"**$name**\"!")
                } else {
                    event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                                     SEE_HELP.format(event.client.prefix, fullname))
                }
            } else if(SQLLocalTags.getTagOwnerId(name,event.guild)==event.author.idLong) {
                SQLLocalTags.deleteTag(name, event.author.idLong, event.guild)
                event.replySuccess("Successfully deleted local tag \"**$name**\"!")
            } else {
                event.replyError("**You cannot delete the local tag \"$name\" because you are not it's owner!**\n" +
                                 SEE_HELP.format(event.client.prefix, fullname))
            }
        } else {
            if(!SQLGlobalTags.isTag(name))
                event.replyError("Tag named \"$name\" does not exist!")
            else if(SQLGlobalTags.getTagOwnerId(name)==event.author.idLong) {
                SQLGlobalTags.deleteTag(name, event.author.idLong)
                event.replySuccess("Successfully deleted local tag \"**$name**\"!")
            } else {
                event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                                 SEE_HELP.format(event.client.prefix, fullname))
            }
        }
    }
}

@MustHaveArguments
private class TagEditCmd : Command() {
    init {
        this.name = "Edit"
        this.fullname = "Tag Edit"
        this.arguments = "[Tag Name] [New Tag Content]"
        this.help = "Edits a tag you own."
        this.documentation =
            "It's worth noting that if a user owns both the local and global version " +
            "of a tag when using this command on a server, the priority when editing goes " +
            "to the *local* version, not the global one."
        this.cooldown = 180
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Regex("\\s+"),2)

        val error = when {
            parts[0].length>50 ->
                "**Tag names cannot exceed 50 characters in length!**\n" +
                SEE_HELP.format(event.client.prefix, fullname)
            event.client.commands[parts[0]]!=null ->
                "**Illegal Tag Name!**\n" +
                "Tags may not have names that match standard command names!"
            parts.size==1 ->
                "**You must specify content when creating a Tag!**\n" +
                SEE_HELP.format(event.client.prefix, fullname)
            parts[1].length>1900 ->
                "**Tag content cannot exceed 1900 characters in length!**\n" +
                SEE_HELP.format(event.client.prefix, fullname)
            else -> null
        }

        if(error != null) return event.replyError(error)

        val name = parts[0]
        val newContent = parts[1]

        if(event.isFromType(ChannelType.TEXT)) {
            if(!SQLLocalTags.isTag(name, event.guild)) {
                if(!SQLGlobalTags.isTag(name)) {
                    event.replyError("Tag named \"$name\" does not exist!")
                } else if(SQLGlobalTags.getTagOwnerId(name)==event.author.idLong) {
                    SQLGlobalTags.editTag(newContent, name, event.author.idLong)
                    event.replySuccess("Successfully edit global tag \"**$name**\"!")
                    event.invokeCooldown()
                } else {
                    event.replyError("**You cannot edit the global tag \"$name\" because you are not it's owner!**\n" +
                                     SEE_HELP.format(event.client.prefix, fullname))
                }
            } else if(SQLLocalTags.getTagOwnerId(name, event.guild)==event.author.idLong) {
                SQLLocalTags.editTag(newContent, name, event.author.idLong, event.guild)
                event.replySuccess("Successfully edit local tag \"**$name**\"!")
                event.invokeCooldown()
            } else {
                event.replyError("**You cannot edit the local tag \"$name\" because you are not it's owner!**\n" +
                                 SEE_HELP.format(event.client.prefix, fullname))
            }
        } else {
            if(!SQLGlobalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else if(SQLGlobalTags.getTagOwnerId(name)==event.author.idLong) {
                SQLGlobalTags.editTag(newContent, name, event.author.idLong)
                event.replySuccess("Successfully edit local tag \"**$name**\"!")
                event.invokeCooldown()
            } else {
                event.replyError("**You cannot edit the global tag \"$name\" because you are not it's owner!**\n" +
                                 SEE_HELP.format(event.client.prefix, fullname))
            }
        }
    }
}

@AutoInvokeCooldown
private class TagListCmd(val waiter: EventWaiter): Command() {
    val builder: Paginator.Builder = Paginator.Builder()
            .timeout          { delay { 20 } }
            .showPageNumbers  { true }
            .numberItems      { true }
            .waitOnSinglePage { true }
            .waiter           { waiter }

    init {
        this.name = "List"
        this.fullname = "Tag List"
        this.arguments = "<User>"
        this.help = "Gets all the tags owned by a user."
        this.documentation =
            "Not specifying a user will get a list of tags owned by the person using the command."
        this.guildOnly = false
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
    }

    override fun execute(event: CommandEvent) {
        val query = event.args
        val temp: Member? = if(event.isFromType(ChannelType.TEXT)) {
            if(query.isEmpty()) {
                event.member
            } else {
                val found = event.guild.findMembers(query)
                when {
                    found.isEmpty() -> null
                    found.size>1 -> return event.replyError(found.multipleMembers(query))
                    else -> found[0]
                }
            }
        } else null

        val user: User = when {
            temp!=null -> temp.user
            query.isEmpty() -> event.author
            else -> {
                val found = event.jda.findUsers(query)
                when {
                    found.isEmpty() -> return event.replyError(noMatch("users", query))
                    found.size>1 -> return event.replyError(found.multipleUsers(query))
                    else -> found[0]
                }
            }
        }

        val member = if(temp == null && event.isFromType(ChannelType.TEXT)) event.guild.getMember(user) else temp

        val localTags: List<String> = if(member!=null)
            SQLLocalTags.getAllTags(member.user.idLong,event.guild).map { "$it (Local)" }
        else emptyList()

        val globalTags = SQLGlobalTags.getAllTags(user.idLong).map { "$it (Global)" }

        if(localTags.isEmpty() && globalTags.isEmpty())
            return event.replyError("${
                if(event.author==user) "You do" else "${user.formattedName(false)} does"
            } not have any tags!")

        event.invokeCooldown()
        with(builder) {
            clearItems()
            text { _,_-> "Tags owned by ${user.formattedName(true)}" }
            items {
                if(localTags.isNotEmpty())
                    addAll(localTags)
                addAll(globalTags)
            }
            finalAction {
                event.linkMessage(it)
                it.removeMenuReactions()
            }
            user { event.author }
            displayIn { event.channel }
        }
    }
}

@MustHaveArguments
private class TagOwnerCmd : Command() {
    init {
        this.name = "Owner"
        this.fullname = "Tag Owner"
        this.aliases = arrayOf("creator")
        this.arguments = "[Tag Name]"
        this.help = "Gets the owner of a tag."
        this.documentation =
            "There are several cases where this command **will not work**.\n" +
            "It is a semi-reliable way to get the owner of a command, but there is " +
            "no guarantee that an owner name (or even ID) will be returned."
        this.cooldown = 10
        this.cooldownScope = CooldownScope.USER
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val name = event.args.split(Regex("\\s+"))[0]
        val ownerId : Long
        val isLocal : Boolean = when {
            event.isFromType(ChannelType.TEXT) -> {
                when {
                    SQLLocalTags.isTag(name, event.guild) -> {
                        ownerId = SQLLocalTags.getTagOwnerId(name, event.guild)
                        true
                    }
                    SQLGlobalTags.isTag(name) -> {
                        ownerId = SQLGlobalTags.getTagOwnerId(name)
                        false
                    }
                    else -> return event.replyError("Tag named \"$name\" does not exist!")
                }
            }
            SQLGlobalTags.isTag(name) -> {
                ownerId = SQLGlobalTags.getTagOwnerId(name)
                false
            }
            else -> return event.replyError("Tag named \"$name\" does not exist!")
        }

        // If this happens... Uh... Let's just put this here in case :/
        if(ownerId==0L) return event.replyError("Tag named \"$name\" does not exist!")
        // Cover overrides
        if(isLocal && ownerId==1L) {
            event.invokeCooldown()
            return event.replyWarning("Local tag named \"$name\" belongs to the server.")
        }

        val str = if(isLocal) "local tag \"${SQLLocalTags.getOriginalName(name, event.guild)}\""
        else "global tag \"${SQLGlobalTags.getOriginalName(name)}\""

        event.jda.retrieveUserById(ownerId).promise() then {
            if(it == null) event.replyError("The owner of $str was improperly retrieved!")
            else           event.replySuccess("The $str is owned by ${it.formattedName(true)}${
            if(!event.jda.users.contains(it)) " (ID: ${it.id})." else "."
            }")
            event.invokeCooldown()
        } catch {
            event.replyError("The owner of $str could not be retrieved for an unexpected reason!")
        }
    }
}

@MustHaveArguments
private class TagRawCmd : Command() {
    init {
        this.name = "Raw"
        this.fullname = "Tag Raw"
        this.arguments = "[Tag Name]"
        this.help = "Gets the raw, non-parsed form of a tag."
        this.documentation =
            "It's worth noting that if a user owns both the local and global version " +
            "of a tag when using this command on a server, the priority when getting content goes " +
            "to the *local* version, not the global one."
        this.guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Arguments.commandArgs, 2)
        val name = parts[0]
        if(event.isFromType(ChannelType.TEXT)) {
            val content: String = when {
                SQLLocalTags.isTag(name, event.guild) -> SQLLocalTags.getTagContent(name, event.guild)
                SQLGlobalTags.isTag(name) -> SQLGlobalTags.getTagContent(name)
                else -> ""
            }
            if(content.isEmpty())
                event.replyError("**No Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.client.prefix,fullname)}")
            else
                event.reply("```\n$content```")
        } else {
            val content = if(SQLGlobalTags.isTag(name)) SQLGlobalTags.getTagContent(name) else ""

            if(content.isEmpty())
                event.replyError("**No Global Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.client.prefix,fullname)}")
            else event.reply("```\n$content```")
        }
    }
}

@MustHaveArguments
private class TagOverrideCmd : Command() {
    init {
        this.name = "Override"
        this.fullname = "Tag Override"
        this.arguments = "[Tag Name] [Tag Content]"
        this.help = "Overrides a local or global tag."
        this.documentation =
            "It's worth noting that if a user owns both the local and global version " +
            "of a tag when using this command on a server, the priority when overriding goes " +
            "to the *local* version, not the global one."
        this.category = Category.MODERATOR
        this.guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val parts = event.args.split(Regex("\\s+"),2)

        val name = if(parts[0].length<=50)
            parts[0]
        else
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.client.prefix, fullname))

        val newContent = when {
            parts.size==1 ->
                return event.replyError("**You must specify content when overriding a tag!**\n" +
                        SEE_HELP.format(event.client.prefix, fullname))
            parts[1].length>1900 ->
                return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                        SEE_HELP.format(event.client.prefix, fullname))
            else -> parts[1]
        }

        if(!SQLLocalTags.isTag(name,event.guild)) {
            if(SQLGlobalTags.isTag(name)) {
                event.replyError("Tag named \"$name\" does not exist!")
            } else {
                val ownerId = SQLGlobalTags.getTagOwnerId(name)
                val member = event.guild.getMemberById(ownerId)
                if(member!=null && (member.isOwner || event.member.canInteract(member)))
                    return event.replyError("I cannot override the global tag \"**$name**\" because " +
                            "you are not able to interact with them due to role hierarchy placement!")
                SQLLocalTags.overrideTag(newContent, SQLGlobalTags.getOriginalName(name), ownerId, event.guild)
                event.replySuccess("Successfully overrode global tag \"**$name**\"!")
            }
        } else {
            val member = event.guild.getMemberById(SQLGlobalTags.getTagOwnerId(name))
            if(member!=null && (member.isOwner || event.member.canInteract(member)))
                return event.replyError("I cannot override the global tag \"**$name**\" because " +
                        "you are not able to interact with them due to role hierarchy placement!")
            SQLLocalTags.addTag(SQLGlobalTags.getOriginalName(name), 1L, newContent, event.guild)
            event.replySuccess("Successfully overrode global tag \"**$name**\"!")
        }
    }
}