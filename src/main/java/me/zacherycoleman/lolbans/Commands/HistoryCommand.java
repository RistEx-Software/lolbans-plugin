package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;

import java.util.Arrays;
import java.sql.*;

public class HistoryCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

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
            return ret;
        }
    }

    private boolean HandleHistory(CommandSender sender, boolean SenderHasPerms, Command command, String label, String[] args)
    {
        if (!SenderHasPerms)
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

                // Preapre a statement
                PreparedStatement pst = self.connection.prepareStatement("SELECT PlayerName, UUID, BanID, Reason, Executioner, TimeBanned, Expiry, UnbanReason, UnbanExecutioner, GROUP_CONCAT(BanID) AS BanIDs, GROUP_CONCAT(Reason) AS Reasons, GROUP_CONCAT(Executioner) AS Executioners, GROUP_CONCAT(DISTINCT Executioner) AS Executioners2, GROUP_CONCAT(UnbanReason) AS UnbanReasons, GROUP_CONCAT(UnbanExecutioner) AS UnbanExecutioners, GROUP_CONCAT(DISTINCT UnbanExecutioner) AS UnbanExecutioners2, MAX(TimeBanned) FROM BannedHistory WHERE PlayerName = ?");
                pst.setString(1, args[0]);

                // Commit to the database.
                ResultSet result = pst.executeQuery();
                if (!result.next() || result.wasNull())
                {
                    sender.sendMessage(String.format(ChatColor.RED + "Player " + ChatColor.DARK_RED + "%s " + ChatColor.RED + " has no recorded ban history.", target.getName()));
                    return true;
                }

                // BanID Array
                String biarr[] = this.GetStringOrDefault(result, "BanIDs", "N/A").split(",");
                String LatestBanID = biarr.length == 1 ? biarr[0] : biarr[biarr.length - 1];
                String BanIDs = String.join("; ", Arrays.copyOfRange(biarr, 0, biarr.length - 1));

                // Exec Array
                String ExecArr[] = this.GetStringOrDefault(result, "Executioners", "N/A").split(",");
                String LatestExec = ExecArr.length == 1 ? ExecArr[0] : ExecArr[ExecArr.length - 1];

                // Because java and SQL are both stupid, we need two of these to output the right executioner in the history command
                String ExecArr2[] = this.GetStringOrDefault(result, "Executioners2", "N/A").split(",");
                String Execs2 = String.join(", ", Arrays.copyOfRange(ExecArr2, 0, ExecArr2.length));

                String Execs = String.join(", ", Arrays.copyOfRange(ExecArr, 0, ExecArr.length - 1));

                // Reasons Array
                String rarr[] = this.GetStringOrDefault(result, "Reasons", "N/A").split(",");
                String LatestReason = rarr.length == 1 ? rarr[0] : rarr[rarr.length - 1];
                String Reasons = String.join(", ", Arrays.copyOfRange(rarr, 0, rarr.length - 1));

                // Unban Reasons Array
                String ubrarr[] = this.GetStringOrDefault(result, "UnbanReasons", "N/A").split(",");
                String LatestUnbanReason = ubrarr.length == 1 ? ubrarr[0] : ubrarr[ubrarr.length - 1];
                String UnbanReasons = String.join(", ", Arrays.copyOfRange(ubrarr, 0, ubrarr.length - 1));

                // Unban Reasons Array
                // The first array uses DISTINCT in SQL, while the other doesn't.
                String ExecUnbanArr[] = this.GetStringOrDefault(result, "UnbanExecutioners", "N/A").split(",");
                String LatestExecUnban = ExecUnbanArr.length == 1 ? ExecUnbanArr[0] : ExecUnbanArr[ExecUnbanArr.length - 1];

                // Because java and SQL are both stupid, we need two of these to output the right executioner in the history command
                String ExecUnbanArr2[] = this.GetStringOrDefault(result, "UnbanExecutioners2", "N/A").split(",");
                String ExecUnbans2 = String.join(", ", Arrays.copyOfRange(ExecUnbanArr2, 0, ExecUnbanArr2.length - 1));

                String ExecUnbans = String.join(", ", Arrays.copyOfRange(ExecUnbanArr, 0, ExecUnbanArr.length - 1));

                // TODO: Figure out why java is being stupid and not returning the right unban execs and ban execs

                sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "------------------------------");
                sender.sendMessage(ChatColor.RED + "Name: " + ChatColor.GRAY + target.getName() + ChatColor.DARK_GRAY  + " [" + target.getUniqueId().toString() + "]");
                
                boolean IsBanned = User.IsPlayerBanned(target);

                if (biarr.length >= 2)
                    sender.sendMessage(String.format(ChatColor.RED + "BanID(s): " + ChatColor.GRAY 
                    + BanIDs + "; %s", IsBanned ? ChatColor.GRAY + "[" + ChatColor.GREEN + LatestBanID + ChatColor.GRAY + "]" : (ChatColor.GRAY + LatestBanID)));
                else
                    sender.sendMessage(String.format(ChatColor.RED + "BanID(s): " + ChatColor.GRAY + LatestBanID));

                if (rarr.length >= 2)
                    sender.sendMessage(String.format(ChatColor.RED + "Reason(s): " + ChatColor.GRAY 
                    + Reasons + ", %s", IsBanned ? ChatColor.GRAY + "[" + ChatColor.GREEN + LatestReason + ChatColor.GRAY + "]" : (ChatColor.GRAY + LatestReason)));
                else
                    sender.sendMessage(String.format(ChatColor.RED + "Reason(s): " + ChatColor.GRAY + LatestReason));
                    
                if (ExecArr.length >= 2)
                    sender.sendMessage(String.format(ChatColor.RED + "Executioner(s): " + ChatColor.GRAY 
                    + Execs2 + ", %s", IsBanned ? ChatColor.GRAY + "[" + ChatColor.GREEN + LatestExec + ChatColor.GRAY + "]" : (ChatColor.GRAY )));
                else
                    sender.sendMessage(String.format(ChatColor.RED + "Executioner(s): " + ChatColor.GRAY + LatestExec));

                sender.sendMessage(String.format(ChatColor.RED + "Times Banned: " + ChatColor.GRAY + (biarr.length - 1)));
                if (IsBanned)
                {
                    Timestamp ts = result.getTimestamp("Expiry");
                    if (ts == null)
                        sender.sendMessage(String.format(ChatColor.RED + "Expiration: " + ChatColor.GRAY + "Indefinite!"));
                    else
                        sender.sendMessage(String.format(ChatColor.RED + "Expiration: " + ChatColor.GRAY + "%s (%s)", TimeUtil.TimeString(ts), TimeUtil.Expires(ts.getTime() / 1000L)));
                }
                else
                    sender.sendMessage(String.format(ChatColor.RED + "Expiration: " + ChatColor.GRAY + "Not Banned"));

                sender.sendMessage(String.format(ChatColor.RED + "Latest Time Banned: " + ChatColor.GRAY + TimeUtil.TimeString(result.getTimestamp("MAX(TimeBanned)"))));

                if (!IsBanned)
                {
                    sender.sendMessage(String.format(ChatColor.RED + "Unban Reason(s): " + ChatColor.GRAY + UnbanReasons + " %s",
                    IsBanned ? "" : "[" + ChatColor.GREEN + LatestUnbanReason + ChatColor.GRAY + "]"));
                    sender.sendMessage(String.format(ChatColor.RED + "Unban Executioner(s): " + ChatColor.GRAY + ExecUnbans2 + "%s",
                    IsBanned ? "" : " [" + ChatColor.GREEN + LatestExecUnban + ChatColor.GRAY + "]"));
                }
                // we can't just "else" this, otherwise if the player has no history, it returns "IS BANNED" and we don't want that.
                else if (IsBanned)
                {
                    sender.sendMessage(String.format(ChatColor.RED + "Unban Reason(s): " + ChatColor.GRAY + UnbanReasons + " [" + ChatColor.GREEN +  "IS BANNED" + ChatColor.GRAY + "]"));
                    sender.sendMessage(String.format(ChatColor.RED + "Unban Executioner(s): " + ChatColor.GRAY + ExecUnbans2 + " [" + ChatColor.GREEN + "IS BANNED" + ChatColor.GRAY + "]"));    
                }

                // boolean mybool = condition ? true : false;
                sender.sendMessage(String.format(ChatColor.RED + "Is Banned: %s", IsBanned ? (ChatColor.YELLOW + "YES") : (ChatColor.GREEN + "No")));
                sender.sendMessage(String.format(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "------------------------------"));
                return true;
            }
            else
            {
                sender.sendMessage("\u00A7CInvalid Syntax!");
                return false; // Show syntax.
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage("\u00A7CThe server encountered an error, please try again later.");
            return true;
        }
    }
    
    private boolean HandleClearHistory(CommandSender sender, boolean SenderHasPerms, Command command, String label, String[] args)
    {
        if (!SenderHasPerms)
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

                // Delete the user's history from the history table
                PreparedStatement pst = self.connection.prepareStatement("DELETE FROM BannedHistory WHERE UUID = ?");
                pst.setString(1, target.getUniqueId().toString());
                pst.executeUpdate();

                // Also unban the user (as they no longer have any history)
                PreparedStatement pst2 = self.connection.prepareStatement("DELETE FROM BannedPlayers WHERE UUID = ?");
                pst2.setString(1, target.getUniqueId().toString());
                pst2.executeUpdate();

                // Send response.
                sender.sendMessage(ChatColor.RED + "Cleared history of " + target.getName());
            }
            else
            {
                sender.sendMessage("\u00A7CInvalid Syntax!");
                return false; // Show syntax.
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage("\u00A7CThe server encountered an error, please try again later.");
            return true;
        }
        return false;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Check if the user has the perms.
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.history") || ((Player)sender).isOp())));
        
        // Handle the History command
        if (command.getName().equalsIgnoreCase("history") || command.getName().equalsIgnoreCase("h"))
            this.HandleHistory(sender, SenderHasPerms, command, label, args);

        // Handle the clear history
        if (command.getName().equalsIgnoreCase("clearhistory") || command.getName().equalsIgnoreCase("ch"))
            this.HandleClearHistory(sender, SenderHasPerms, command, label, args);
            
        // Invalid command.
        return false;
    }
}