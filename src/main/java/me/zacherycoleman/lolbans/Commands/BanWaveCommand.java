package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Runnables.BanWaveRunnable;
import me.zacherycoleman.lolbans.Utils.BanID;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;

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
                    return User.NoSuchPlayer(sender, args[0], true);

                if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                {
                    sender.sendMessage(Messages.Prefix + Messages.GetMessages().Translate("Banwave.CannotAddSelf", null));
                    return true;
                }

                if (User.IsPlayerInWave(target))
                    return User.PlayerOnlyVariableMessage("Banwave.PlayerIsInBanWave", sender, target.getName(), true);

                // Prepare our reason
                boolean silent = reason.contains("-s");
                reason = reason.replace("-s", "").trim();
                int i = 1;

                // Get the latest ID of the banned players to generate a BanID form it.
                String banid = BanID.GenerateID(DatabaseUtil.GenID());
                
                // Preapre a statement
                PreparedStatement pst = self.connection.prepareStatement("INSERT INTO BanWave (UUID, PlayerName, IPAddress, Reason, Executioner, BanID) VALUES (?, ?, ?, ?, ?, ?)");
                pst.setString(i++, target.getUniqueId().toString());
                pst.setString(i++, target.getName());
                if (target.isOnline())
                    pst.setString(i++, ((Player)target).getAddress().getAddress().getHostAddress());
                else
                    pst.setString(i++, "UNKNOWN");
                pst.setString(i++, reason);
                pst.setString(i++, sender.getName());
                pst.setString(i++, banid);

                // Commit to the database.
                pst.executeUpdate();

                // Reply to the sender that we added the message.
                User.PlayerOnlyVariableMessage("Banwave.AddedToWave", sender, target.getName(), false);

                // Log to console.
                // TODO: Log to everyone with the alert permission?
                Bukkit.getConsoleSender().sendMessage(String.format(Messages.Prefix + "\u00A7c%s \u00A77has added \u00A7c%s\u00A77 to the banwave: \u00A7c%s\u00A77%s\u00A7r", 
                sender.getName(), target.getName(), reason, (silent ? " [silent]" : "")));

                // Send to Discord.
                DiscordUtil.SendBanWaveAdd(sender.getName(), target.getName(), target.getUniqueId().toString(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", reason, banid);
                return true;
            }
            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
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
            return User.NoSuchPlayer(sender, args[0], true);

        // Check if they're banned normally
        if (!User.IsPlayerInWave(target))
            return User.PlayerOnlyVariableMessage("Banwave.PlayerNotInBanWave", sender, target.getName(), true);

        try
        {
            // God forbid any fucking thing does `return this;` so we can chain calls together like a builder. 
            PreparedStatement fuckingdumb = self.connection.prepareStatement("DELETE FROM BanWave WHERE UUID = ?");
            fuckingdumb.setString(1, target.getUniqueId().toString());
            fuckingdumb.executeUpdate();

            User.PlayerOnlyVariableMessage("Banwave.RemovedFromWave", sender, target.getName(), false);
            return true;
            
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }

    private boolean BanWaveExecute(CommandSender sender, Command command, String label, String[] args)
    {
        User.PlayerOnlyVariableMessage("Banwave.BanwaveStart", sender, sender.getName(), false);
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