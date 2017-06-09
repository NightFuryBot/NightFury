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
import java.util.stream.Collectors;

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import me.kgustave.nightfury.utils.SearcherUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

/**
 *
 * @author Kaidan Gustave
 */
public class RoleMeCmd extends NightFuryCommand {

    public RoleMeCmd(NightFuryManager manager) {
        super(manager);
        this.name = "roleme";
        this.arguments = "[role]";
        this.help = "gives you a roleme role or removes one you already have";
        this.guildOnly = true;
        this.cooldown = 30;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.children = new NightFuryCommand[]{
                new AddCmd(manager),
                new RemoveCmd(manager)
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        String query = event.getArgs();
        
        if(settings.getRoleMeArray().length()<1)
        {
            event.replyError(FormatUtils.errorWithHelp("No RoleMe roles on this server!", this.getName()));
            return;
        }
        if(query.isEmpty())
        {
            event.replyError(FormatUtils.tooFewArgs(this.getName()));
            return;
        }
        
        List<Role> roles = SearcherUtil.searchRole(query, event.getGuild());
        List<Role> rolemes = roles.stream().filter(role -> settings.isRoleMe(role.getIdLong())).collect(Collectors.toList());
        
        if(rolemes.size()<1) {
            if(roles.size()<1)
                event.replyError(FormatUtils.noMatch("roles", query));
            else
                event.replyError(FormatUtils.errorWithHelp("\""+roles.get(0).getName()+"\" is not a roleme role!", this.getName()));
        } else if(rolemes.size()>1) {
            event.replyError(FormatUtils.multipleRolesFound(query, rolemes));
        } else {
            Role requested = rolemes.get(0);
            if(!event.getSelfMember().canInteract(requested)) {
                event.replyError(FormatUtils.errorWithHelp("Cannot interract with requested role!", this.getName()));
            } else if(!event.getMember().getRoles().contains(requested)){
                event.getGuild().getController().addRolesToMember(event.getMember(), requested).queue(v -> {
                    event.replySuccess("Successfully gave the role \""+requested.getName()+"\"!");
                });
            } else {
                event.getGuild().getController().removeRolesFromMember(event.getMember(), requested).queue(v -> {
                    event.replySuccess("Successfully removed the role \""+requested.getName()+"\"!");
                });
            }
            
        }
    }
    
    private class AddCmd extends NightFuryCommand {

        public AddCmd(NightFuryManager manager) {
            super(manager);
            this.name = "add";
            this.arguments = "<role>";
            this.help = "adds a roleme role for the server";
            this.guildOnly = true;
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
            this.category = manager.ADMINISTRATOR;
        }

        @Override
        protected void execute(CommandEvent event) {
            Settings settings = manager.getSettingsForGuild(event.getGuild());
            String query = event.getArgs();
            List<Role> found = SearcherUtil.searchRole(query, event.getGuild());
            if(found.size()<1)
                event.replyError(FormatUtils.noMatch("roles", query));
            else if(found.size()>1)
                event.replyError(FormatUtils.multipleRolesFound(query, found));
            else {
                Role requested = found.get(0);
                if(settings.isRoleMe(requested.getIdLong()))
                    event.replyError("The role \""+requested.getName()+"\" is already a roleme role!");
                else {
                    if(event.getSelfMember().canInteract(requested))
                        event.replySuccess("The role \""+requested.getName()+"\" was set as roleme!");
                    else {
                        event.replyWarning("The role \""+requested.getName()+"\" was set as roleme!\n"
                                + "Please be aware that due to role heirarchy positioning, I will not be able to give this role to members!\n"
                                + "To fix this, make sure my I have a role higher than \""+requested.getName()+"\" on the roles list.");
                    }
                    settings.addRoleMe(requested.getIdLong());
                }
            }
        }
        
    }

    private class RemoveCmd extends NightFuryCommand {

        public RemoveCmd(NightFuryManager manager) {
            super(manager);
            this.name = "remove";
            this.arguments = "<role>";
            this.help = "removes a roleme role for the server";
            this.guildOnly = true;
            this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
            this.category = manager.ADMINISTRATOR;
        }

        @Override
        protected void execute(CommandEvent event) {
            Settings settings = manager.getSettingsForGuild(event.getGuild());
            String query = event.getArgs();
            List<Role> found = SearcherUtil.searchRole(query, event.getGuild())
                    .stream().filter(role -> settings.isRoleMe(role.getIdLong())).collect(Collectors.toList());
            if(found.size()<1)
                event.replyError(FormatUtils.noMatch("roleme roles", query));
            else if(found.size()>1)
                event.replyError(multipleRoleMesFound(query, found));
            else {
                Role requested = found.get(0);
                event.replySuccess("The role \""+requested.getName()+"\" is no longer set as roleme!");
                settings.removeRoleMe(requested.getIdLong());
            }
        }
        
        private String multipleRoleMesFound(String argument, List<Role> roles) {
            String str = "Multiple roleme roles found matching \""+argument+"\":\n";
            for(int i=0; i<roles.size() || i<5; i++) {
                str += roles.get(0).getName()+" (ID: "+roles.get(0).getId()+")\n";
                if(i==4 && roles.size()>4)
                    str += "And "+(roles.size()-4)+" other roleme role"+(roles.size()-4>1?"s...":"...");
            }
            return str.trim();
        }
    }
}
