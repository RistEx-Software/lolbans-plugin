/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.common.commands.mute;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;

public class MuteChat extends AsyncCommand {

    public MuteChat(LolBans plugin) {
        super("mutechat", plugin);
        this.setDescription("Mute the chat for all players (toggleable)");
        this.setPermission("lolbans.mutechat");
        this.setAliases(Arrays.asList(new String[] { "mute-chat" }));
        setSyntax(getPlugin().getLocaleProvider().get("syntax.chat-mute"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {

    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        return null;
    }

    // TODO: Add timer ability
    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        if (!sender.hasPermission("lolbans.mutechat"))
        sender.permissionDenied("lolbans.mutechat");
        try {
            LolBans.getPlugin().setChatMute(!LolBans.getPlugin().isChatMute());

            //TODO: maybe add more info in here?
            if (LolBans.getPlugin().isChatMute()) 
                LolBans.getPlugin().broadcastMessage(LolBans.getPlugin().getLocaleProvider().translate("mute.global-muted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
            else
                LolBans.getPlugin().broadcastMessage(LolBans.getPlugin().getLocaleProvider().translate("mute.global-unmuted", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
        }
        return false;
    }
    
}