package com.ristexsoftware.lolbans.Commands.Mute;

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

public class UnmuteCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.unmute"))
            return User.PermissionDenied(sender, "lolbans.unmute");

        // /unmute [-s] <PlayerName> <Reason>
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
            
            if (!User.IsPlayerMuted(target))
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsNotMuted", sender, target.getName(), true);

            // Preapre a statement
            // We need to get the latest banid first.
            PreparedStatement pst3 = self.connection.prepareStatement("SELECT PunishID FROM Punishments WHERE UUID = ? AND Type = 1 AND Appealed = false");
            pst3.setString(1, target.getUniqueId().toString());

            ResultSet result = pst3.executeQuery();
            result.next();
            String MuteID = result.getString("PunishID");

            // Run the async task for the database
            Future<Boolean> UnMute = DatabaseUtil.RemovePunishment(MuteID, target, sender, reason, TimeUtil.TimestampNow());
            if (!UnMute.get())
            {
                sender.sendMessage(Messages.ServerError);
                return true;
            }

            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("unmuter", sender.getName());
                    put("punishid", MuteID);
                }};

            // Post that to the database.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!silent && (p.hasPermission("lolbans.alerts") || p.isOp()) && !p.equals(target))
                    p.sendMessage(Messages.Translate(silent ? "Mute.SilentUnmuteAnnouncment" : "Mute.UnmuteAnnouncment", Variables));
            }

            if (target.isOnline())
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereUnMuted", Variables));

            if (DiscordUtil.UseSimplifiedMessage == true)
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentUnmute" : "Discord.SimpMessageUnmute", Variables));
            else
                DiscordUtil.SendDiscord(sender, "un-muted", target, reason, MuteID, silent);
        }
        catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}