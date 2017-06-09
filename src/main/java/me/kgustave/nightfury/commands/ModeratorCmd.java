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

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.SearcherUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

/**
 *
 * @author Kaidan Gustave
 */
public class ModeratorCmd extends NightFuryCommand {
    
    public ModeratorCmd(NightFuryManager manager) {
        super(manager);
        this.name = "moderator";
        this.aliases = new String[]{"mod"};
        this.arguments = "[add|remove|set] <arguments>";
        this.guildOnly = true;
        this.help = "add, remove, and manage moderators";
        this.category = manager.ADMINISTRATOR;
        this.children = new NightFuryCommand[]{
                new AddCmd(manager),
                new RemoveCmd(manager),
                new SetCmd(manager)
        };
    }
    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty())
            event.replyError(FormatUtils.tooFewArgs(this.getName()));
        else
            event.replyError(FormatUtils.invalidArgs(this.getName()));
    }
    
    private class AddCmd extends NightFuryCommand {

        public AddCmd(NightFuryManager manager) {
            super(manager);
            this.name = "add";
            this.arguments = "<User mention or ID>";
            this.help = "adds a user as a moderator";
            this.guildOnly = true;
            this.category = manager.ADMINISTRATOR;
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        }

        @Override
        protected void execute(CommandEvent event) {
            Guild guild = event.getGuild();
            Settings settings = manager.getSettingsForGuild(guild);
            if(settings.hasModId()) {
                Role role = guild.getRoleById(settings.getModId());
                if(event.getSelfMember().canInteract(role)) {
                    if(event.getArgs().isEmpty()) {
                        event.replyError(FormatUtils.tooFewArgs("moderator add"));
                    } else if(event.getArgs().trim().matches(FormatUtils.TARGET_ID_REGEX)) {
                        String id = event.getArgs().trim().replaceAll(FormatUtils.TARGET_ID_REGEX, "$2");
                        Member member = guild.getMemberById(id);
                        if(member!=null) {
                            String name = FormatUtils.formattedUserName(member.getUser(), true);
                            if(!member.getRoles().contains(role)) {
                                guild.getController().addRolesToMember(member, role).reason("Appointed by "+FormatUtils.formattedUserName(event.getAuthor(), false))
                                .queue(v -> event.replySuccess("Successfully added "+name+" as a moderator!"));
                            } else
                                event.replyError("Could not add "+name+" because they already are a moderator!");
                        } else
                            event.replyError("Could not find user on this server with ID "+id+"!");
                    } else
                        event.replyError(FormatUtils.invalidArgs("moderator add"));
                } else
                    event.replyError(FormatUtils.errorWithHelp("Cannot interact with moderator role!", "moderator add"));
            } else
                event.replyError(FormatUtils.errorWithHelp("Moderator Role has not been set up!", "moderator add"));
        }
    }
    
    private class RemoveCmd extends NightFuryCommand {

        public RemoveCmd(NightFuryManager manager) {
            super(manager);
            this.name = "remove";
            this.arguments = "<User mention or ID>";
            this.help = "removes a user as a moderator";
            this.guildOnly = true;
            this.category = manager.ADMINISTRATOR;
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        }

        @Override
        protected void execute(CommandEvent event) {
            Guild guild = event.getGuild();
            Settings settings = manager.getSettingsForGuild(guild);
            if(settings.hasModId()) {
                Role role = guild.getRoleById(settings.getModId());
                if(event.getSelfMember().canInteract(role)) {
                    if(event.getArgs().isEmpty()) {
                        event.replyError(FormatUtils.tooFewArgs("moderator remove"));
                    } else if(event.getArgs().trim().matches(FormatUtils.TARGET_ID_REGEX)) {
                        String id = event.getArgs().trim().replaceAll(FormatUtils.TARGET_ID_REGEX, "$2");
                        Member member = guild.getMemberById(id);
                        if(member!=null) {
                            String name = FormatUtils.formattedUserName(member.getUser(), true);
                            if(member.getRoles().contains(role)) {
                                guild.getController().removeRolesFromMember(member, role).reason("Retired by "+FormatUtils.formattedUserName(event.getAuthor(), false))
                                .queue(v -> event.replySuccess("Successfully removed "+name+" as a moderator!"));
                            } else
                                event.replyError("Could not remove "+name+" because they are not a moderator!");
                        } else
                            event.replyError("Could not find user on this server with ID "+id+"!");
                    } else
                        event.replyError(FormatUtils.invalidArgs("moderator remove"));
                } else
                    event.replyError(FormatUtils.errorWithHelp("Cannot interact with moderator role!", "moderator add"));
            } else
                event.replyError(FormatUtils.errorWithHelp("Moderator Role has not been set up!", "moderator add"));
        }
    }
    
    private class SetCmd extends NightFuryCommand {

        public SetCmd(NightFuryManager manager) {
            super(manager);
            this.name = "set";
            this.arguments = "<Role name, mention or ID>";
            this.help = "sets the moderator role to use this bot's commands";
            this.guildOnly = true;
            this.category = manager.ADMINISTRATOR;
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        }

        @Override
        protected void execute(CommandEvent event) {
            if(event.getArgs().isEmpty()) {
                event.replyError(FormatUtils.tooFewArgs("moderator set"));
                return;
            }
            Guild guild = event.getGuild();
            Settings settings = manager.getSettingsForGuild(guild);
            List<Role> roles = SearcherUtil.searchRole(event.getArgs(), guild);
            int size = roles.size();
            if(size>1) {
                event.replyError(FormatUtils.multipleRolesFound(event.getArgs(), roles));
            } else if(roles.isEmpty()) {
                event.replyError("Could not find role matching \""+event.getArgs()+"\"!");
            } else {
                Role target = roles.get(0);
                String res = "Successfully set \""+target.getName()+"\" as moderator role!";
                settings.setModId(target.getIdLong());
                if(event.getSelfMember().canInteract(target))
                    event.replySuccess(res);
                else
                    event.replyWarning(res+"\n**Please be advised:** due to role higherarchy I may not be able to perform certain commands having to due with the moderator role!");
            }
        }
        
    }
}