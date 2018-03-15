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
@file:Suppress("Unused")
package xyz.nightfury.util.jda

import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import net.dv8tion.jda.core.utils.SessionController
import net.dv8tion.jda.core.utils.SessionControllerAdapter
import okhttp3.OkHttpClient
import xyz.nightfury.NightFury
import xyz.nightfury.util.listeningTo
import xyz.nightfury.util.playing
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

inline fun client(accountType: AccountType, init: JDABuilder.() -> Unit): JDA {
    return JDABuilder(accountType).apply(init).buildAsync()
}

inline fun client(accountType: AccountType, shards: Int, lazy: JDABuilder.() -> Unit): ReceiveChannel<JDA> {
    val builder = JDABuilder(accountType)
    builder.lazy()
    return produce {
        builder.sessionController { SessionControllerAdapter() }
        for(i in 0 until shards) {
            builder.useSharding(i, shards)
            NightFury.LOG.info("Shard [$i / ${shards - 1}] now building...")
            launch(coroutineContext) {
                val shard = builder.buildSuspended()
                send(shard)
            }
            delay(5, TimeUnit.SECONDS) // Five second backoff
        }
    }
}

inline fun <reified T: JDABuilder> T.token(lazy: () -> String): T = apply { setToken(lazy()) }
inline fun <reified T: JDABuilder> T.game(lazy: () -> String): T = apply { setGame(playing(lazy())) }
inline fun <reified T: JDABuilder> T.listening(lazy: () -> String): T = apply { setGame(listeningTo(lazy())) }
inline fun <reified T: JDABuilder> T.watching(lazy: () -> String): T = apply { setGame(xyz.nightfury.util.watching(lazy())) }
inline fun <reified T: JDABuilder> T.status(lazy: () -> OnlineStatus): T = apply { setStatus(lazy()) }
inline fun <reified T: JDABuilder> T.manager(lazy: () -> IEventManager): T = apply { setEventManager(lazy()) }
inline fun <reified T: JDABuilder> T.listener(lazy: () -> Any): T = apply { addEventListener(lazy()) }
inline fun <reified T: JDABuilder> T.audioSendFactory(lazy: () -> IAudioSendFactory): T = apply { setAudioSendFactory(lazy()) }
inline fun <reified T: JDABuilder> T.idle(lazy: () -> Boolean): T = apply { setIdle(lazy()) }
inline fun <reified T: JDABuilder> T.shutdownHook(lazy: () -> Boolean): T = apply { setEnableShutdownHook(lazy()) }
inline fun <reified T: JDABuilder> T.audio(lazy: () -> Boolean): T = apply { setAudioEnabled(lazy()) }
inline fun <reified T: JDABuilder> T.autoReconnect(lazy: () -> Boolean): T = apply { setAutoReconnect(lazy()) }
inline fun <reified T: JDABuilder> T.contextMap(lazy: () -> ConcurrentMap<String, String>?): T = apply { setContextMap(lazy()) }
inline fun <reified T: JDABuilder> T.sessionController(lazy: () -> SessionController): T = apply { setSessionController(lazy()) }
inline fun <reified T: JDABuilder> T.removeListener(vararg listener: Any): T = apply { removeEventListener(*listener) }
inline fun <reified T: JDABuilder> T.webSocketFactory(factory: WebSocketFactory = WebSocketFactory(),
                                                      init: WebSocketFactory.() -> Unit): T = apply { setWebsocketFactory(factory.apply(init)) }
inline fun <reified T: JDABuilder> T.httpSettings(builder: OkHttpClient.Builder = OkHttpClient.Builder(),
                                                  init: OkHttpClient.Builder.() -> Unit = {}): T = apply { setHttpClientBuilder(builder.apply(init)) }

suspend inline fun <reified T: JDABuilder> T.buildSuspended(): JDA = suspendCoroutine { cont ->
    on<ReadyEvent> {
        cont.resume(it.jda)
    }
}

inline fun <reified E: Event> JDABuilder.on(crossinline handle: (E) -> Unit): JDABuilder = listener {
    EventListener {
        if(it is E) {
            handle(it)
        }
    }
}
