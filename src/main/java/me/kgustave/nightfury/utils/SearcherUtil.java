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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;

/**
 *
 * @author Kaidan Gustave
 */
public class SearcherUtil {
    
    public final static String USER_MENTION = "<@!?(\\d+)>";
    
    public static List<User> searchUsers(String query, JDA jda)
    {
        String id;
        String discrim = null;
        if(query.matches(USER_MENTION))
        {
            id = query.replaceAll(USER_MENTION, "$1");
            User u = jda.getUserById(id);
            if(u!=null)
                return Collections.singletonList(u);
            else {
                try {
                    return Collections.singletonList(jda.retrieveUserById(id).complete());
                } catch(Exception e) {
                    return new ArrayList<>();
                }
            }
        }
        else if(query.matches("^.*#\\d{4}$"))
        {
            discrim = query.substring(query.length()-4);
            query = query.substring(0,query.length()-5).trim();
        }
        ArrayList<User> exact = new ArrayList<>();
        ArrayList<User> wrongcase = new ArrayList<>();
        ArrayList<User> startswith = new ArrayList<>();
        ArrayList<User> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for(User u: jda.getUsers())
        {
            if(discrim!=null && !u.getDiscriminator().equals(discrim))
                continue;
            if(u.getName().equals(query))
                exact.add(u);
            else if (exact.isEmpty() && u.getName().equalsIgnoreCase(query))
                wrongcase.add(u);
            else if (wrongcase.isEmpty() && u.getName().toLowerCase().startsWith(lowerQuery))
                startswith.add(u);
            else if (startswith.isEmpty() && u.getName().toLowerCase().contains(lowerQuery))
                contains.add(u);
        }
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<Member> searchMembers(String query, Guild guild)
    {
        String id;
        String discrim = null;
        if(query.matches(USER_MENTION))
        {
            id = query.replaceAll(USER_MENTION, "$1");
            Member m = guild.getMemberById(id);
            if(m!=null)
                return Collections.singletonList(m);
        }
        else if(query.matches("^.*#\\d{4}$"))
        {
            discrim = query.substring(query.length()-4);
            query = query.substring(0,query.length()-5).trim();
        }
        ArrayList<Member> exact = new ArrayList<>();
        ArrayList<Member> wrongcase = new ArrayList<>();
        ArrayList<Member> startswith = new ArrayList<>();
        ArrayList<Member> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for(Member m: guild.getMembers())
        {
            User u = m.getUser();
            String nickname = m.getNickname();
            if(discrim!=null && !u.getDiscriminator().equals(discrim))
                continue;
            if(u.getName().equals(query) || (nickname!=null && nickname.equals(query)))
                exact.add(m);
            else if (exact.isEmpty() && (u.getName().equalsIgnoreCase(query) || (nickname!=null && nickname.equalsIgnoreCase(query))))
                wrongcase.add(m);
            else if (wrongcase.isEmpty() && (u.getName().toLowerCase().startsWith(lowerQuery) || (nickname!=null && nickname.toLowerCase().startsWith(lowerQuery))))
                startswith.add(m);
            else if (startswith.isEmpty() && (u.getName().toLowerCase().contains(lowerQuery) || (nickname!=null && nickname.toLowerCase().contains(lowerQuery))))
                contains.add(m);
        }
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<User> searchBannedUsers(String query, Guild guild)
    {
        List<User> bans;
        try{
            bans = guild.getBans().complete();
        } catch(Exception e) {
            return null;
        }
        String id;
        String discrim = null;
        if(query.matches(USER_MENTION))
        {
            id = query.replaceAll(USER_MENTION, "$1");
            User u = guild.getJDA().getUserById(id);
            if(bans.contains(u))
                return Collections.singletonList(u);
            for(User user : bans)
                if(user.getId().equals(id))
                    return Collections.singletonList(user);
        }
        else if(query.matches("^.*#\\d{4}$"))
        {
            discrim = query.substring(query.length()-4);
            query = query.substring(0,query.length()-5).trim();
        }
        ArrayList<User> exact = new ArrayList<>();
        ArrayList<User> wrongcase = new ArrayList<>();
        ArrayList<User> startswith = new ArrayList<>();
        ArrayList<User> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for(User u: bans)
        {
            if(discrim!=null && !u.getDiscriminator().equals(discrim))
                continue;
            if(u.getName().equals(query))
                exact.add(u);
            else if (exact.isEmpty() && u.getName().equalsIgnoreCase(query))
                wrongcase.add(u);
            else if (wrongcase.isEmpty() && u.getName().toLowerCase().startsWith(lowerQuery))
                startswith.add(u);
            else if (startswith.isEmpty() && u.getName().toLowerCase().contains(lowerQuery))
                contains.add(u);
        }
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<TextChannel> searchTextChannel(String query, Guild guild)
    {
        String id;
        if(query.matches("<#\\d+>"))
        {
            id = query.replaceAll("<#(\\d+)>", "$1");
            TextChannel tc = guild.getJDA().getTextChannelById(id);
            if(tc!=null && tc.getGuild().equals(guild))
                return Collections.singletonList(tc);
        }
        ArrayList<TextChannel> exact = new ArrayList<>();
        ArrayList<TextChannel> wrongcase = new ArrayList<>();
        ArrayList<TextChannel> startswith = new ArrayList<>();
        ArrayList<TextChannel> contains = new ArrayList<>();
        String lowerquery = query.toLowerCase();
        guild.getTextChannels().stream().forEach((tc) -> {
            if(tc.getName().equals(lowerquery))
                exact.add(tc);
            else if (tc.getName().equalsIgnoreCase(lowerquery) && exact.isEmpty())
                wrongcase.add(tc);
            else if (tc.getName().toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty())
                startswith.add(tc);
            else if (tc.getName().toLowerCase().contains(lowerquery) && startswith.isEmpty())
                contains.add(tc);
        });
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<VoiceChannel> searchVoiceChannel(String query, Guild guild)
    {
        String id;
        if(query.matches("<#\\d+>"))
        {
            id = query.replaceAll("<#(\\d+)>", "$1");
            VoiceChannel vc = guild.getJDA().getVoiceChannelById(id);
            if(vc!=null && vc.getGuild().equals(guild))
                return Collections.singletonList(vc);
        }
        ArrayList<VoiceChannel> exact = new ArrayList<>();
        ArrayList<VoiceChannel> wrongcase = new ArrayList<>();
        ArrayList<VoiceChannel> startswith = new ArrayList<>();
        ArrayList<VoiceChannel> contains = new ArrayList<>();
        String lowerquery = query.toLowerCase();
        guild.getVoiceChannels().stream().forEach((vc) -> {
            if(vc.getName().equals(lowerquery))
                exact.add(vc);
            else if (vc.getName().equalsIgnoreCase(lowerquery) && exact.isEmpty())
                wrongcase.add(vc);
            else if (vc.getName().toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty())
                startswith.add(vc);
            else if (vc.getName().toLowerCase().contains(lowerquery) && startswith.isEmpty())
                contains.add(vc);
        });
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<Role> searchRole(String query, Guild guild)
    {
        String id;
        if(query.matches("<@&\\d+>"))
        {
            id = query.replaceAll("<@&(\\d+)>", "$1");
            Role r = guild.getRoleById(id);
            if(r!=null)
                return Collections.singletonList(r);
        }
        if(query.matches("[Ii][Dd]\\s*:\\s*\\d+"))
        {
            id = query.replaceAll("[Ii][Dd]\\s*:\\s*(\\d+)", "$1");
            Role r = guild.getRoleById(id);
            if(r!=null)
                return Collections.singletonList(r);
        }
        ArrayList<Role> exact = new ArrayList<>();
        ArrayList<Role> wrongcase = new ArrayList<>();
        ArrayList<Role> startswith = new ArrayList<>();
        ArrayList<Role> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        guild.getRoles().stream().forEach((role) -> {
            if(role.getName().equals(query))
                exact.add(role);
            else if (role.getName().equalsIgnoreCase(query) && exact.isEmpty())
                wrongcase.add(role);
            else if (role.getName().toLowerCase().startsWith(lowerQuery) && wrongcase.isEmpty())
                startswith.add(role);
            else if (role.getName().toLowerCase().contains(lowerQuery) && startswith.isEmpty())
                contains.add(role);
        });
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<Guild> searchGuild(String query, JDA jda)
    {
        String id;
        if(query.matches("[Ii][Dd]\\s*:\\s*\\d+"))
        {
            id = query.replaceAll("[Ii][Dd]\\s*:\\s*(\\d+)", "$1");
            Guild g = jda.getGuildById(id);
            if(g!=null)
                return Collections.singletonList(g);
        }
        ArrayList<Guild> exact = new ArrayList<>();
        ArrayList<Guild> wrongcase = new ArrayList<>();
        ArrayList<Guild> startswith = new ArrayList<>();
        ArrayList<Guild> contains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        jda.getGuilds().stream().forEach((guild) -> {
            if(guild.getName().equals(query))
                exact.add(guild);
            else if (guild.getName().equalsIgnoreCase(query) && exact.isEmpty())
                wrongcase.add(guild);
            else if (guild.getName().toLowerCase().startsWith(lowerQuery) && wrongcase.isEmpty())
                startswith.add(guild);
            else if (guild.getName().toLowerCase().contains(lowerQuery) && startswith.isEmpty())
                contains.add(guild);
        });
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
}
