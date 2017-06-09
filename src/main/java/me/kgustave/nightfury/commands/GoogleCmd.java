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

import java.util.List;

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.api.GoogleAPI;
import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.utils.FormatUtils;

/**
 *
 * @author Kaidan Gustave
 */
public class GoogleCmd extends NightFuryCommand {
    
    private final GoogleAPI api;
    
    public GoogleCmd(NightFuryManager manager, GoogleAPI api) {
        super(manager);
        this.name = "google";
        this.aliases = new String[]{"g"};
        this.arguments = "[query]";
        this.help = "searches google";
        this.cooldown = 30;
        this.guildOnly = false;
        this.api = api;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        String query = event.getArgs();
        if(query.isEmpty()) {
            event.replyError(FormatUtils.tooFewArgs(this.getName()));
        } else {
            List<String> results = api.search(query);
            event.getChannel().sendTyping().queue(v -> {
                if(results == null) {
                    event.replyError("An unexpected error occured while searching!");
                } else if(results.size()<1) {
                    event.replyError("No results were found for \""+query+"\"!");
                } else {
                    event.replySuccess(event.getAuthor().getAsMention()+" "+results.get(0));
                }
            });
        }
    }
}
