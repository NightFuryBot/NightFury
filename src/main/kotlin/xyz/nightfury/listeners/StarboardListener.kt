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
package xyz.nightfury.listeners

// I FUCKING DID IT
// LOOK MOM, I FUCKING MADE A STARBOARD!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
// - Kaidan 2017
//
// P.S. Yes I actually yelled this out at the top of my lungs when I finished this.

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.nightfury.entities.starboard.StarboardHandler
import xyz.nightfury.entities.starboard.isStarReaction
import xyz.nightfury.entities.promise

/**
 * @author Kaidan Gustave
 */
class StarboardListener : EventListener {
    override fun onEvent(event: Event?) {
        if(event !is GenericGuildMessageEvent)
            return

        val guild: Guild = event.guild
        val starboard = StarboardHandler.getStarboard(guild) ?: return

        if(!guild.selfMember.hasPermission(event.channel, Permission.MESSAGE_MANAGE))
            return

        if(event is GuildMessageDeleteEvent) {
            starboard[event.messageIdLong]?.delete()
        } else if(event is GenericGuildMessageReactionEvent) {

            event.reaction.takeIf { it.isStarReaction } ?: return

            when(event) {
                is GuildMessageReactionAddEvent -> {
                    val starMessage = starboard[event.messageIdLong]
                    if(starMessage == null) {
                        event.channel.getMessageById(event.messageId).promise() then { it ?: return@then
                            starboard.addStar(event.user, it)
                        }
                    } else {
                        if(starMessage.isStarring(event.user))
                            return
                        starMessage.addStar(event.user)
                    }
                }

                is GuildMessageReactionRemoveEvent -> {
                    val starMessage = synchronized(starboard) { starboard[event.messageIdLong] ?: return }

                    launch {
                        val updated = run(coroutineContext) {
                            try {
                                event.channel.getMessageById(event.messageIdLong).complete()
                            } catch(e: Exception) { null }
                        } ?: return@launch
                        var noneRemain = true

                        @Suppress("LoopToCallChain") // Nice try foxbot.
                        for(reaction in updated.reactions) {
                            if(reaction.users.any(event.user::equals)) {
                                noneRemain = false
                                break
                            }
                        }

                        if(noneRemain)
                            starMessage.removeStar(event.user)
                    }
                }

                is GuildMessageReactionRemoveAllEvent -> {
                    starboard.deletedMessage((event as GuildMessageReactionRemoveAllEvent).messageIdLong)
                }
            }
        }
    }
}