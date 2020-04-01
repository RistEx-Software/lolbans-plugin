package com.ristexsoftware.lolbans.Commands.Warn;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.Map;


public class WarnCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.warn"))
            return User.PermissionDenied(sender, "lolbans.warn");

        // /warn [-s] <PlayerName> <Reason>
        if (args.length < 2)
            return false;

        try 
        {
            boolean silent = args.length > 2 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            // Get the latest ID of the banned players to generate a PunishID form it.
            String warnid = PunishID.GenerateID(DatabaseUtil.GenID("Warnings"));

            // InsertWarn
            Future<Boolean> InsertWarn = DatabaseUtil.InsertPunishment(PunishmentType.PUNISH_WARN, target, sender, reason, warnid, null);
            if (!InsertWarn.get())
            {
                sender.sendMessage(Messages.ServerError);
                return true;
            }

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("punishid", warnid);
                put("warner", sender.getName());
            }};
                
            
            // If they're online, require acknowledgement immediately by freezing them and sending a message.
            if (target.isOnline())
            {
                String WarnedMessage = Messages.Translate("Warn.WarnedMessage", Variables);
                User u = Main.USERS.get(target.getUniqueId());
                u.SetWarned(true, ((Player)target).getLocation(), WarnedMessage);
                u.SendMessage(WarnedMessage);

                // Send them a box as well. This will disallow them from sending move events.
                // However, client-side enforcement is not guaranteed so we also enforce the
                // same thing using the MovementListener, this just helps stop rubberbanding.
                u.SpawnBox(true, null);
            }
        
            String WarnAnnouncement = Messages.Translate("Warn.WarnAnnouncment", Variables);

            // Log to console.
            self.getLogger().info(WarnAnnouncement);
                
            // Send the message to all online players.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!silent && (p.hasPermission("lolbans.alerts") || p.isOp() && p != target))
                    p.sendMessage(WarnAnnouncement);
            }

            // Send to Discord. (New method)
            if (DiscordUtil.UseSimplifiedMessage == true)
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentWarn" : "Discord.SimpMessageWarn", Variables));
            else
                DiscordUtil.SendDiscord(sender, "warned", target, reason, warnid, silent);
        }
        catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}