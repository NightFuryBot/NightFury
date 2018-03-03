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
package xyz.nightfury.command.standard

import xyz.nightfury.command.AutoCooldown
import xyz.nightfury.command.Command
import xyz.nightfury.command.CommandContext
import xyz.nightfury.command.MustHaveArguments
import xyz.nightfury.entities.TagErrorException
import xyz.nightfury.util.menus.Paginator
import xyz.nightfury.listeners.EventWaiter
import xyz.nightfury.ndb.entities.DBTag
import xyz.nightfury.util.commandArgs
import xyz.nightfury.util.db.addTag
import xyz.nightfury.util.db.getTagByName
import xyz.nightfury.util.db.isTag
import xyz.nightfury.util.db.tags
import xyz.nightfury.util.ext.*
import xyz.nightfury.util.ignored

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class TagCommand(waiter: EventWaiter): Command(StandardGroup) {
    companion object {
        private const val NAME_MAX_LENGTH = 30
        private const val CONTENT_MAX_LENGTH = 1900
    }

    override val name = "Tag"
    override val aliases = arrayOf("T")
    override val arguments = "[Tag Name] <Tag Arguments>"
    override val help = "Calls a tag."
    override val guildOnly = false
    override val children = arrayOf(
        TagCreateCommand(),
        TagCreateGlobalCommand(),
        TagDeleteCommand(),
        TagEditCommand(),
        TagListCommand(waiter),
        TagOwnerCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val parts = ctx.args.split(commandArgs, 2)

        val name = if(parts[0] in ctx.client.commands) {
            return ctx.reply("You remember Shengaero's words: *\"Not everything is a tag!\"*")
        } else parts[0]

        val args = if(parts.size > 1) parts[1].trim() else ""
        val parser = ctx.client.parser

        // Clear parser
        parser.clear()

        // Load default parser data
        parser.put("args", args)
        parser.put("user", ctx.author)

        val tag = if(ctx.isGuild) {
            // Load guild specific parser data
            parser.put("guild", ctx.guild)
            parser.put("channel", ctx.channel)

            // Get guild scope tag
            ctx.guild.getTagByName(name)
        } else ctx.jda.getTagByName(name) // Get global scope tag

        if(tag === null) {
            return ctx.replyError("The tag \"$name\" does not exist or could not be found.")
        }

        val output = try {
            parser.parse(tag.content)
        } catch(e: TagErrorException) {
            return ctx.replyError(e.message ?: "Tag \"$name\" could not be processed for an unknown reason!")
        }

        ctx.reply(output)
    }

    @MustHaveArguments
    private inner class TagCreateCommand : Command(this@TagCommand) {
        override val name = "Create"
        override val arguments = "[Tag Name] [Tag Content]"
        override val help = "Creates a new local tag."
        override val guildOnly = true
        override val cooldown = 120
        override val cooldownScope = CooldownScope.USER_GUILD

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)
            val name = parts[0]
            val content = if(parts.size > 1) parts[1] else return ctx.missingArgs {
                "You must specify a new tag name and it's content in the format `$arguments`."
            }

            if(name.length > NAME_MAX_LENGTH) return ctx.replyError {
                "Tag names must be no greater than $NAME_MAX_LENGTH characters long."
            }

            if(content.length > CONTENT_MAX_LENGTH) return ctx.replyError {
                "Tag content must be no greater than $CONTENT_MAX_LENGTH characters long."
            }

            val guild = ctx.guild

            if(guild.isTag(name)) {
                // The tag already exists
                return ctx.replyError("A local tag named $name already exists on this guild!")
            }

            val tag = DBTag(name, content, ctx.author.idLong, guild.idLong)

            guild.addTag(tag)

            ctx.replySuccess("Successfully created local tag \"$name\"!")

            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments
    private inner class TagCreateGlobalCommand : Command(this@TagCommand) {
        override val name = "CreateGlobal"
        override val arguments = "Creates a new global tag."
        override val help = "Creates a new global tag."
        override val guildOnly = false
        override val cooldown = 120
        override val cooldownScope = CooldownScope.USER

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)
            val name = parts[0]
            val content = if(parts.size > 1) parts[1] else return ctx.missingArgs {
                "You must specify a new tag name and it's content in the format `$arguments`."
            }


            if(name.length > NAME_MAX_LENGTH) return ctx.replyError {
                "Tag names must be no greater than $NAME_MAX_LENGTH characters long."
            }

            if(content.length > CONTENT_MAX_LENGTH) return ctx.replyError {
                "Tag content must be no greater than $CONTENT_MAX_LENGTH characters long."
            }

            if(ctx.jda.isTag(name)) {
                // The tag already exists
                return ctx.replyError("A global tag named $name already exists!")
            }

            val tag = DBTag(name, content, ctx.author.idLong, null)

            ctx.jda.addTag(tag)

            ctx.replySuccess("Successfully created global tag \"$name\"!")

            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments
    private inner class TagDeleteCommand : Command(this@TagCommand) {
        override val name = "Delete"
        override val arguments = "[Tag Name]"
        override val help = "Deletes a tag you own."
        override val guildOnly = false

        override suspend fun execute(ctx: CommandContext) {
            val name = ctx.args

            val tag = if(ctx.isGuild) ctx.guild.getTagByName(name) else ctx.jda.getTagByName(name)

            if(tag === null) {
                return ctx.replyError("Unable to find tag named \"$name\".")
            }

            // Future changes might require that this name be directly
            // taken from the database. As such, we save an instance before
            // deleting it to make sure this isn't affected later on.
            val tagName = tag.name

            if(tag.ownerId != ctx.author.idLong) return ctx.replyError {
                "Cannot delete tag \"$tagName\" because you are not the owner of the tag!"
            }

            tag.delete()
            ctx.replySuccess("Successfully deleted tag \"$tagName\"")
        }
    }

    @MustHaveArguments
    private inner class TagEditCommand : Command(this@TagCommand) {
        override val name = "Edit"
        override val arguments = "[Tag Name] [Tag Content]"
        override val help = "Creates a new local tag."
        override val guildOnly = true
        override val cooldown = 30
        override val cooldownScope = CooldownScope.USER

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)
            val name = parts[0]
            val content = if(parts.size > 1) parts[1] else return ctx.missingArgs {
                "You must specify an existing tag name and it's new content in the format `$arguments`."
            }

            val tag = if(ctx.isGuild) ctx.guild.getTagByName(name) else ctx.jda.getTagByName(name)

            if(tag === null) {
                return ctx.replyError("Unable to find tag named \"$name\".")
            }

            if(content.length > CONTENT_MAX_LENGTH) return ctx.replyError {
                "Tag content must be no greater than $CONTENT_MAX_LENGTH characters long."
            }

            val tagName = tag.name

            if(tag.ownerId != ctx.author.idLong) return ctx.replyError {
                "Cannot edit tag \"$tagName\" because you are not the owner of the tag!"
            }

            tag.content = content
            ctx.replySuccess("Successfully edited tag \"$tagName\"")
            ctx.invokeCooldown()
        }
    }

    @AutoCooldown
    private inner class TagListCommand(waiter: EventWaiter): Command(this@TagCommand) {
        override val name = "List"
        override val arguments = "<User>"
        override val help = "Gets a list of tags owned by a user."
        override val guildOnly = false
        override val cooldown = 10

        private val builder = Paginator.Builder {
            waiter           { waiter }
            timeout          { delay { 20 } }
            showPageNumbers  { true }
            numberItems      { true }
            waitOnSinglePage { true }
        }

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val temp = if(ctx.isGuild) {
                if(query.isEmpty()) {
                    ctx.member
                } else {
                    val found = ctx.guild.findMembers(query)
                    when {
                        found.isEmpty() -> null
                        found.size > 1 -> return ctx.replyError(found.multipleMembers(query))
                        else -> found[0]
                    }
                }
            } else null

            val user = when {
                temp !== null -> temp.user
                query.isEmpty() -> ctx.author
                else -> {
                    val found = ctx.jda.findUsers(query)
                    when {
                        found.isEmpty() -> return ctx.replyError(noMatch("users", query))
                        found.size > 1 -> return ctx.replyError(found.multipleUsers(query))
                        else -> found[0]
                    }
                }
            }

            val member = if(temp === null && ctx.isGuild) ctx.guild.getMember(user) else temp

            val localTags = member?.let { member.tags }?.map { "${it.name} (Local)" }
            val globalTags = user.tags.map { "${it.name} (Global)" }

            if((localTags === null || localTags.isEmpty()) && globalTags.isEmpty()) {
                return ctx.replyError("${if(ctx.author == user) "You do" else "${user.formattedName(false)} does"} not have any tags!")
            }

            builder.clearItems()

            with(builder) {
                text { _, _ -> "Tags owned by ${user.formattedName(true)}" }
                items {
                    localTags?.let { if(localTags.isNotEmpty()) addAll(localTags) }
                    addAll(globalTags)
                }
                finalAction {
                    ctx.linkMessage(it)
                    it.removeMenuReactions()
                }
                user { ctx.author }
            }

            Paginator(builder).displayIn(ctx.channel)
        }
    }

    @MustHaveArguments
    private inner class TagOwnerCommand : Command(this@TagCommand) {
        override val name = "Owner"
        override val arguments = "[Tag Name]"
        override val help = "Gets the owner of a tag by name."
        override val cooldown = 10

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val tag = if(ctx.isGuild) ctx.guild.getTagByName(query) else ctx.jda.getTagByName(query)

            if(tag === null) {
                return ctx.replyError("Unable to find tag matching \"$query\".")
            }

            val ownerId = tag.ownerId

            // This is due to an override either by the server or by me
            if(ownerId === null) {
                val message = "The ${if(tag.isGlobal) "global" else "local"} tag \"${tag.name}\" has no owner."
                return ctx.replySuccess(message)
            }

            val owner = ignored(null) { ctx.jda.retrieveUserById(ownerId).await() }

            if(owner === null) {
                return ctx.replyError("Unable to retrieve the owner of tag \"${tag.name}\"!")
            }

            val message = "The ${if(tag.isGlobal) "global" else "local"} tag \"${tag.name}\" " +
                          "is owned by ${owner.formattedName(true)}."

            ctx.replySuccess(message)
            ctx.invokeCooldown()
        }
    }
}
