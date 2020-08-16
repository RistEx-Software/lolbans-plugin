package com.ristexsoftware.lolbans.bukkit.Listeners;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.MaintenanceLevel;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;

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
            
            event.setMotd(Messages.translate("maintenance.description", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
                put("maintenancelevel", MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()));
            }}));

        }
    }
}