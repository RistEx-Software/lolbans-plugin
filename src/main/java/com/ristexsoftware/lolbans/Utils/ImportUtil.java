package com.ristexsoftware.lolbans.Utils;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Future;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.MojangUtil.MojangUser;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ImportUtil {

    private static Main self = Main.getPlugin(Main.class);
    static boolean isRunning = false;

    // We send the sender a message if anything happens..
    /**
     * Import function for importing Essentials user data and punishments NOTE: This
     * should only be called asynchronously (i.e. RistexCommandAsync)
     * 
     * @param sender The command sender
     */
    public static void importEssentials(CommandSender sender) {
        if (isRunning) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.Prefix
                    + "&cAn import task is already running! Type \"/lolbans import cancel\" to cancel it"));
            return;
        }
        

        File banJson = new File(Bukkit.getWorldContainer().getAbsolutePath() + "/banned-players.json");
        File userYaml = new File(Bukkit.getWorldContainer().getAbsolutePath() + "/plugins/Essentials/userdata/");
        File[] userYamls = userYaml.listFiles();

        if (banJson.exists() && userYaml.exists()) {
            try {
                Reader reader = Files.newBufferedReader(
                        Paths.get(Bukkit.getWorldContainer().getAbsolutePath() + "/banned-players.json"));
                JsonParser parser = new JsonParser();
                JsonElement tree = parser.parse(reader);
                JsonArray array = tree.getAsJsonArray();
                int seconds = userYamls.length + array.size();
                Timestamp ts = new Timestamp((TimeUtil.GetUnixTime() * 1000L) + Math.round((seconds * 1000L) * 0.25));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        Messages.Prefix + "&bImporting &f" + array.size() + " &bpunishments and &f" + userYamls.length
                                + " &busers!\n" + Messages.Prefix + "&bThis will take approximately &f"
                                + TimeUtil.Expires(ts)));

                isRunning = true;
                for (JsonElement element : array) {
                    if (isRunning) {
                        JsonObject obj = element.getAsJsonObject();
                        Timestamp timeStamp = null;
                        if (obj.get("expires").toString().replace("\"", "").equals("forever")) {
                            timeStamp = TimeUtil.ParseToTimestamp("0");
                        } else {
                            Date date = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss Z")
                                    .parse(obj.get("expires").toString().replace("\"", ""));
                            timeStamp = new Timestamp(date.getTime());
                        }
                        MojangUtil mojangAPI = new MojangUtil();
                        MojangUser mojangUser = mojangAPI.resolveUser(obj.get("uuid").toString().replace("\"", ""));
                        if (mojangUser == null) {
                            self.getLogger().warning(obj.get("name").toString() + " does not exist! Skipping...");
                            continue; // Skip this, this user doesn't even exist.
                        }
                        OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
                        if (User.IsPlayerBanned(op)) {
                            self.getLogger().warning(mojangUser.getName() + " is already banned, skipping...");
                            continue;
                        }
                        Punishment punishment = new Punishment(PunishmentType.PUNISH_BAN, sender, op,
                                obj.get("reason").toString().replace("\"", ""), timeStamp, false);
                        punishment.Commit(sender);
                        Thread.sleep(250); // Lets wait a bit here too...
                    }

                }

                for (final File file : userYamls) {
                    if (isRunning) {
                        FileConfiguration ymlFile = YamlConfiguration.loadConfiguration(file);

                        String name = ymlFile.getString("lastAccountName");
                        Timestamp login = ymlFile.get("timestamps.login") == null ? null
                                : new Timestamp(ymlFile.getLong("timestamps.login"));
                        Timestamp logout = ymlFile.get("timestamps.logout") == null ? null
                                : new Timestamp(ymlFile.getLong("timestamps.logout"));
                        String ipaddress = ymlFile.getString("ipAddress");
                        String uuid = file.getName().replace(".yml", "");

                        // Essentials logs NPCs for some reason, so lets just skip those!
                        if (ymlFile.getBoolean("npc"))
                            continue;

                        // If the userdata is null, don't continue there's no point
                        if (name == null || ipaddress == null || uuid == null) {
                            self.getLogger().warning("User data is null, skipping file " + file.getName());
                            continue;
                        }
                        // If either of these are null, just set the timestamp to now
                        if (login == null || logout == null) {
                            login = login == null ? TimeUtil.TimestampNow() : login;
                            logout = logout == null ? TimeUtil.TimestampNow() : logout;
                        }

                        // Finally, import the user, but also ensure InsertUser doesn't fail.
                        final Future<Boolean> insertUser = DatabaseUtil.InsertUser(uuid, name, ipaddress, login,
                                logout);
                        if (!insertUser.get()) {
                            self.getLogger().severe("There was an error importing " + file.getName());
                            continue;
                        }

                        Thread.sleep(250); // Lets wait a bit before the next import...
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isRunning = false;
        }
    }

    /**
     * Import function for importing LiteBans user data and punishments NOTE: This
     * should only be called from an asynchronous command (RistexCommandAsync)
     * 
     * @param sender The command sender, this is used to send messages when
     *               something happens
     */
    public static void importLitebans(CommandSender sender) {
        try {
            // sender.sendMessage(Messages.Prefix + ChatColor.RED + "LiteBans importing is not supported yet!");
            // return;
            if (isRunning) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Messages.Prefix
                        + "&cAn import task is already running! Type \"/lolbans import cancel\" to cancel it"));
                return;
            }
            PreparedStatement bans = self.connection.prepareStatement("SELECT * FROM litebans_bans WHERE ipban = FALSE");
            PreparedStatement ipbans = self.connection.prepareStatement("SELECT * FROM litebans_bans WHERE ipban = TRUE");
            PreparedStatement mutes = self.connection.prepareStatement("SELECT * FROM litebans_mutes");
            PreparedStatement kicks = self.connection.prepareStatement("SELECT * FROM litebans_kicks");
            PreparedStatement warns = self.connection.prepareStatement("SELECT * FROM litebans_warnings");
            ResultSet banrs = bans.executeQuery();
            ResultSet muters = mutes.executeQuery();
            ResultSet kickrs = kicks.executeQuery();
            ResultSet warnrs = warns.executeQuery();
            while (banrs.next()) {
                MojangUtil mojangAPI = new MojangUtil();
                MojangUser mojangUser = mojangAPI.resolveUser(banrs.getString("uuid"));
                MojangUser arbiter = mojangAPI.resolveUser(banrs.getString("banned_by_uuid"));
                if (mojangUser == null) {
                    self.getLogger().warning(banrs.getString("uuid") + " does not exist! Skipping...");
                    continue; // Skip this, this user doesn't even exist.
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
                if (User.IsPlayerBanned(op)) {
                    self.getLogger().warning(mojangUser.getName() + " is already banned, skipping...");
                    continue;
                }
                System.out.println(op.getName());
                System.out.println(op.getUniqueId());
                Punishment ban = new Punishment(PunishmentType.PUNISH_BAN, arbiter.getName(), op, banrs.getString("reason"), banrs.getLong("until") <= 0 ? null : new Timestamp(banrs.getLong("until")), banrs.getBoolean("silent"));
                ban.Commit(sender);
                Thread.sleep(250); // Make sure we're not going too fast
            }

            // So we need to do seperate while loops, because litebans is literally dumb and uses seperate tables...
            while (muters.next()) {
                MojangUtil mojangAPI = new MojangUtil();
                MojangUser mojangUser = mojangAPI.resolveUser(muters.getString("uuid"));
                MojangUser arbiter = mojangAPI.resolveUser(muters.getString("banned_by_uuid"));
                if (mojangUser == null) {
                    self.getLogger().warning(muters.getString("uuid") + " does not exist! Skipping...");
                    continue; // Skip this, this user doesn't even exist.
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
                if (User.isPlayerMuted(op).get()) {
                    self.getLogger().warning(mojangUser.getName() + " is already muted, skipping...");
                    continue;
                }
                Punishment ban = new Punishment(PunishmentType.PUNISH_MUTE, arbiter.getName(), op, muters.getString("reason"), muters.getLong("until") <= 0 ? null : new Timestamp(muters.getLong("until")), muters.getBoolean("silent"));
                ban.Commit(sender);
                Thread.sleep(250); // Make sure we're not going too fast
            }

            while (kickrs.next()) {
                MojangUtil mojangAPI = new MojangUtil();
                MojangUser mojangUser = mojangAPI.resolveUser(kickrs.getString("uuid"));
                MojangUser arbiter = mojangAPI.resolveUser(kickrs.getString("banned_by_uuid"));
                if (mojangUser == null) {
                    self.getLogger().warning(kickrs.getString("uuid") + " does not exist! Skipping...");
                    continue; // Skip this, this user doesn't even exist.
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
                Punishment ban = new Punishment(PunishmentType.PUNISH_KICK, arbiter.getName(), op, kickrs.getString("reason"), kickrs.getLong("until") <= 0 ? null : new Timestamp(kickrs.getLong("until")), kickrs.getBoolean("silent"));
                ban.Commit(sender);
                Thread.sleep(250); // Make sure we're not going too fast
            }

            while (warnrs.next()) {
                MojangUtil mojangAPI = new MojangUtil();
                MojangUser mojangUser = mojangAPI.resolveUser(warnrs.getString("uuid"));
                MojangUser arbiter = mojangAPI.resolveUser(warnrs.getString("banned_by_uuid"));
                if (mojangUser == null) {
                    self.getLogger().warning(warnrs.getString("uuid") + " does not exist! Skipping...");
                    continue; // Skip this, this user doesn't even exist.
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(mojangUser.getName());
                Punishment ban = new Punishment(PunishmentType.PUNISH_WARN, arbiter.getName(), op, warnrs.getString("reason"), warnrs.getLong("until") <= 0 ? null : new Timestamp(warnrs.getLong("until")), warnrs.getBoolean("silent"));
                ban.Commit(sender);
                Thread.sleep(250); // Make sure we're not going too fast
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancel any active import tasks
     */
    public static void Cancel() {
        isRunning = false;
    }

    /**
     * Check if there is an active import task running
     * 
     * @return true if a task is running
     */
    public static boolean ActiveTask() {
        return isRunning;
    }
}