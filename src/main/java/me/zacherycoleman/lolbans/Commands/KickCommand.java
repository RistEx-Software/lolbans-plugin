package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.BanID;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class KickCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.kick") || ((Player)sender).isOp())));
        
        if (command.getName().equalsIgnoreCase("kick"))
        {
            if (SenderHasPerms)
            {
                try 
                {
                    // just incase someone, magically has a 1 char name........
                    if (!(args.length < 1 || args == null))
                    {
                        OfflinePlayer target = User.FindPlayerByBanID(args[0]);

                        if (args.length < 2)
                            return User.PlayerOnlyVariableMessage("InvalidArguments", sender, target.getName(), true);

                        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : args[1];
                        reason = reason.replace(",", "").trim();

                        if (target == null)
                            return User.NoSuchPlayer(sender, args[0], true);

                        if (!target.isOnline())
                            return User.PlayerIsOffline(sender, args[0], true);

                        if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                            return User.PlayerOnlyVariableMessage("Kick.CannotKickSelf", sender, target.getName(), true);

                        // Prepare our reason
                        boolean silent = false;
                        if (args.length > 2)
                            silent = args[1].equalsIgnoreCase("-s");


                        final String FuckingJava = new String(reason);
                        int i = 1;

                        // Get the latest ID of the banned players to generate a BanID form it.
                        String kickid = BanID.GenerateID(DatabaseUtil.GenID());

                        // Preapre a statement
                        PreparedStatement pst = self.connection.prepareStatement("INSERT INTO Kicks (UUID, PlayerName, IPAddress, Reason, Executioner, KickID) VALUES (?, ?, ?, ?, ?, ?)");
                        //CREATE TABLE IF NOT EXISTS Kicks (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT
                        // NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, KickID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").execute();
                        pst.setString(i++, target.getUniqueId().toString());
                        pst.setString(i++, target.getName());
                        if (target.isOnline())
                            pst.setString(i++, ((Player)target).getAddress().toString());
                        else
                            pst.setString(i++, "UNKNOWN");
                        pst.setString(i++, reason);
                        pst.setString(i++, sender.getName());
                        pst.setString(i++, kickid);

                        // Commit to the database.
                        pst.executeUpdate();

                        Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("kickid", kickid);
                            put("kicker", sender.getName());
                        }};

                        String KickAnnouncement = Messages.GetMessages().Translate(silent ? "Warn.SilentKickAnnouncement" : "Kick.KickAnnouncement", Variables);

                        // Kick the player
                        User.KickPlayer(sender.getName(), (Player) target, kickid, reason);
                    
                        // Log to console.
                        Bukkit.getConsoleSender().sendMessage(KickAnnouncement);
                            
                        // Send the message to all online players.
                        for (Player p : Bukkit.getOnlinePlayers())
                        {
                            if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                                continue;

                            p.sendMessage(KickAnnouncement);
                        }

                        // Send to Discord. (New method)
                        if (sender instanceof ConsoleCommandSender)
                            DiscordUtil.SendKick(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, kickid, silent);
                        else
                        {
                            DiscordUtil.SendKick(sender.getName().toString(), target.getName(), 
                                    ((Entity) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, kickid, silent);
                        }

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
            // They're denied perms, just return.
            return true;
        }
        // Invalid command.
        return false;
    }
}