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

package com.ristexsoftware.lolbans.bukkit.Listeners;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.MaintenanceLevel;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class PingListener implements Listener {
    
    // Bukkit is retarded, unlike BungeeCord, there's no reasonable way to set the server's protocol version
    // without using craftbukkit api calls.
    // TODO: hack bukkit some more.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event) throws InvalidConfigurationException {
        if (LolBans.getPlugin().getMaintenanceModeEnabled()) {
            
            event.setMotd(LolBans.getPlugin().getLocaleProvider().translate("maintenance.description", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
                put("maintenancelevel", MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()));
            }}));

        }
    }
}