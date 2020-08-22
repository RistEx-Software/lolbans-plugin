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

import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.MaintenanceLevel;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PingEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(final ProxyPingEvent event) throws InvalidConfigurationException {
        if (LolBans.getPlugin().getMaintenanceModeEnabled()) {
            final ServerPing ping = event.getResponse();
            ping.getVersion().setProtocol(1);
            ping.getVersion().setName(LolBans.getPlugin().getLocaleProvider().translate("maintenance.player-count", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
                    put("online", Integer.toString(ping.getPlayers().getOnline()));
                    put("max", Integer.toString(ping.getPlayers().getMax()));
                    put("maintenancelevel", MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()));
            }}));
            
            String[] hoverSplit = LolBans.getPlugin().getLocaleProvider().translate("maintenance.hover-message", new TreeMap<String, String>(){{
                put("maintenancelevel",MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()));
            }}).split("\n");

            ServerPing.PlayerInfo[] sample = new ServerPing.PlayerInfo[hoverSplit.length];
            for (int i = 0; i < hoverSplit.length; i++) {
                sample[i] = new ServerPing.PlayerInfo(hoverSplit[i], "");
            }
            ping.getPlayers().setSample(sample);

            ping.setDescription(LolBans.getPlugin().getLocaleProvider().translate("maintenance.description", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
                put("maintenancelevel",MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()));
            }}));
        }
    }
}