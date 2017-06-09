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
package me.kgustave.nightfury.listeners;

import java.time.OffsetDateTime;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 *
 * @author Kaidan Gustave
 */
public class SettingsListener extends ListenerAdapter {
    
    private final NightFuryManager manager;
    
    public SettingsListener(NightFuryManager manager) {
        this.manager = manager;
    }
    
    // Ready Loader
    @Override
    public void onReady(ReadyEvent event) {
        boolean loaded = manager.readyLoader(event.getJDA());
        if(!loaded)
            event.getJDA().shutdown();
    }
    
    // Handle joining guilds
    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        int notReal = (int)event.getGuild().getMembers().stream().filter(member -> member.getUser().isBot() || member.getUser().isFake()).count();
        if((notReal>=20) && event.getGuild().getMemberById(211393686628597761L)==null)
            event.getGuild().leave().queue();
        if(!manager.hasId(event.getGuild().getIdLong()) && event.getGuild().getSelfMember().getJoinDate().plusMinutes(5).isAfter(OffsetDateTime.now()))
            manager.createSettings(event.getGuild().getIdLong());
    }

    // Handle leaving guilds
    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        if(manager.hasId(event.getGuild().getIdLong()))
            manager.removeSettings(event.getGuild().getIdLong());
    }
    
    // Handle manual deletion of important roles
    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        long id = event.getRole().getIdLong();
        
        // Muted Role
        if(settings.hasMutedId() && settings.getMutedId()==id)
            settings.setMutedId(0L);
        
        // Moderator Role
        if(settings.hasModId() && settings.getModId()==id)
            settings.setModId(0L);
        
        // RoleMe Roles
        if(settings.isRoleMe(id))
            settings.removeRoleMe(id);
        
        // ColorMe Roles
        if(settings.isColorme(id))
            settings.removeColorme(id);
    }

    // Handle creation of new channels regarding muted role
    @Override
    public void onTextChannelCreate(TextChannelCreateEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        
        if(settings.hasMutedId())
            event.getChannel().createPermissionOverride(event.getGuild().getRoleById(settings.getMutedId()))
            .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
    }
    
    @Override
    public void onVoiceChannelCreate(VoiceChannelCreateEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        
        if(settings.hasMutedId())
            event.getChannel().createPermissionOverride(event.getGuild().getRoleById(settings.getMutedId()))
            .setDeny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).queue();
    }
    
    // Handle manual deletion of Mod Log and reset case number to 0
    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasModLogId() && settings.getModLogId()==event.getChannel().getIdLong())
            settings.putModLogId(0L).resetCaseSystem();
    }
    
    @Override
    public void onReconnect(ReconnectedEvent event) {
        event.getJDA().getGuilds().stream().filter(guild -> !manager.hasId(guild.getIdLong())).forEach(guild -> manager.createSettings(guild.getIdLong()));
    }
    
    // Handle Shutdown
    @Override
    public void onShutdown(ShutdownEvent event) {
        manager.shutdown();
    }
    
}
