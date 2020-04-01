package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;

import java.sql.*;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UnbanCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.unban"))
            return true;

        if (args.length < 2)
            return false;
        
        // Syntax: /unban [-s] <PlayerName|PunishID> <Reason>
        try 
        {
            boolean silent = args.length > 3 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            OfflinePlayer target = User.FindPlayerByAny(args[0]);
            
            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsNotBanned", sender, target.getName(), true);

            // Preapre a statement
            // We need to get the latest banid first.
            // TODO: Can we ensure they have the right punish id?
            PreparedStatement pst3 = self.connection.prepareStatement("SELECT PunishID FROM Punishments WHERE UUID = ? AND Type = 0 AND Appealed = false");
            pst3.setString(1, target.getUniqueId().toString());
            // TODO: Uh. we need to add a database call for this
            ResultSet result = pst3.executeQuery();
            result.next();
            String PunishID = result.getString("PunishID");

            // Run the async task for the database
            Future<Boolean> UnBan = DatabaseUtil.RemovePunishment(PunishID, target, sender, reason, TimeUtil.TimestampNow());

            if (!UnBan.get())
            {
                sender.sendMessage(Messages.ServerError);
                return true;
            }

            // Prepare our announce message
            String AnnounceMessage = Messages.Translate(silent ? "Ban.SilentUnbanAnnouncment" : "Ban.UnbanAnnouncment",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("banner", sender.getName());
                    put("punishid", PunishID);
                }}
            );

            // Log to console.
            self.getLogger().info(AnnounceMessage);

            // Post that to the database.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                    p.sendMessage(AnnounceMessage);
            }

            if (DiscordUtil.UseSimplifiedMessage == true)
            {
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentUnban" : "Discord.SimpMessageUnban",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", reason);
                        put("banner", sender.getName());
                        put("punishid", PunishID);
                    }}
                ));
                return true;
            }
            else
            {
                DiscordUtil.SendDiscord(sender, "unbanned", target, reason, PunishID, silent);
                return true;
            }
        }
        catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }
}