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
package xyz.nightfury.resources

object Arguments {
    val commandArgs: Regex = Regex("\\s+")
    val emoteRegex: Regex = Regex("<:\\S{2,32}:(\\d+)>")
    val discordID: Regex = Regex("(\\d{17,20})")
    val userMention: Regex = Regex("<@!?(\\d{17,20})>")
    val reasonPattern: Regex = Regex("(^.+)\\s(?:for\\s+)([\\s\\S]+)$", RegexOption.DOT_MATCHES_ALL)
    val targetIDWithReason: Regex = Regex("(\\d{17,20})(?:\\s+(?:for\\s+)?([\\s\\S]+))?")
    val targetMentionWithReason: Regex = Regex("<@!?(\\d{17,20})>(?:\\s+(?:for\\s+)?([\\s\\S]+))?")
}