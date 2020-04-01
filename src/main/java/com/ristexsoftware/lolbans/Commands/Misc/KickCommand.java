package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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


public class KickCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.kick"))
            return User.PermissionDenied(sender, "lolbans.kick");
    
        // /kick [-s] <PlayerName> <Reason>
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

            if (!target.isOnline())
                return User.PlayerIsOffline(sender, PlayerName, true);

            if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                return User.PlayerOnlyVariableMessage("Kick.CannotKickSelf", sender, target.getName(), true);

            // Get the latest ID of the banned players to generate a PunishID form it.
            String kickid = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));
            DatabaseUtil.InsertPunishment(PunishmentType.PUNISH_KICK, target, sender, reason, kickid, null);

            String KickAnnouncement = Messages.Translate(silent ? "Kick.SilentKickAnnouncement" : "Kick.KickAnnouncement",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("kickid", kickid);
                    put("kicker", sender.getName());
                }}
            );

            // Kick the player
            User.KickPlayer(sender.getName(), (Player) target, kickid, reason);
        
            // Log to console.
            self.getLogger().info(KickAnnouncement);
                
            // Send the message to all online players.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                    p.sendMessage(KickAnnouncement);
            }

            if (DiscordUtil.UseSimplifiedMessage == true)
            {
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentKick" : "Discord.SimpMessageKick",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", reason);
                        put("banner", sender.getName());
                        put("kickid", kickid);
                    }}
                ));
            }
            else
                DiscordUtil.SendDiscord(sender, "kicked", target, reason, kickid, null, silent);
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}