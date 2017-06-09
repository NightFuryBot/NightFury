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

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import com.jagrosh.jdautilities.commandclient.CommandEvent;

import me.kgustave.nightfury.manager.NightFuryManager;
import me.kgustave.nightfury.manager.Settings;
import me.kgustave.nightfury.utils.FormatUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

/**
 *
 * @author Kaidan Gustave
 */
public class ColorMeCmd extends NightFuryCommand {
    
    private static final String HEX_REGEX = "#([0-9|A-F|a-f])([0-9|A-F|a-f])([0-9|A-F|a-f])([0-9|A-F|a-f])([0-9|A-F|a-f])([0-9|A-F|a-f])";
    
    public ColorMeCmd(NightFuryManager manager) {
        super(manager);
        this.name = "colorme";
        this.arguments = "hexcode";
        this.help = "changes your role color to a given hexcode";
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.children = new NightFuryCommand[]{
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        Settings settings = manager.getSettingsForGuild(event.getGuild());
        String args = event.getArgs();
        
        if(!args.matches(HEX_REGEX)) {
            event.replyError(FormatUtils.invalidArgs(this.getName()));
            return;
        }
        if(args.isEmpty()) {
            event.replyError(FormatUtils.tooFewArgs(this.getName()));
            return;
        }
        
        String hex = args.replaceFirst("#", "");
        
        List<Role> colormes = event.getMember().getRoles()
                .stream().filter(role -> settings.isColorme(role.getIdLong()) && event.getSelfMember().canInteract(role))
                .sorted((a,b) -> a.compareTo(b)).collect(Collectors.toList());
        Color color;
        try {
            color = Color.decode(hex);
        } catch(NumberFormatException e) {
            event.replyError("\""+args+"\" is not a valid color!");
            return;
        }
        if(colormes.size()<1)
            event.replyError(FormatUtils.errorWithHelp("You do not have any ColorMe roles!", this.getName()));
        else {
            Role h = colormes.get(0);
            if(h==null) {
                event.replyError(FormatUtils.errorWithHelp("Cannot interract with any ColorMe roles!", this.getName()));
            } else {
                h.getManager().setColor(color).queue(v -> {
                    event.replySuccess("Successfully colored your highest ColorMe role as *"+hex+"*!");
                });
            }
        }
    }

}
