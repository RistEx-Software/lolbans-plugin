/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */


package com.ristexsoftware.lolbans.Commands.Warn;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WarnCommand extends RistExCommand {
    public WarnCommand(Plugin owner) {
        super("warn", owner);
        this.setDescription("Issue a warning against a player");
        this.setPermission("lolbans.warn");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args) {
        try {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(
                    Messages.Translate("Syntax.Warn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args) {
        if (!PermissionUtil.Check(sender, "lolbans.warn"))
            return User.PermissionDenied(sender, "lolbans.warn");

        // /warn [-s] <PlayerName> <Reason>
        try {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("PlayerName", 0);
            a.RequiredSentence("Reason", 1);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            String PlayerName = a.get("PlayerName");
            String reason = a.get("Reason");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            Punishment punish = new Punishment(PunishmentType.PUNISH_WARN, sender instanceof Player ? ((Player)sender).getUniqueId().toString() : null, target, reason, null, silent);
            punish.Commit(sender);

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", target.getName());
                    put("reason", reason);
                    put("punishid", punish.GetPunishmentID());
                    put("arbiter", sender.getName());
                    put("silent", Boolean.toString(silent));
                    put("appealed", Boolean.toString(punish.GetAppealed()));
                    put("simple", Boolean.toString(
                            Main.getPlugin(Main.class).getConfig().getBoolean("WarningSettings.SimpleWarning")));
                }
            };

            // If they're online, require acknowledgement immediately by freezing them and
            // sending a message.
            if (target.isOnline()) {
                String WarnedMessage = Messages.Translate("Warn.WarnedMessage", Variables);
                User u = Main.USERS.get(target.getUniqueId());
                System.out.println(u);
                User.playSound((Player) target,
                        Main.getPlugin(Main.class).getConfig().getString("WarningSettings.Sound"));
                if (Main.getPlugin(Main.class).getConfig().getBoolean("WarningSettings.SimpleWarning")) {
                    ((Player) target).sendMessage(WarnedMessage);
                } else {
                    System.out.println(((Player)target).getLocation() + " " + WarnedMessage);
                    u.SetWarned(true, ((Player) target).getLocation(), WarnedMessage); // FIXME: This line produces a NPE, not sure why, Works For Me™
                    u.SendMessage(WarnedMessage);
                    // Send them a box as well. This will disallow them from sending move events.
                    // However, client-side enforcement is not guaranteed so we also enforce the
                    // same thing using the MovementListener, this just helps stop rubberbanding.
                    u.SpawnBox(true, null);
                }
            }
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Warn.WarnAnnouncment", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        } catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}