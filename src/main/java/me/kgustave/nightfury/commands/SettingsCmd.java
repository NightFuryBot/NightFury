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

import org.json.JSONArray;

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author Kaidan Gustave
 */
public class SettingsCmd extends NightFuryCommand {

    public SettingsCmd(NightFuryManager manager) {
        super(manager);
        this.name = "settings";
        this.help = "manage and get info on the server's settings";
        this.aliases = new String[]{"config", "configurations"};
        this.guildOnly = true;
        this.category = manager.MODERATOR;
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        Settings settings = manager.getSettingsForGuild(guild);
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(String.format("Settings for %s (ID: %d)", guild.getName(), guild.getIdLong()), null, guild.getIconUrl());
        builder.setColor(event.getSelfMember().getColor());
        JSONArray prefixes = settings.getPrefixes();
        String pre = "";
        for(int i=0; i<prefixes.length(); i++) {
            pre += "`"+prefixes.getString(i)+"`";
            if(i!=prefixes.length()-1)
                pre += ", ";
        }
        builder.addField("Prefixes", pre, true);
        builder.addField("Moderator Role", (settings.hasModId()? guild.getRoleById(settings.getModId()).getName() : "None"), true);
        builder.addField("Mod Log", (settings.hasModLogId()? guild.getTextChannelById(settings.getModLogId()).getAsMention() : "None"), true);
        builder.addField("Muted Role", (settings.hasMutedId()? guild.getRoleById(settings.getMutedId()).getName() : "None"), true);
        builder.addField("Cases", settings.getCasesJSON().length()+" cases", true);
        event.reply(builder.build());
    }
}
