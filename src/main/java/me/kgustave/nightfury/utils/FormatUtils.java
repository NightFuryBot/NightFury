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

import java.util.List;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author Kaidan Gustave
 */
public class FormatUtils {
    
    public static final String TARGET_ID_REGEX = "(<@)?!?(\\d+)>?";
    public static final String TARGET_ID_REASON_REGEX = "(<@)?!?(\\d+)>?((\\s+for)?([\\s|\\S]+))?";
    public static final String TARGET_ID_REASON_FAIL_REGEX = "(@)?([\\s|\\S]+)#(\\d{4})((\\s+for)?([\\s|\\S]+))?";
    
    // FORMATTING
    public static final String CASE_FORMAT = "`[  CASE  ]` `[%d]` %s %s %s **%s** (ID: %d)\n`[ REASON ]` %s";
    
    public static String formattedUserName(User user, boolean boldUsername) {
        return (boldUsername? "**"+user.getName()+"**" : user.getName())+"#"+user.getDiscriminator();
    }
    
    
    // COMMAND FAILURES
    public static String invalidArgs(String command) {
        return errorWithHelp("Invalid Arguments Provided!", command);
    }
    
    public static String tooFewArgs(String command) {
        return errorWithHelp("Too Few Arguments!", command);
    }

    public static String multipleUsersFound(String argument, List<User> users) {
        String str = "Multiple users found matching \""+argument+"\":\n";
        for(int i=0; i<users.size() || i<5; i++) {
            str += formattedUserName(users.get(i),true)+"\n";
            if(i==4 && users.size()>4)
                str += "And "+(users.size()-4)+" other user"+(users.size()-4>1?"s...":"...");
        }
        return str.trim();
    }
    
    public static String multipleMembersFound(String argument, List<Member> members) {
        String str = "Multiple users found matching \""+argument+"\":\n";
        for(int i=0; i<members.size() || i<5; i++) {
            str += formattedUserName(members.get(i).getUser(),true)+"\n";
            if(i==4 && members.size()>4)
                str += "And "+(members.size()-4)+" other member"+(members.size()-4>1?"s...":"...");
        }
        return str.trim();
    }
    
    public static String multipleRolesFound(String argument, List<Role> roles) {
        String str = "Multiple roles found matching \""+argument+"\":\n";
        for(int i=0; i<roles.size() || i<5; i++) {
            str += roles.get(0).getName()+" (ID: "+roles.get(0).getId()+")\n";
            if(i==4 && roles.size()>4)
                str += "And "+(roles.size()-4)+" other role"+(roles.size()-4>1?"s...":"...");
        }
        return str.trim();
    }
    
    public static String noMatch(String lookedFor, String query) {
        return "Could not find any "+lookedFor+" matching \""+query+"\"!";
    }
    
    public static String errorWithSuggestion(String error, String suggestion) {
        return String.format("**%s**\n%s", error, suggestion);
    }
    
    public static String errorWithHelp(String error, String command) {
        return errorWithSuggestion(error, "Use `|"+command+" help` for more information on this command.");
    }
    
    public static String errorWithoutSuggestion(String error) {
        return "**"+error+"**";
    }
}
