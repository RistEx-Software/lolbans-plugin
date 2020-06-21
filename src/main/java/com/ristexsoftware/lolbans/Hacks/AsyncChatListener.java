package com.ristexsoftware.lolbans.Hacks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

public class AsyncChatListener implements Listener {

    private static Main self = Main.getPlugin(Main.class);

    // This is honestly jank as fuck, and i'm t ired, but it Just Works:tm:
    // We need to make sure they can't use /msg to send players messages!
    @EventHandler
    public static void onCommandProcess(PlayerCommandPreprocessEvent event) {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    List<String> cmds = self.getConfig().getStringList("MuteSettings.blacklisted-commands");
                    for (String cmd : cmds) {
                        if (event.getMessage().startsWith("/" + cmd)) {
                            try {
                                PreparedStatement MuteStatement = self.connection.prepareStatement(
                                        "SELECT * FROM lolbans_punishments WHERE UUID = ? AND Type = ? AND (Expiry IS NULL OR Expiry >= NOW()) AND Appealed = False");
                                MuteStatement.setString(1, event.getPlayer().getUniqueId().toString());
                                MuteStatement.setInt(2, PunishmentType.PUNISH_MUTE.ordinal());

                                Future<Optional<ResultSet>> MuteRecord = DatabaseUtil.ExecuteLater(MuteStatement);
                                Optional<ResultSet> MuteResult = MuteRecord.get();
                                // The query was successful.
                                if (MuteResult.isPresent()) {
                                    ResultSet result = MuteResult.get();
                                    // They're muted. don't let them speak...
                                    if (result.next()) {
                                        event.setCancelled(true);
                                        User.playSound((Player) event.getPlayer(),
                                                self.getConfig().getString("MuteSettings.Sound"));
                                        Timestamp MuteTime = result.getTimestamp("Expiry");

                                        event.getPlayer().sendMessage(Messages.Translate("Mute.YouAreMuted",
                                                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                                    {
                                                        put("player", event.getPlayer().getName());
                                                        put("reason", result.getString("Reason"));
                                                        put("arbiter", result.getString("ArbiterName"));
                                                        put("punishid", result.getString("PunishID"));
                                                        put("expiry", MuteTime == null ? "" : MuteTime.toString());
                                                    }
                                                }));
                                        for (Player staff : Bukkit.getOnlinePlayers()) {
                                            if (staff.hasPermission("lolbans.mute.notify")) {

                                                staff.sendMessage(Messages.Translate("Mute.ChatAttempt",
                                                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                                            {
                                                                put("player", event.getPlayer().getName());
                                                                put("message", event.getMessage());
                                                            }
                                                        }));
                                            }
                                        }
                                        return true;
                                    }
                                }
                                return false;
                            } catch (SQLException | InvalidConfigurationException | InterruptedException
                                    | ExecutionException e) {
                                e.printStackTrace();
                                event.setCancelled(true);
                                event.getPlayer().sendMessage(Messages.ServerError);
                            }
                        }
                        else return false;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Messages.ServerError);
                }
                return false;
            }
        });
        Main.pool.execute(t);
        try {
            if (t.get()) {
                event.setCancelled(true);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.ServerError);
        }
    }

    @EventHandler
    public static void OnAsyncPlayerChat(AsyncPlayerChatEvent event) throws InterruptedException, ExecutionException {
        try {
            // Send a message to the user if the chat is muted globally
            if (self.ChatMuted) {
                if (event.getPlayer().hasPermission("lolbans.mute.bypass"))
                    return;
                event.setCancelled(true);
                User.playSound((Player) event.getPlayer(), self.getConfig().getString("MuteSettings.Sound"));
                event.getPlayer().sendMessage(Messages.Translate("Mute.GlobalMuted",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
                return;
            }

            // Otherwise check if they're individually muted.
            PreparedStatement MuteStatement = self.connection.prepareStatement(
                    "SELECT * FROM lolbans_punishments WHERE UUID = ? AND Type = ? AND (Expiry IS NULL OR Expiry >= NOW()) AND Appealed = False");
            MuteStatement.setString(1, event.getPlayer().getUniqueId().toString());
            MuteStatement.setInt(2, PunishmentType.PUNISH_MUTE.ordinal());

            Future<Optional<ResultSet>> MuteRecord = DatabaseUtil.ExecuteLater(MuteStatement);
            Optional<ResultSet> MuteResult = MuteRecord.get();
            // The query was successful.
            if (MuteResult.isPresent()) {
                ResultSet result = MuteResult.get();
                // They're muted. don't let them speak...
                if (result.next()) {
                    event.setCancelled(true);
                    User.playSound((Player) event.getPlayer(), self.getConfig().getString("MuteSettings.Sound"));
                    Timestamp MuteTime = result.getTimestamp("Expiry");

                    event.getPlayer().sendMessage(Messages.Translate("Mute.YouAreMuted",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("player", event.getPlayer().getName());
                                    put("reason", result.getString("Reason"));
                                    put("arbiter", result.getString("ArbiterName"));
                                    put("punishid", result.getString("PunishID"));
                                    put("expiry", MuteTime == null ? "" : MuteTime.toString());
                                }
                            }));
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.hasPermission("lolbans.mute.notify")) {

                            staff.sendMessage(Messages.Translate("Mute.ChatAttempt",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                        {
                                            put("player", event.getPlayer().getName());
                                            put("message", event.getMessage());
                                        }
                                    }));
                        }
                    }
                }
            }
        } catch (SQLException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}