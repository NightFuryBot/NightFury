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

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nashtag.NashTagBuilder;
import me.kgustave.nashtag.NashTagParser;
import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nashtag.NashTagException;

/**
 *
 * @author Kaidan Gustave
 */
public class EvalCmd extends Command {
    
    private final NightFuryManager manager;
    
    public EvalCmd(NightFuryManager manager) {
        this.name = "eval";
        this.help = "evals using NashTag";
        this.guildOnly = false;
        this.ownerCommand = true;
        this.category = manager.OWNER;
        this.arguments = "[script]";
        this.manager = manager;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs();
        NashTagParser se = new NashTagBuilder()
        .put("event", event)
        .put("jda", event.getJDA())
        .put("guild", event.getGuild())
        .put("channel", event.getChannel())
        .put("client", event.getClient())
        .put("manager", manager)
        .put("settings", manager.getSettingsForGuild(event.getGuild()))
        .build();
        try {
            event.reply("```java\n" + args + " ```" + "Evaluated:\n```\n" + se.eval(args)+" ```");
        } catch(NashTagException e) {
            event.reply("```java\n" + args + " ```" + "A ParseException was thrown:\n```\n" + e.getMessage() + " ```");
        } catch(Exception e) {
            event.reply("```java\n" + args + " ```" + "An exception was thrown:\n```\n" + e + " ```");
        }
    }

}
