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
import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.kgustave.nightfury.Argument
import me.kgustave.nightfury.Command
import me.kgustave.nightfury.CommandEvent
import me.kgustave.nightfury.CooldownScope
import me.kgustave.nightfury.utils.formatUserName
import net.dv8tion.jda.core.entities.ChannelType


/**
 * @author Kaidan Gustave
 */
class TagCommand : Command()
{
    val parser : Parser = JagTag.newDefaultBuilder().build()

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
        this.children = arrayOf(TagCreateGlobalCmd(), TagCreateCmd(), TagDeleteCmd(), TagOwnerCmd()
        )
    }

    override fun execute(event: CommandEvent)
    {
        if(event.args.isEmpty())
            return event.replyError(INVALID_ARGS_ERROR.format("Try specifying a tag name in the format `${event.prefixUsed}tag [tag name]`."))
        val parts = event.args.split(Regex("\\s+"),2)
        val name = parts[0]
        val args = if(parts.size>1) parts[1] else ""
        if(event.isFromType(ChannelType.TEXT))
        {
            val content : String = if(event.client.manager.isLocalTag(name, event.guild)) {
                event.client.manager.getContentForLocalTag(name, event.guild)
            } else if(event.client.manager.isGlobalTag(name)) {
                event.client.manager.getContentForGlobalTag(name)
            } else ""
            if(content.isEmpty())
                return event.replyError("**No Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.prefixUsed,this.name)}")
            else event.reply(parser.put("args", args).parse(content))
        }
        else
        {
            val content : String = if(event.client.manager.isGlobalTag(name)) {
                event.client.manager.getContentForGlobalTag(name)
            } else ""

            if(content.isEmpty())
                return event.replyError("**No Global Tag Found Matching \"$name\"**\n${SEE_HELP.format(event.prefixUsed,this.name)}")
            else event.reply(parser.put("args", args).parse(content))
            parser.clear()
        }
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

        val name = if(parts[0].length<=50)
            parts[0]
        else
            return event.replyError("**Tag names cannot exceed 50 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a tag!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        if(event.client.manager.isLocalTag(name, event.guild) || event.client.manager.isGlobalTag(name))
            return event.replyError("Tag named \"$name\" already exists!")
        else {
            event.client.manager.addLocalTag(name,content,event.member)
            event.replySuccess("Successfully created local tag \"**$name**\" on ${event.guild.name}!")
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
            with(event.client.manager) {
                if(!isLocalTag(name, event.guild)) {
                    if(!isGlobalTag(name)) {
                        event.replyError("Tag named \"$name\" does not exist!")
                    } else if(isGlobalTagOwner(name, event.author)) {
                        deleteGlobalTag(name, event.author)
                        event.replySuccess("Successfully deleted local tag \"**$name**\"!")
                    } else {
                        event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                                SEE_HELP.format(event.prefixUsed, fullname))
                    }
                } else if(isLocalTagOwner(name, event.member)) {
                    deleteLocalTag(name, event.member)
                    event.replySuccess("Successfully deleted local tag \"**$name**\"!")
                } else {
                    event.replyError("**You cannot delete the local tag \"$name\" because you are not it's owner!**\n" +
                            SEE_HELP.format(event.prefixUsed, fullname))
                }
            }
        } else {
            with(event.client.manager)
            {
                if(!isGlobalTag(name)) {
                    event.replyError("Tag named \"$name\" does not exist!")
                } else if(isGlobalTagOwner(name, event.author)) {
                    deleteGlobalTag(name, event.author)
                    event.replySuccess("Successfully deleted local tag \"**$name**\"!")
                } else {
                    event.replyError("**You cannot delete the global tag \"$name\" because you are not it's owner!**\n" +
                            SEE_HELP.format(event.prefixUsed, fullname))
                }
            }
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
        else parts[0]

        val content = if(parts.size==1)
            return event.replyError("**You must specify content when creating a tag!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else if(parts[1].length>1900)
            return event.replyError("**Tag content cannot exceed 1900 characters in length!**\n" +
                    SEE_HELP.format(event.prefixUsed, fullname))
        else parts[1]

        if((event.isFromType(ChannelType.TEXT) && event.client.manager.isLocalTag(name, event.guild))
                || event.client.manager.isGlobalTag(name))
            return event.replyError("Tag named \"$name\" already exists!")
        else {
            event.client.manager.addGlobalTag(name,content,event.author)
            event.replySuccess("Successfully created global tag \"**$name**\"!")
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
        val ownerId : Long = with(event.client.manager)
        {
            if(event.isFromType(ChannelType.TEXT))
                if(isLocalTag(name, event.guild))
                    return@with getOwnerIdForLocalTag(name, event.guild)
            if(isGlobalTag(name))
                return@with getOwnerIdForGlobalTag(name)
            return@with 0L
        }
        if(ownerId == 0L) return event.replyError("Tag named \"$name\" does not exist!")

        event.jda.retrieveUserById(ownerId).promise() then {
            if(it == null)
                return@then event.replyError("The owner of ${
                if(event.client.manager.isGlobalTag(name))
                    "global tag \"${event.client.manager.getOriginalNameOfGlobalTag(name)}\""
                else
                    "local tag \"${event.client.manager.getOriginalNameOfLocalTag(name, event.guild)}\""
                } was improperly retrieved!")
            event.replySuccess("The ${
            if(event.client.manager.isGlobalTag(name))
                "global tag \"${event.client.manager.getOriginalNameOfGlobalTag(name)}\""
            else
                "local tag \"${event.client.manager.getOriginalNameOfLocalTag(name, event.guild)}\""
            } is owned by ${formatUserName(it, true)}")
        } catch {
            event.replyError("The owner of ${
            if(event.client.manager.isGlobalTag(name))
                "global tag \"${event.client.manager.getOriginalNameOfGlobalTag(name)}\""
            else
                "local tag \"${event.client.manager.getOriginalNameOfLocalTag(name, event.guild)}\""
            } could not be retrieved for an unexpected reason!")
        }
    }
}