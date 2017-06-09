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

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.OtherUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

/**
 *
 * @author Kaidan Gustave
 */
public class ModsCmd extends NightFuryCommand {

    /**
     * @param manager
     */
    public ModsCmd(NightFuryManager manager) {
        super(manager);
        this.name = "mods";
        this.aliases = new String[]{"moderators"};
        this.help = "lists all moderators online";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        Settings settings = manager.getSettingsForGuild(guild);
        boolean lookForOnline = event.getArgs().equalsIgnoreCase("online");
        if(settings.hasModId()) {
            EmbedBuilder b = new EmbedBuilder();
            Role role = guild.getRoleById(settings.getModId());
            b.setTitle("Moderators"+(lookForOnline? " Online" : ""));
            b.setColor(role.getColor());
            guild.getMembers().stream().filter(member -> {
                return (lookForOnline? member.getOnlineStatus().equals(OnlineStatus.ONLINE) : true) && !member.getUser().isBot();
            }).filter(member -> {
                return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) || member.getRoles().contains(role);
            }).sorted((a,c) -> {
                return a.getJoinDate().compareTo(c.getJoinDate());
            }).forEach(mod -> {
                b.appendDescription(event.getJDA().getEmoteById(OtherUtils.getStatusEmote(mod.getOnlineStatus())).getAsMention()+" ");
                b.appendDescription(FormatUtils.formattedUserName(mod.getUser(), true));
                if(mod.isOwner())
                    b.appendDescription(" `[OWNER]`\n");
                else if(mod.hasPermission(Permission.ADMINISTRATOR))
                    b.appendDescription(" `[ADMIN]`\n");
                else
                    b.appendDescription(" `[MOD]`\n");
            });
            event.reply(b.build());
        } else {
            event.replyError("Could not detect any moderators!");
        }
    }

}
