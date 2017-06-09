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
package me.kgustave.nightfury.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.jagrosh.jdautilities.commandclient.Command.Category;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.SimpleLog;

/**
 * A simplistic Manager for {@link Settings} objects.
 * 
 * @author Kaidan Gustave
 */
public class NightFuryManager {
    
    private final HashMap<Long,Settings> settingsMap = new HashMap<>();
    private final File settingsFile;
    private final ScheduledExecutorService executor;
    private final Runnable overwrite;
    
    public final Category OWNER;
    public final Category SERVER_OWNER;
    public final Category ADMINISTRATOR;
    public final Category MODERATOR;
    
    private boolean setToWrite = false;
    
    public static final SimpleLog LOG = SimpleLog.getLog("NightFuryManager");
    
    public NightFuryManager(int shard, String root, ScheduledExecutorService executor) {
        
        if(shard!=-1)
            this.settingsFile = new File(root+File.separatorChar+"SettingsShard"+shard+".json");
        else
            this.settingsFile = new File(root+File.separatorChar+"Settings.json");
        
        this.executor = executor;
        
        this.overwrite = () -> {
            try {
                JSONObject newGuildsJSON = new JSONObject();
                synchronized(settingsMap) {
                    settingsMap.keySet().forEach((l) -> {
                        newGuildsJSON.put(l.toString(), settingsMap.get(l).getGuildJSON().toString());
                    });
                }
                Files.write(settingsFile.toPath(), newGuildsJSON.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                Files.copy(settingsFile.toPath(), Paths.get(root,"Backups", settingsFile.getName()),
                        StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                setToWrite = false;
            } catch (JSONException|IOException e) {
                LOG.fatal(e);
            }
        };
        
        if(!settingsFile.exists())
            try {
                settingsFile.createNewFile();
                executor.execute(overwrite);
            } catch (IOException e) {}
        
        this.OWNER = new Category("Owner", (event) -> {
            return event.isOwner() || event.isCoOwner();
        });
        
        this.SERVER_OWNER = new Category("Server Owner", (event) -> {
            if(event.isFromType(ChannelType.PRIVATE))
                return true;
            else
                return this.OWNER.test(event) || event.getMember().isOwner();
        });
        
        this.ADMINISTRATOR = new Category("Administrator", (event) -> {
            if(event.isFromType(ChannelType.PRIVATE))
                return true;
            else
                return this.SERVER_OWNER.test(event) || event.getMember().hasPermission(Permission.ADMINISTRATOR);
        });
        
        this.MODERATOR = new Category("Moderator", (event) -> {
            if(event.isFromType(ChannelType.PRIVATE))
                return true;
            else if(!getSettingsById(event.getGuild().getIdLong()).hasModId())
                return false;
            else
                return this.ADMINISTRATOR.test(event) || 
                        event.getMember().getRoles().contains(event.getGuild().getRoleById(getSettingsById(event.getGuild().getIdLong()).getModId()));
        });
    }
    
    public File getFile() {
        return settingsFile;
    }
    
    public Settings getSettingsById(long guildId) {
        if(hasId(guildId)) {
            synchronized (settingsMap) {
                return settingsMap.get(guildId);
            }
        }
        else throw new IllegalArgumentException("Guild ID specified could not be found.");
    }
    
    public Settings getSettingsForGuild(Guild guild) {
        if(guild==null)
            return null;
        long guildId = guild.getIdLong();
        if(hasId(guildId)) {
            synchronized(settingsMap) {
                return settingsMap.get(guildId);
            }
        }
        else throw new IllegalArgumentException("Guild specified could not be found.");
    }
    
    public void createSettings(long guildId) {
        if(!hasId(guildId)) {
            put(new Settings(this, guildId, new JSONObject()));
        } 
        else throw new IllegalArgumentException("Attempted to create a Settings for a previously registered Guild ID.");
    }
    
    public void removeSettings(long guildId) {
        if(hasId(guildId)) {
            remove(guildId);
        }
        else throw new IllegalArgumentException("Attempted to remove a Settings for a unregistered Guild ID.");
    }
    
    public boolean hasId(long guildId) {
        synchronized(settingsMap) {
            return settingsMap.containsKey(guildId);
        }
    }
    
    public void put(Settings settings) {
        synchronized(settingsMap) {
            settingsMap.put(settings.getIdLong(), settings);
        }
        save();
    }
    
    public void remove(long guildId) {
        synchronized (settingsMap) {
            settingsMap.remove(guildId);
        }
        save();
    }
    
    public void remove(Settings settings) {
        remove(settings.getIdLong());
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public boolean readyLoader(JDA jda) {
        // Step one: leave bad guilds
        // Step two: auto-shard
            // Gets GuildSettings files
            // Goes through each of them and handles
            // All guilds from other shards now on this one are collected
        // Step three: handle data from this shard
        // Step four: handle new guilds w/o data
        // Step five: save
        
        // STEP 1
        // We want to synchronize this because we want to go into handling the loaded JSONs only after leaving bad guilds.
        // Not doing this could cause some bad guilds to not be accounted for 
        synchronized(jda) {
            jda.getGuilds().stream().filter(guild -> {
                if(guild.getMemberById(211393686628597761L)==null)
                    return false;
                int notReal = (int)guild.getMembers().stream().filter(member -> member.getUser().isBot() || member.getUser().isFake()).count();
                return (notReal>=20);
            }).forEach(guild -> {
                guild.leave();//.queue();
            });
        }
        
        File backupFolder = new File(settingsFile.getParentFile().getAbsolutePath()+File.separatorChar+"Backups");
        if(!backupFolder.exists())
            backupFolder.mkdirs();
        
        // STEP 2
        // This will automatically handle new shards
        if(jda.getShardInfo()!=null) {
            try {
                // The autosharding of data for NightFury is simple.
                // On shutdown of all shards, there is no more data being collected by the bot to any shard.
                // By collecting all of the data from each shard, and dumping it into a single directory for each hosting
                // NightFury can iterate through each file one by one and collect guilds
                for(File file : settingsFile.getParentFile().listFiles()) {
                    if(file.getName().matches("SettingsShard(\\d+).json")||file.getName().equals("Settings.json")) {
                        if(file.getName().matches("SettingsShard(\\d+).json") && 
                                Integer.parseInt(file.getName().replaceFirst("SettingsShard(\\d+).json", "$1"))==jda.getShardInfo().getShardId())
                            continue;
                        Files.copy(settingsFile.toPath(),
                                Paths.get(backupFolder.getPath(), settingsFile.getName()),
                                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                        synchronized(jda) {
                            handleLoadedJSON(jda, new JSONObject(new String(Files.readAllBytes(file.toPath()))));
                        }
                    }
                }
            } catch (IOException e) {
                LOG.fatal("Autosharding has failed! "+e);
                this.shutdown();
                return false;
            }
        }
        JSONObject guildsJSON;
        try {
            guildsJSON = new JSONObject(new String(Files.readAllBytes(settingsFile.toPath())));
        } catch(IOException e) {
            // If an IOException is thrown, this is most likely fine.
            // If the exception is a FileNotFoundException, it's rather normal
            // this means that there may just not be a file yet because this is a new shard.
            // However, if this is any other exception we're gonna have to resort to storing a backup...
            if(e instanceof NoSuchFileException) {
                LOG.info("No Guild Settings JSON detected!");
                guildsJSON = new JSONObject();
            } else {
                LOG.warn("An IOException was thrown! "+e);
                LOG.warn("Attempting to restore from backup!");
                guildsJSON = restoreBackup(backupFolder);
            }
        } catch(JSONException e) {
            // If a JSONException is thrown, then we have to try to restore from a backup.
            // This means that we may be able to run the bot, even if the data is shaky at best.
            LOG.warn("JSONException was thrown! "+e);
            LOG.warn("Attempting to restore from backup!");
            guildsJSON = restoreBackup(backupFolder);
        }
        // If the guilds JSON is null, that's basically the bot being out of ideas for getting data.
        if(guildsJSON==null) {
            this.shutdown();
            return false;
        }
        
        //Puts all visible Guilds that were saved. Removes ones that were left while offline.
        synchronized(jda) {
            handleLoadedJSON(jda, guildsJSON);
        }
        
        //Puts all new Visible Guilds that were joined while offline.
        jda.getGuilds().stream().filter((guild) -> {
            return !hasId(guild.getIdLong());
        }).forEach((guild) -> put(new Settings(this, guild.getIdLong(), new JSONObject())));
        
        executor.execute(overwrite);
        LOG.info("Successfully Loaded Guild Settings"+(jda.getShardInfo()==null? "!" : " for shard "+jda.getShardInfo().getShardString()+"!"));
        return true;
    }
    
    private JSONObject restoreBackup(File backupFolder) {
        try {
            return new JSONObject(new String(Files.readAllBytes(Paths.get(backupFolder.getPath(), settingsFile.getName()))));
        } catch (IOException | JSONException ex) {
            // If another JSONException or an IOException is thrown, then we're not in good enough shape to proceed.
            // Unlike most bots, NightFury's functionality rely's on well structured data, as well as not losing it.
            // Without any and all data, tons of errors will occur, so shutdown is just a quicker way to finish the job.
            return null;
        }
    }
    
    private void handleLoadedJSON(JDA jda, JSONObject guildsJSON) {
        guildsJSON.keySet().stream()
        .filter((key) -> {
            // Get only real saved guilds
            return jda.getGuildById(key)!=null;
        })
        .forEach((key) -> {
            Settings settings = new Settings(this, Long.parseLong(key), new JSONObject(guildsJSON.getString(key)));
            Guild guild = jda.getGuildById(settings.getIdLong());
            int notReal = (int)guild.getMembers().stream().filter(member -> member.getUser().isBot() || member.getUser().isFake()).count();
            if((notReal>=20) && guild.getMemberById(211393686628597761L)==null) {
                guild.leave().queue();
                return;
            }
            
            // Muted Role Check
            if(settings.hasMutedId()) {
                if(guild.getRoleById(settings.getMutedId())==null) {
                    settings.putMutedId(0L);
                } else {
                    Role muted = guild.getRoleById(settings.getMutedId());
                    guild.getTextChannels().forEach(tc -> {
                        if(tc.getPermissionOverride(muted)==null) {
                            tc.createPermissionOverride(muted).setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
                        }
                    });
                    guild.getVoiceChannels().forEach(vc -> {
                        if(vc.getPermissionOverride(muted)==null) {
                            vc.createPermissionOverride(muted).setAllow(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).queue();
                        }
                    });
                    settings.putClearMutes();
                    guild.getMembersWithRoles(muted).forEach(member -> {
                        settings.putMute(member.getUser().getIdLong());
                    });
                }
            }
            
            // RoleMe roles check
            if(settings.getRoleMeArray().length()>0)
            {
                for(Object id : settings.getRoleMeArray()) {
                    if(guild.getRoleById((long)id)==null)
                        settings.removeRoleMe((long)id);
                }
            }
            
            // ColorMe roles check
            if(settings.getColormeArray().length()>0) {
                for(Object id : settings.getColormeArray()) {
                    if(guild.getRoleById((long)id)==null)
                        settings.removeRoleMe((long)id);
                }
            }
            
            // Modlog check
            if(settings.hasModLogId() && guild.getTextChannelById(settings.getModLogId())==null)
                settings.putModLogId(0L).putNewCaseSystem();
            
            // Mod role check
            if(settings.hasModId() && guild.getRoleById(settings.getModId())==null)
                settings.putModId(0L);
            
            // Finished
            put(settings);
        });
    }
    
    private void save() {
        if(!setToWrite) {
            setToWrite = true;
            executor.schedule(overwrite, 30, TimeUnit.SECONDS);
        }
    }
}