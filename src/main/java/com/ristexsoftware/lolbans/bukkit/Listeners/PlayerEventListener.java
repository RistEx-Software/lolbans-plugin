package com.ristexsoftware.lolbans.bukkit.Listeners;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.event.EventHandler;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerEventListener implements Listener {

    @EventHandler
    public void onCommandProccess(PlayerCommandPreprocessEvent event) {
        FutureTask<Boolean> t = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    User user = LolBans.getPlugin().getUser(event.getPlayer().getUniqueId());
                    List<String> commands = LolBans.getPlugin().getConfig()
                            .getStringList("chat-settings.mute-settings.blacklisted-commands");
                    for (String command : commands) {
                        if (event.getMessage().toLowerCase().startsWith("/" + command.toLowerCase())) {
                            Punishment punishment = user.getLatestPunishmentOfType(PunishmentType.MUTE);
                            if (punishment != null && !(punishment.getExpiresAt().getTime() <= System.currentTimeMillis())) {
                                event.setCancelled(true);
                                user.sendMessage(punishment);

                                LolBans.getPlugin().notifyStaff(Messages.translate("mute.chat-attempt",
                                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                            {
                                                put("player", event.getPlayer().getName());
                                                put("message", event.getMessage());
                                            }
                                        }), "lolbans.mute.notify");
                                return false;
                            }
                        }
                    }
                    return true;
                } catch (Exception e) {
                    event.getPlayer().sendMessage(Messages.serverError);
                    return false;
                }
            }
        });
        LolBans.getPlugin().getPool().execute(t);
        try {
            if (t.get()) {
                event.setCancelled(true);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.serverError);
        }
    }

    @EventHandler
    public void onAsyncChat(AsyncPlayerChatEvent event) throws InvalidConfigurationException {
        User user = LolBans.getPlugin().getUser(event.getPlayer().getUniqueId());
        if (!LolBans.getPlugin().isChatEnabled()) {
            if (!event.getPlayer().hasPermission("lolbans.mute.bypass")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Messages.translate("Mute.GlobalMuted",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
            }
        }

        Punishment punishment = user.getLatestPunishmentOfType(PunishmentType.MUTE);
        if (punishment != null  && !(punishment.getExpiresAt().getTime() <= System.currentTimeMillis())) {
            event.setCancelled(true);
            user.sendMessage(punishment);
            LolBans.getPlugin().notifyStaff(
                    Messages.translate("mute.chat-attempt", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                        {
                            put("player", event.getPlayer().getName());
                            put("message", event.getMessage());
                        }
                    }), "lolbans.mute.notify");
        }
    }

}