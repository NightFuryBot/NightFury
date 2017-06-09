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

import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.menu.orderedmenu.OrderedMenuBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;

import me.kgustave.nightfury.manager.NightFuryManager;

/**
 *
 * @author Kaidan Gustave
 */
public class ServerCmd extends NightFuryCommand {
    
    private final OrderedMenuBuilder b;
    
    public ServerCmd(NightFuryManager manager, EventWaiter waiter) {
        super(manager);
        this.name = "server";
        this.aliases = new String[]{"guild"};
        this.help = "gets info on the server";
        this.guildOnly = true;
        this.b = new OrderedMenuBuilder()
                .setDescription("Choose a field to get info on:")
                .setTimeout(20, TimeUnit.SECONDS)
                .setEventWaiter(waiter);
    }
    
    @Override
    protected void execute(CommandEvent event) {
        b.allowTextInput(true);
        if(manager.MODERATOR.test(event))
            b.setChoices("Owner", "Moderators", "Settings");
        else
            b.setChoices("Owner", "Moderators");
        b.setAction(i -> {
            switch(i)
            {
                case 1:
                {
                    ((InfoCmd) event.getClient().getCommands().stream()
                            .filter(command -> command.isCommandFor("info")).findAny().get()).executeInfo(event, event.getGuild().getOwner());
                    break;
                }
                case 2:
                {
                    ((ModsCmd) event.getClient().getCommands().stream()
                            .filter(command -> command.isCommandFor("mods")).findAny().get()).execute(event);
                    break;
                }
                case 3:
                {
                    ((SettingsCmd) event.getClient().getCommands().stream()
                            .filter(command -> command.isCommandFor("settings")).findAny().get()).execute(event);
                    break;
                }
            }
        });
        b.addUsers(event.getAuthor());
        b.setColor(event.getSelfMember().getColor());
        b.build().display(event.getChannel());
    }

}
