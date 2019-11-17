package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Runnables.BanWaveRunnable;
import me.zacherycoleman.lolbans.Utils.BanID;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;

public class BanWaveCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    private boolean BanWaveAdd(CommandSender sender, Command command, String label, String[] args)
    {
        try 
        {
            // just incase someone, magically has a 1 char name........
            if (!(args.length < 2 || args == null))
            {
                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                reason = reason.replace(",", "");
                OfflinePlayer target = User.FindPlayerByBanID(args[0]);

                if (target == null)
                {
                    sender.sendMessage(String.format(Configuration.Prefix + ChatColor.RED + "Player \"%s\" does not exist!", args[0]));
                    return true;
                }

                if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                {
                    sender.sendMessage(Configuration.Prefix + ChatColor.RED + "You cannot add yourself to the ban wave.");
                    return true;
                }

                if (User.IsPlayerInWave(target))
                {
                    sender.sendMessage(String.format(Configuration.Prefix + "Player \"%s\" is already in the ban wave!", target.getName()));
                    return true;
                }

                // Prepare our reason
                boolean silent = reason.contains("-s");
                reason = reason.replace("-s", "").trim();

                // Get the latest ID of the banned players to generate a BanID form it.
                ResultSet ids = self.connection.createStatement().executeQuery("SELECT MAX(id) FROM BanWave");
                int id = 1;
                if (ids.next())
                {
                    if (!ids.wasNull())
                        id = ids.getInt(1);
                }
                String banid = BanID.GenerateID(id);
                
                // Preapre a statement
                PreparedStatement pst = self.connection.prepareStatement("INSERT INTO BanWave (UUID, PlayerName, Reason, Executioner, BanID) VALUES (?, ?, ?, ?, ?)");
                pst.setString(1, target.getUniqueId().toString());
                pst.setString(2, target.getName());
                pst.setString(3, reason);
                pst.setString(4, sender.getName());
                pst.setString(5, banid);

                // Commit to the database.
                pst.executeUpdate();

                sender.sendMessage(ChatColor.RED + target.getName() + " has been added to the next ban wave.");

                // Log to console.
                Bukkit.getConsoleSender().sendMessage(String.format(Configuration.Prefix + "\u00A7c%s \u00A77has added \u00A7c%s\u00A77 to the banwave: \u00A7c%s\u00A77%s\u00A7r", 
                sender.getName(), target.getName(), reason, (silent ? " [silent]" : "")));

                // Send to Discord.

                //String sender, String target, String TargetUUID, String SenderUUID, String reason, String BanID
                DiscordUtil.SendBanWaveAdd(sender.getName(), target.getName(), target.getUniqueId().toString(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", reason, banid);
                

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

    private boolean BanWaveRemove(CommandSender sender, Command command, String label, String[] args)
    {
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length )) : args[1];
        reason = reason.replace(",", "");

        // Check and make sure the user actually exists.
        OfflinePlayer target = User.FindPlayerByBanID(args[0]);
        if (target == null)
        {
            sender.sendMessage(String.format(Configuration.Prefix + "Player \"%s\" does not exist!", args[0]));
            return true;
        }


        // Check if they're banned normally
        if (!User.IsPlayerInWave(target))
        {
            sender.sendMessage(String.format("%s is not part of any ban waves.", target.getName()));
            return true;
        }

        try
        {
            // God forbid any fucking thing does `return this;` so we can chain calls together like a builder. 
            PreparedStatement fuckingdumb = self.connection.prepareStatement("DELETE FROM BanWave WHERE UUID = ?");
            fuckingdumb.setString(1, target.getUniqueId().toString());
            fuckingdumb.executeUpdate();

            sender.sendMessage(String.format("%s has been removed from the next ban wave.", target.getName()));
            return true;
            
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            sender.sendMessage("The server encountered an error.");
            return true;
        }
    }

    private boolean BanWaveExecute(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Configuration.Prefix + ChatColor.GRAY + "Starting ban wave.");
        BanWaveRunnable bwr = new BanWaveRunnable();
        bwr.sender = sender;
        bwr.runTaskAsynchronously(self);
        return false;
    }
        
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.banwave") || ((Player)sender).isOp())));
        
        if (command.getName().equalsIgnoreCase("banwave"))
        {
            if (SenderHasPerms)
            {
                //System.out.println("User has perms, args: " + String.join(" ", args));
                // Invalid arguments.
                if (args.length < 1)
                    return false;

                String SubCommand = args[0];
                String[] Subargs = Arrays.copyOfRange(args, 1, args.length);

                //System.out.println("Subcommand: " + SubCommand + " subargs: " + String.join(" ", Subargs));

                if (SubCommand.equalsIgnoreCase("add"))
                {
                    return this.BanWaveAdd(sender, command, label, Subargs);
                }
                else if (SubCommand.equalsIgnoreCase("remove") || SubCommand.equalsIgnoreCase("rm")) 
                {
                    return this.BanWaveRemove(sender, command, label, Subargs);
                }
                else if (SubCommand.equalsIgnoreCase("enforce") || SubCommand.equalsIgnoreCase("run") || 
                    SubCommand.equalsIgnoreCase("start") || SubCommand.equalsIgnoreCase("exec") || SubCommand.equalsIgnoreCase("execute"))
                {
                    return this.BanWaveExecute(sender, command, label, Subargs);
                }
                // If they run a sub command we don't know
                return false;
            }
            // They're denied perms, just return.
            return true;
        }
        // Invalid command.
        return false;
    }
}