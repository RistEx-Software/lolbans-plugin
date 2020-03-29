package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.sql.*;

public class HistoryCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    /*
    private String GetStringOrDefault(ResultSet rs, String deal, String def)
    {
        String ret = def;
        try
        {
            if ((ret = rs.getString(deal)) != null)
                return ret;
            return def;
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            return ret;
        }
    }

    private String GetLineItems(ResultSet result, String DatabaseName, String DisplayName, String Delim, boolean IsBanned)
    {
        // Get the array from the database (or N/A as the default)
        String array[] = this.GetStringOrDefault(result, DatabaseName, "N/A").split(",");

        // Get the latest ban ID and remove it from the comma separated list of things
        String LatestItem = array.length == 1 ? array[0] : array[array.length - 1];
        String Items = String.join(Delim, Arrays.copyOfRange(array, 0, array.length - 1));

        if (array.length >= 2)
            return this.fmt("&c%s:&7 %s %s", DisplayName, Items, IsBanned ? this.fmt("[&a%s&7]", LatestItem) : LatestItem);
        else
            return this.fmt("&c%s:&7 %s", DisplayName, LatestItem);
    }
    */

    /*
    private String[] bnyeh(ResultSet result, String PunishID, String DisplayName, String Delim, boolean IsBanned)
    {
        // Get the array from the database (or N/A as the default)
        String array[] = this.GetStringOrDefault(result, PunishID, "N/A").split(",");

        return array;
    }
    */

    private boolean HandleHistory(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.history"))
            return false;

        try
        {
            // They only need the players name, nothing else.
            if (args.length == 1)
            {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

                if (target == null)
                    return User.NoSuchPlayer(sender, args[0], true);

                // Preapre a statement
                // FIXME: This absolute unit of a query needs to be fixed... and so does this file...
                PreparedStatement pst = self.connection.prepareStatement("SELECT PlayerName, UUID, PunishID, Reason, Executioner, TimeBanned, Expiry, UnbanReason, UnbanExecutioner, GROUP_CONCAT(PunishID) AS PunishIDs, GROUP_CONCAT(Reason) AS Reasons, GROUP_CONCAT(Executioner) AS Executioners, GROUP_CONCAT(DISTINCT Executioner) AS Executioners2, GROUP_CONCAT(UnbanReason) AS UnbanReasons, GROUP_CONCAT(UnbanExecutioner) AS UnbanExecutioners, GROUP_CONCAT(DISTINCT UnbanExecutioner) AS UnbanExecutioners2, MAX(TimeBanned) FROM BannedHistory WHERE PlayerName = ?");
                pst.setString(1, args[0]);

                ResultSet result = pst.executeQuery();
                if (!result.next() || result.wasNull())
                {
                    try 
                    {
                        sender.sendMessage(Messages.Translate("History.NoSuchPlayer",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                            {{
                                put("player", target.getName());
                            }}
                        ));
                    }
                    catch (InvalidConfigurationException ex)
                    {
                        ex.printStackTrace();
                    }

                    return true;
                }

                boolean IsBanned = User.IsPlayerBanned(target);

                // We need to put all of the stupid shit from the database into this string array.
                String[] thing = {};

                // we also need to loop through each thing in the database, and return things like a banid username and so on in 1 "block"
                /* example:
                    =====
                    thingy 1
                    thingy 2
                    =====

                    =====
                    thingy 1
                    thingy2
                    =====
                */
                for (String thingy : thing)
                {
                    System.out.println(thingy);
                }
                
                

                //System.out.println(this.GetLineItems(result, "PunishIDs", "Ban ID(s)", ", ", IsBanned));
                

                /*
                sender.sendMessage(this.fmt("&8&m------------------------------"));
                sender.sendMessage(this.fmt("&cName: &7%s &8[%s]", target.getName(), target.getUniqueId().toString()));
                sender.sendMessage(this.GetLineItems(result, "PunishIDs", "Ban ID(s)", ", ", IsBanned));
                sender.sendMessage(this.GetLineItems(result, "Reasons", "Reason(s)", ", ", IsBanned));
                sender.sendMessage(this.GetLineItems(result, "Executioners", "Executioners(s)", ", ", IsBanned));
                sender.sendMessage(this.fmt("&cTimes Banned: &7%d", this.GetStringOrDefault(result, "PunishIDs", "").chars().filter(ch -> ch == ',').count()));

                if (IsBanned)
                {
                    Timestamp ts = result.getTimestamp("Expiry");
                    if (ts == null)
                        sender.sendMessage(this.fmt("&cExpiration: &7Indefinite."));
                    else
                        sender.sendMessage(this.fmt("&cExpiration: &7%s (%s)", TimeUtil.TimeString(ts), TimeUtil.Expires(ts)));
                }

                Timestamp LatestBanTime = result.getTimestamp("MAX(TimeBanned)");
                if (LatestBanTime != null)
                    sender.sendMessage(this.fmt("&cLast Banned: &7%s (%s)", TimeUtil.TimeString(LatestBanTime), TimeUtil.Expires(LatestBanTime)));
                else
                    sender.sendMessage(this.fmt("&cLast Banned: &7Never Banned."));

                if (!IsBanned)
                {
                    sender.sendMessage(this.GetLineItems(result, "UnbanExecutioners", "Unban Executioner(s)", ", ", IsBanned));
                    sender.sendMessage(this.GetLineItems(result, "UnbanReasons", "Unban Reason(s)", ", ", IsBanned));
                }

                sender.sendMessage(String.format(ChatColor.RED + "Currently Banned: %s", IsBanned ? (ChatColor.YELLOW + "YES") : (ChatColor.GREEN + "No")));
                sender.sendMessage(this.fmt("&8&m------------------------------"));
                */
                return true;
            }
            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }
    
    private boolean HandleClearHistory(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.clearhistory"))
            return false;
            
        try 
        {
            // They only need the players name, nothing else.
            if (args.length == 1)
            {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

                if (target == null)
                {
                    sender.sendMessage(String.format("Player \"%s\" does not exist!", target));
                    return true;
                }

                // Also unban the user (as they no longer have any history)
                PreparedStatement pst2 = self.connection.prepareStatement("DELETE FROM Punishments WHERE UUID = ? AND AppealStaff != NULL AND WarningAck != NULL");
                pst2.setString(1, target.getUniqueId().toString());
                pst2.executeUpdate();

                // Send response.
                sender.sendMessage(ChatColor.RED + "Cleared history of " + target.getName());
            }
            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
        return false;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Handle the History command
        if (command.getName().equalsIgnoreCase("history") || command.getName().equalsIgnoreCase("h"))
            return this.HandleHistory(sender, command, label, args);

        // Handle the clear history
        if (command.getName().equalsIgnoreCase("clearhistory") || command.getName().equalsIgnoreCase("ch"))
            return this.HandleClearHistory(sender, command, label, args);
            
        // Invalid command.
        return false;
    }
}