package com.ristexsoftware.lolbans.Commands.Misc;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.MojangUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.Timing;
import com.ristexsoftware.lolbans.Utils.MojangUtil.MojangUser;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

// Yeet
public class LolBansCommand extends RistExCommandAsync {
    private static Main self = Main.getPlugin(Main.class);

    public LolBansCommand(Plugin owner) {
        super("lolbans", owner);
        this.setDescription("Reload config.yml and messages.yml");
        this.setPermission("lolbans.reload");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args) {
        try {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(
                    Messages.Translate("Syntax.LolBans", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args) {
        if (!PermissionUtil.Check(sender, "lolbans.reload"))
            return User.PermissionDenied(sender, "lolbans.reload");

        if (args.length < 1) {
            try {

                // self.saveConfig();
                Messages.Reload();
                self.reloadConfig();
                sender.sendMessage(Messages.Prefix + ChatColor.GREEN + "Reloaded LolBans successfully!");
            } catch (Exception e) {
                sender.sendMessage(Messages.ServerError);
            }
        } else if (args[0].equalsIgnoreCase("import")) {
            Timing t = new Timing();
            File banJson = new File(Bukkit.getWorldContainer().getAbsolutePath() + "/banned-players.json");
            if (banJson.exists()) {
                try {
                    Reader reader = Files.newBufferedReader(Paths.get(Bukkit.getWorldContainer().getAbsolutePath() + "/banned-players.json"));
                    JsonParser parser = new JsonParser();
                    JsonElement tree = parser.parse(reader);
                    JsonArray array = tree.getAsJsonArray();
                    int i = array.size();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&bLolBans &f» &bImporting &f" + i + " &bpunishments!"));

                    for (JsonElement element : array) {
                        JsonObject obj =  element.getAsJsonObject();
                        Timestamp timeStamp = null;
                        if (obj.get("expires").toString().replace("\"", "").equals("forever")) {
                            timeStamp = TimeUtil.ParseToTimestamp("0"); 
                        } 
                        else {
                            Date date = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss Z").parse(obj.get("expires").toString().replace("\"", ""));
                            timeStamp = new Timestamp(date.getTime());
                        }
                        MojangUtil mojangAPI = new MojangUtil();
                        MojangUser mojangUser = mojangAPI.resolveUser(obj.get("uuid").toString().replace("\"", ""));
                        if (mojangUser == null) {
                            System.out.println("Skipping: " + obj.get("name").toString());
                            continue; // Skip this, this user doesn't even exist.  
                        } 
                        Punishment punishment = new Punishment(PunishmentType.PUNISH_BAN, sender, Bukkit.getOfflinePlayer(mojangUser.getName()), obj.get("reason").toString().replace("\"", ""), timeStamp);
                        punishment.Commit(sender);
                    }
                    t.Finish(sender);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println(banJson.getPath());
            }
        }
        return true;
    }
}