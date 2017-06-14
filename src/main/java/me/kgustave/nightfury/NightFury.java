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
package me.kgustave.nightfury;

import java.nio.file.Paths;

import javax.security.auth.login.LoginException;

import com.jagrosh.jdautilities.commandclient.CommandClient;
import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.commandclient.examples.PingCommand;
import com.jagrosh.jdautilities.waiter.EventWaiter;

import me.kgustave.nightfury.api.E621API;
import me.kgustave.nightfury.api.GoogleAPI;
import me.kgustave.nightfury.commands.*;
import me.kgustave.nightfury.listeners.LogsListener;
import me.kgustave.nightfury.listeners.SettingsListener;
import me.kgustave.nightfury.manager.NightFuryManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author Kaidan Gustave
 */
public class NightFury {
    
    public static final SimpleLog LOG = SimpleLog.getLog("NightFury");
    
    public static void main(String[] args) throws LoginException, IllegalArgumentException, RateLimitedException
    {
        LOG.info("Starting NightFury...");

        Config config = new Config(Paths.get(System.getProperty("user.home"),"Desktop","NightFury-Data","config.txt").toFile());
        
        EventWaiter waiter = new EventWaiter();
        
        JDA jda = new JDABuilder(AccountType.BOT)
                .setToken(config.getToken())
                .addEventListener(waiter)
                .buildAsync();
        
        // Load Manager
        NightFuryManager manager;
        synchronized(jda) {
            manager = new NightFuryManager(jda.getShardInfo()==null? -1 : jda.getShardInfo().getShardId(), config.getRootPath(), config.getExecutor());
        }
        
        synchronized(jda) {
            jda.addEventListener(new SettingsListener(manager), new LogsListener(manager));
        }
        
        // APIs
        GoogleAPI gapi = new GoogleAPI();
        E621API eapi = new E621API();
        
        CommandClient client = new CommandClientBuilder()
                .setOwnerId(config.getOwnerId())
                .useDefaultGame()
                .setDiscordBotsKey(config.getDBotsKey())
                .setEmojis(config.getSuccess(), config.getWarning(), config.getError())
                .setPrefix(config.getPrefix())
                .useHelpBuilder(false)
                .addCommands(
                        // Regular Commands
                        new AboutCmd(manager,config.getColor(),config.getDescription(),config.getFeatures(),config.getPermissions()),
                        new ColorMeCmd(manager),
                        new GoogleCmd(manager, gapi),
                        new HelpCmd(manager),
                        new InfoCmd(manager),
                        new ModsCmd(manager),
                        new PingCommand(),
                        new RoleMeCmd(manager),
                        new ServerCmd(manager, waiter),
                        new E621Cmd(manager,eapi,waiter),
                        
                        // Moderator Commands
                        new BanCmd(manager),
                        new CleanCmd(manager),
                        new KickCmd(manager),
                        new MuteCmd(manager),
                        new ReasonCmd(manager),
                        new SettingsCmd(manager),
                        new UnmuteCmd(manager),
                        
                        // Admin Commands
                        new ModeratorCmd(manager),
                        
                        // Dev Commands
                        new EvalCmd(manager),
                        new ShutdownCmd(manager)
                        )
                .setScheduleExecutor(config.getExecutor())
                .setServerInvite(config.getServer())
                .build();
        
        jda.addEventListener(client);
    }
}
