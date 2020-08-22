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