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
package me.kgustave.nightfury.utils;

import net.dv8tion.jda.core.OnlineStatus;

/**
 *
 * @author Kaidan Gustave
 */
public class OtherUtils {
    
    // This is primarily used in the mods and info commands
    public static long getStatusEmote(OnlineStatus status) {
        switch(status) {
        case ONLINE : return 313956277808005120L;
        case IDLE : return 313956277220802560L;
        case DO_NOT_DISTURB : return 313956276893646850L;
        case OFFLINE : return 313956277237710868L;
        case INVISIBLE : return 313956277107556352L;
        case UNKNOWN : return 313956277107556352L;
        }
        return 0L;
    }

}
