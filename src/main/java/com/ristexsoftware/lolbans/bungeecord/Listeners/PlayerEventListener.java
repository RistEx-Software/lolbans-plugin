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

package com.ristexsoftware.lolbans.bungeecord.Listeners;

import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerEventListener implements Listener {
    
    @EventHandler
    public void onChat(ChatEvent event) throws InvalidConfigurationException {
        if (event.getSender() instanceof ProxiedPlayer) {
            User user = LolBans.getPlugin().getUser(((ProxiedPlayer) event.getSender()).getUniqueId());
            String message = event.getMessage();
            Punishment punishment = user.getLatestPunishmentOfType(PunishmentType.MUTE);
            List<String> commands = LolBans.getPlugin().getConfig().getStringList("mute-settings.blacklisted-commands");
            if (punishment == null || punishment.getExpiresAt().getTime() <= System.currentTimeMillis()) return;
            if (event.isCommand() || event.isProxyCommand()) {
                for (String command : commands) {
                    if (event.getMessage().toLowerCase().startsWith("/"+command.toLowerCase())) {
                        if (punishment != null) {
                            event.setCancelled(true);
                            user.sendMessage(punishment);

                            LolBans.getPlugin().notifyStaff(LolBans.getPlugin().getLocaleProvider().translate("mute.chat-attempt",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                        {
                                            put("player", user.getName());
                                            put("message", message);
                                        }
                                    }), "lolbans.mute.notify");
                            return;
                        }
                    }
                }
            } else {
                event.setCancelled(true);
                user.sendMessage(punishment);
                LolBans.getPlugin().notifyStaff(
                    LolBans.getPlugin().getLocaleProvider().translate("mute.chat-attempt", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                        {
                            put("player", user.getName());
                            put("message", message);
                        }
                    }), "lolbans.mute.notify");
            }
        }
    }
}