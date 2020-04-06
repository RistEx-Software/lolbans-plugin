package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.Optional;
import java.sql.*;

public class PruneHistoryCommand implements CommandExecutor 
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.history.prune"))
            return User.PermissionDenied(sender, "lolbans.history.prune");

        // /prunehistory <Player|PunishID|CIDR|Regex> <Distance In Time|*>
        if (args.length < 2)
            return false;

        try 
        {
            String searchable = args[0];
            String TimePeriod = args[1];
            @SuppressWarnings("deprecation") OfflinePlayer player = Bukkit.getOfflinePlayer(searchable);
            Timestamp bantime = null;

            // Parse ban time.
            if (!Messages.CompareMany(TimePeriod, new String[]{"*", "0"}))
            {
                Optional<Long> dur = TimeUtil.Duration(TimePeriod);
                if (dur.isPresent())
                    bantime = new Timestamp(System.currentTimeMillis() - (dur.get() * 1000L));
                else
                    return false;
            }

            // This only prunes the specific punishment
            if (PunishID.ValidateID(searchable))
            {
                PreparedStatement RegexStatement = self.connection.prepareStatement("SELECT * FROM RegexBans WHERE PunishID = ?");
                PreparedStatement IPBanStatement = self.connection.prepareStatement("SELECT * FROM IPBans WHERE PunishID = ?");
                PreparedStatement Reports = self.connection.prepareStatement("SELECT * FROM Reports WHERE PunishID = ?");
                
                // TODO: What about Regex bans or others
                Optional<Punishment> punish = Punishment.FindPunishment(searchable);
                if (punish.isPresent())
                {
                    Punishment p = punish.get();
                    p.Delete();
                }

            }
            else if (searchable.contains(".") || searchable.contains(":"))
            {
                // try
                // {
                    IPAddressString thingy = new IPAddressString(searchable);
                    for (IPAddressString cb : Main.BannedAddresses)
                    {
                        if (cb.contains(thingy))
                        {
                            // TODO: make async
                            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM IPBans WHERE IPAddress = ?");
                            pst.setString(1, cb.toString());
            
                            ResultSet res = pst.executeQuery();
                            // Clear the history for this address range
                            if (!res.next())
                            {
                                sender.sendMessage("The IP " + cb.toString() + " is not banned.");
                                return true;
                            }

                            PreparedStatement ps = self.connection.prepareStatement("DELETE FROM IPBans WHERE id = ?");
                            ps.setInt(1, res.getInt("id"));
                            DatabaseUtil.ExecuteUpdate(ps);
                            sender.sendMessage("History Cleared.");
                        }
                    }
                // }
                // catch (AddressStringException e)
                // {
                //     // TODO: message.yml-ify this
                //     sender.sendMessage("Invalid IP Address");
                // }
            }
            else if (player != null)
            {
                PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM Punishments WHERE UUID = ?");
                ps.setString(1, player.getUniqueId().toString());

                Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();
                if (ores.isPresent())
                {
                    ResultSet res = ores.get();
                    if (res.next())
                    {
                        ps = self.connection.prepareStatement("DELETE FROM Punishments WHERE UUID = ?");
                        ps.setString(1, player.getUniqueId().toString());
                        DatabaseUtil.ExecuteUpdate(ps);
                        sender.sendMessage("History Cleared.");
                    }
                    else
                        sender.sendMessage("Player " + player.getName() + " is not punished.");
                }
            }
            else
            {
                Pattern regex = Pattern.compile(searchable);
                PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM RegexBans WHERE Regex = ?");
                pst.setString(1, regex.toString());

                Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(pst).get();
                if (ores.isPresent())
                {
                    ResultSet res = ores.get();
                    if (res.next())
                    {
                        pst = self.connection.prepareStatement("DELETE FROM RegexBans WHERE id = ?");
                        DatabaseUtil.ExecuteUpdate(pst);
                        sender.sendMessage("History Cleared.");
                    }
                    else
                        sender.sendMessage("Regex " + searchable + " is not banned.");
                }
            }
        }
        catch (InterruptedException | ExecutionException | SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}