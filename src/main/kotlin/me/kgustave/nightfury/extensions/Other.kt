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
package me.kgustave.nightfury.extensions

import net.dv8tion.jda.core.OnlineStatus

val OnlineStatus.emoteId : Long
    get() = when(this)
    {
        OnlineStatus.ONLINE -> 313956277808005120L
        OnlineStatus.IDLE -> 313956277220802560L
        OnlineStatus.DO_NOT_DISTURB -> 313956276893646850L
        OnlineStatus.OFFLINE -> 313956277237710868L
        OnlineStatus.INVISIBLE -> 313956277107556352L
        OnlineStatus.UNKNOWN -> 313956277107556352L
    }
fun getEmoteIdFor(status: OnlineStatus) = status.emoteId

infix fun Int.randomNextInt(int: Int): Int
{
    require( this >=0 ) {    "Cannot use negative numbers as receiver in random range!"    }
    require(this < int) {            "Parameter must be greater than receiver!"            }

    return this + (Math.random() * (int - this + 1)).toInt()
}