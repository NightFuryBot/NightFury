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
package me.kgustave.nightfury.commands;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.OtherUtils;
import me.kgustave.nightfury.utils.SearcherUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author Kaidan Gustave
 */
public class InfoCmd extends NightFuryCommand {
    
    private static final String BULLET = "\uD83D\uDD39 ";
    
    public InfoCmd(NightFuryManager manager) {
        super(manager);
        this.name = "info";
        this.aliases = new String[]{"i", "information"};
        this.arguments = "<user>";
        this.help = "gets info on a user";
        this.cooldown = 5;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs();
        if(args.isEmpty()) {
            executeInfo(event, event.getMember());
        } else {
            List<Member> found = SearcherUtil.searchMembers(args, event.getGuild());
            if(found.size()==1) {
                executeInfo(event, found.get(0));
            } else if(found.size()>1) {
                event.replyError(FormatUtils.multipleMembersFound(args, found));
            } else {
                event.replyError("No users found!");
            }
        }
    }
    
    public void executeInfo(CommandEvent event, Member member) {
        EmbedBuilder b = new EmbedBuilder();
        User user = member.getUser();
        b.setTitle((user.isBot()? event.getJDA().getEmoteById(230105988211015680L).getAsMention() :"\u2139")
                + " __Information on "+FormatUtils.formattedUserName(user, false)+":__");
        b.setThumbnail(user.getAvatarUrl()==null? user.getDefaultAvatarUrl() : user.getAvatarUrl());
        b.setColor(member.getColor());
        b.appendDescription(BULLET+"**ID:** "+user.getId()+"\n");
        if(member.getNickname()!=null)
            b.appendDescription(BULLET+"**Nickname:** "+member.getNickname()+"\n");
        List<Role> roles = member.getRoles();
        int size=roles.size();
        if(roles.size()>0) {
            b.appendDescription(BULLET+"**Role"+(size>1?"s:** ":":** "));
            b.appendDescription("`"+roles.get(0).getName()+"`");
            if(size>1)
                for(int i=1; i<size; i++)
                    b.appendDescription(", `"+roles.get(i).getName()+"`");
            b.appendDescription("\n");
        }
        b.appendDescription(BULLET+"**Status:** ");
        if(member.getGame()!=null) {
            if(member.getGame().getUrl()!=null) {
                b.appendDescription(event.getJDA().getEmoteById(313956277132853248L).getAsMention());
                b.appendDescription(" Streaming** ["+member.getGame().getName().replaceAll("\\*", "*")+"]("+member.getGame().getUrl()+") **");
            } else {
                b.appendDescription(event.getJDA().getEmoteById(OtherUtils.getStatusEmote(member.getOnlineStatus())).getAsMention());
                b.appendDescription(" Playing** "+member.getGame().getName().replaceAll("\\*", "*")+" **");
            }
        } else {
            b.appendDescription(event.getJDA().getEmoteById(OtherUtils.getStatusEmote(member.getOnlineStatus())).getAsMention()+" *"+member.getOnlineStatus().name()+"*");
        }
        b.appendDescription("\n");
        b.appendDescription(BULLET+"**Creation Date:** ").appendDescription(user.getCreationTime().format(DateTimeFormatter.ISO_LOCAL_DATE)+"\n");
        List<Member> joins = new ArrayList<>(event.getGuild().getMembers());
        Collections.sort(joins, (Member a, Member c) -> a.getJoinDate().compareTo(c.getJoinDate()));
        int index = joins.indexOf(member);
        b.appendDescription(BULLET+"**Join Date:** ").appendDescription(member.getJoinDate().format(DateTimeFormatter.ISO_LOCAL_DATE)+" `[#"+(index+1)+"]`\n");
        b.appendDescription(BULLET+"**Join Order:**\n");
        index-=3;
        if(index<0)
            index = 0;
        if(joins.get(index).equals(member))
            b.appendDescription("**["+user.getName()+"]()**");
        else
            b.appendDescription(joins.get(index).getUser().getName());
        for(int i=index+1;i<index+7;i++) {
            if(i>=joins.size())
                break;
            Member m = joins.get(i);
            String name = m.getUser().getName();
            if(m.equals(member))
                name="**["+name+"]()**";
            b.appendDescription(" > "+name);
        }
        event.reply(b.build());
    }
}
