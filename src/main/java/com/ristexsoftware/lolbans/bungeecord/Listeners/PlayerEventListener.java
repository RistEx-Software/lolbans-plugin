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