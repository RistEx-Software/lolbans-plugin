package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Runnables.BanWaveRunnable;
import com.ristexsoftware.lolbans.Utils.BanID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

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
        if (!PermissionUtil.Check(sender, "lolbans.banwave.add"))
            return true;

        try 
        {
            // just incase someone, magically has a 1 char name........
            if (!(args.length < 2 || args == null))
            {
                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                reason = reason.replace(",", "").trim();
                OfflinePlayer target = User.FindPlayerByAny(args[0]);

                if (target == null)
                    return User.NoSuchPlayer(sender, args[0], true);

                if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                {
                    sender.sendMessage(Messages.Translate("Banwave.CannotAddSelf", null));
                    return true;
                }

                if (User.IsPlayerInWave(target))
                    return User.PlayerOnlyVariableMessage("Banwave.PlayerIsInBanWave", sender, target.getName(), true);


                final String FuckingJava = new String(reason);

                // Prepare our reason
                boolean silent = args.length > 2 ? args[1].equalsIgnoreCase("-s") : false;

                // Get the latest ID of the banned players to generate a BanID form it.
                String banid = BanID.GenerateID(DatabaseUtil.GenID("BanWave"));
                    
                // Preapre a statement
                int i = 1;
                PreparedStatement pst = self.connection.prepareStatement("INSERT INTO BanWave (UUID, PlayerName, IPAddress, Reason, Executioner, PunishID) VALUES (?, ?, ?, ?, ?, ?)");
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
                // FIXME: Silents?
                // Format our messages.
                String BanWaveAnnouncement = Messages.Translate(silent ? "BanWave.AddedToWave" : "BanWave.AddedToWave",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("banner", sender.getName());
                        put("banid", banid);
                        put("silent", (silent ? " [silent]" : ""));
                    }}
                );
                Bukkit.getConsoleSender().sendMessage(BanWaveAnnouncement);

                // Send to Discord.
                // TODO: Uhhh this is different now?
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
        if (!PermissionUtil.Check(sender, "lolbans.banwave.remove"))
            return true;

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length )) : args[1];
        reason = reason.replace(",", "");

        // Check and make sure the user actually exists.
        OfflinePlayer target = User.FindPlayerByAny(args[0]);
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
        if (!PermissionUtil.Check(sender, "lolbans.banwave.enforce"))
            return true;

        User.PlayerOnlyVariableMessage("Banwave.BanwaveStart", sender, sender.getName(), false);
        BanWaveRunnable bwr = new BanWaveRunnable();
        bwr.sender = sender;
        bwr.runTaskAsynchronously(self);
        return false;
    }
        
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.banwave"))
            return true;
        
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
}