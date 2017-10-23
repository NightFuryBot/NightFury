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
package me.kgustave.nightfury.entities.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.AppenderBase
import me.kgustave.nightfury.entities.KEmbedBuilder
import net.dv8tion.jda.webhook.WebhookClient
import net.dv8tion.jda.webhook.WebhookClientBuilder
import java.awt.Color
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.*

/**
 * @author Kaidan Gustave
 */
class WebhookAppender: AppenderBase<ILoggingEvent>()
{
    private val client: WebhookClient

    init {
        val lines = Paths.get(System.getProperty("user.dir"), "webhook.txt").toFile().readLines()
        client = WebhookClientBuilder(lines[0].toLong(), lines[1])
                .setThreadFactory { Thread(it, "WebhookLogger").apply { isDaemon = true } }
                .build()
    }

    override fun append(event: ILoggingEvent?)
    {
        if(event == null) return

        try {
            client.send {
                title { event.loggerName.split(packageRegex).run { this[size - 1] } }
                append(event.formattedMessage)
                val proxy = event.throwableProxy
                if(proxy != null)
                    append("\n\n${buildStackTrace(proxy)}")
                color { colorFromLevel(event.level) }
                footer { value = "Logged at" }
                time {
                    val gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
                    gmt.timeInMillis = event.timeStamp
                    OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
                }
            }
        } catch(e: Throwable) {
            return e.printStackTrace()
        }
    }

    override fun stop() {
        super.stop()
        client.close()
    }

    companion object {
        private val packageRegex: Regex = Regex("\\.")

        private fun colorFromLevel(level: Level) = when(level) {
            Level.INFO  -> Color.BLUE
            Level.WARN  -> Color.ORANGE
            Level.ERROR -> Color.RED
            Level.DEBUG -> Color.YELLOW
            else        -> null
        }

        private val EMBED_LIMIT = 750

        fun buildStackTrace(proxy: IThrowableProxy) = buildString {
            append("```java\n")
            append(proxy.className)
            val message = proxy.message

            if(message != null)
                append(": $message")
            append("\n")

            val arr = proxy.stackTraceElementProxyArray
            for((index, element) in arr.withIndex())
            {
                val str = element.steAsString
                if(str.length + length > EMBED_LIMIT)
                {
                    append("\t... (${arr.size - index + 1} more calls)")
                    break
                }
                append("\t$str\n")
            }
            append("```")
        }
    }

    private fun WebhookClient.send(embed: KEmbedBuilder.() -> Unit) = with(KEmbedBuilder()) {
        embed()
        send(this.build())
    }
}
