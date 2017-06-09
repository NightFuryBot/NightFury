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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author Kaidan Gustave
 */
public class Config {
    
    private final String token;
    private final String ownerId;
    private final String shutdownKey;
    private final String dBotsKey;
    private final String prefix;
    private final String success;
    private final String warning;
    private final String error;
    private final String server;
    private final Permission[] permissions;
    private final String description;
    private final String[] features;
    private final Color color;
    private final ScheduledExecutorService executor;
    private final File root;
    private static final SimpleLog LOG = SimpleLog.getLog("NightFury");
    
    public Config(File key) {
        List<String> config = null;
        try {
            config = Files.readAllLines(key.toPath());
        } catch (IOException e) {
            LOG.warn("The configurations file for NightFury was not found! "+e);
            System.exit(1);
        }
        try {
            this.token = config.get(0);
            this.ownerId = config.get(1);
            this.shutdownKey = config.get(2);
            this.dBotsKey = config.get(3);
        } catch(IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("One of the necessary values in configuration file was not found!");
        }
        this.prefix = "|";
        this.success = "\uD83D\uDC32";
        this.warning = "\uD83D\uDC22";
        this.error = "\uD83D\uDD25";
        this.server = "https://discord.gg/xkkw54u";
        this.permissions = new Permission[]{
                Permission.BAN_MEMBERS,
                Permission.KICK_MEMBERS,
                Permission.MESSAGE_MANAGE,
                Permission.MANAGE_ROLES,
                Permission.MESSAGE_ADD_REACTION,
                Permission.VOICE_MOVE_OTHERS,
                Permission.VOICE_MUTE_OTHERS,
                Permission.MESSAGE_EMBED_LINKS
        };
        this.description = "Simple moderation bot.";
        this.features = new String[]{
                "Easy moderation",
                "Logs (coming soon)"
        };
        this.color = Color.ORANGE;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.root = key.getParentFile();
    }

    public String getToken() {
        return token;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getShutdownKey() {
        return shutdownKey;
    }

    public String getDBotsKey() {
        return dBotsKey;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuccess() {
        return success;
    }

    public String getWarning() {
        return warning;
    }

    public String getError() {
        return error;
    }

    public String getServer() {
        return server;
    }

    public Permission[] getPermissions() {
        return permissions;
    }

    public String getDescription() {
        return description;
    }

    public String[] getFeatures() {
        return features;
    }

    public Color getColor() {
        return color;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public File getRoot() {
        return root;
    }
    
    public String getRootPath() {
        return root.getAbsolutePath();
    }
}
