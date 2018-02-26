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
package xyz.nightfury.listeners

// I FUCKING DID IT
// LOOK MOM, I FUCKING MADE A STARBOARD!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
// - Kaidan 2017
//
// P.S. Yes I actually yelled this out at the top of my lungs when I finished this.

import kotlinx.coroutines.experimental.CoroutineScope
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import xyz.nightfury.entities.await
import xyz.nightfury.entities.starboard.isStarReaction
import xyz.nightfury.util.db.starboard
import xyz.nightfury.util.ignored

/**
 * @author Kaidan Gustave
 */
class StarboardListener : SuspendedListener {
    override suspend fun CoroutineScope.onEvent(event: Event) {
        if(event !is GenericGuildMessageEvent)
            return

        val guild = event.guild
        val starboard = guild.starboard ?: return

        if(!guild.selfMember.hasPermission(event.channel, Permission.MESSAGE_MANAGE))
            return

        if(event is GuildMessageDeleteEvent) {
            starboard[event.messageIdLong]?.delete()
        } else if(event is GenericGuildMessageReactionEvent) {
            event.reaction.takeIf { it.isStarReaction } ?: return
            when(event) {
                is GuildMessageReactionAddEvent -> {
                    val starMessage = starboard[event.messageIdLong]
                    if(starMessage === null) {
                        val message = event.channel.getMessageById(event.messageIdLong).await()
                        message?.let { starboard.addStar(event.user, message) }
                    } else {
                        if(starMessage.isStarring(event.user))
                            return
                        starMessage.addStar(event.user)
                    }
                }

                is GuildMessageReactionRemoveEvent -> {
                    // Shouldn't ever be null
                    val starMessage = starboard[event.messageIdLong] ?: return
                    val updated = ignored(null) { event.channel.getMessageById(event.messageIdLong).await() } ?: return

                    var noneRemain = true
                    @Suppress("LoopToCallChain") // Nice try foxbot.
                    for(reaction in updated.reactions) {
                        if(reaction.users.any(event.user::equals)) {
                            noneRemain = false
                            break
                        }
                    }

                    if(noneRemain) {
                        starMessage.removeStar(event.user)
                    }
                }
            }
        } else if(event is GuildMessageReactionRemoveAllEvent) {
            // If all the reactions are removed we just delete the message
            // because that means that all the reactions were removed.
            starboard.deletedMessage(event.messageIdLong)
        }
    }
}
