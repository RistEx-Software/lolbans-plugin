package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.util.TreeMap;


public class KickCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /kick [-s] <PlayerName> <Reason>");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
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

            Punishment punish = new Punishment(PunishmentType.PUNISH_KICK, sender, target, reason, null);
            punish.Commit(sender);

            // Kick the player
            User.KickPlayer(punish);

            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("punishid", punish.GetPunishmentID());
                    put("kicker", sender.getName());
                }};

            String KickAnnouncement = Messages.Translate(silent ? "Kick.SilentKickAnnouncement" : "Kick.KickAnnouncement", Variables);
        
            // Log to console.
            self.getLogger().info(KickAnnouncement);
                
            // Send the message to all online players.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                    p.sendMessage(KickAnnouncement);
            }

            if (DiscordUtil.UseSimplifiedMessage == true)
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentKick" : "Discord.SimpMessageKick", Variables));
            else
                DiscordUtil.SendDiscord(punish, silent);
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}