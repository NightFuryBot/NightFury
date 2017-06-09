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

/**
 *
 * @author Kaidan Gustave
 */
public class ShutdownCmd extends NightFuryCommand {

    /**
     * @param manager
     */
    public ShutdownCmd(NightFuryManager manager) {
        super(manager);
        this.name = "shutdown";
        this.help = "shuts down this instance of the bot";
        this.ownerCommand = true;
        this.category = manager.OWNER;
        this.guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        event.replyWarning("Shutting down...");
        synchronized(this)
        {
            manager.shutdown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}
        }
        event.getJDA().shutdown();
    }

}
