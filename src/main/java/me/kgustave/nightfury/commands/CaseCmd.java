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

/**
 *
 * @author Kaidan Gustave
 */
public class CaseCmd extends NightFuryCommand {

    /**
     * @param manager
     */
    public CaseCmd(NightFuryManager manager) {
        super(manager);
        this.name = "case";
        this.arguments = "[case number]";
        this.help = "gets a case report";
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        if(settings.hasModLogId()) {
            if(event.getArgs().matches("\\d+")) {
                int number = Integer.parseInt(event.getArgs());
                if(number>0 && settings.getCurrentCaseNumber()>=number) {
                    long msgId = settings.getMessageIdForCase(number);
                    event.getGuild().getTextChannelById(settings.getModLogId()).getMessageById(msgId).queue(msg -> {
                        
                    });
                }
            }
        }
    }

}
