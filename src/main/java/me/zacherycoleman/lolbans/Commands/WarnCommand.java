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


public class WarnCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.warn") || ((Player)sender).isOp())));
        
        if (command.getName().equalsIgnoreCase("warn"))
        {
            if (SenderHasPerms)
            {
                try 
                {
                    // just incase someone, magically has a 1 char name........
                    if (!(args.length < 1 || args == null))
                    {
                        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : args[1];
                        reason = reason.replace(",", "");
                        OfflinePlayer target = User.FindPlayerByBanID(args[0]);

                        if (target == null)
                            return User.NoSuchPlayer(sender, args[0], true);

                        // Prepare our reason
                        boolean silent = reason.contains("-s");
                        reason = reason.replace("-s", "").trim();
                        final String FuckingJava = new String(reason);
                        int i = 1;

                        // Get the latest ID of the banned players to generate a BanID form it.
                        String warnid = BanID.GenerateID(DatabaseUtil.GenID());

                        // Preapre a statement
                        PreparedStatement pst = self.connection.prepareStatement("INSERT INTO Warnings (UUID, PlayerName, IPAddress, Reason, Executioner, WarnID, Accepted) VALUES (?, ?, ?, ?, ?, ?, ?)");
                        pst.setString(i++, target.getUniqueId().toString());
                        pst.setString(i++, target.getName());
                        if (target.isOnline())
                            pst.setString(i++, ((Player)target).getAddress().toString());
                        else
                            pst.setString(i++, "UNKNOWN");
                        pst.setString(i++, reason);
                        pst.setString(i++, sender.getName());
                        pst.setString(i++, warnid);
                        pst.setBoolean(i++, false);

                        // Commit to the database.
                        pst.executeUpdate();

                        Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("warnid", warnid);
                            put("warner", sender.getName());
                        }};
                            
                        String WarnedMessage = Messages.GetMessages().Translate("Warn.WarnedMessage", Variables);
                        String WarnAnnouncement = Messages.GetMessages().Translate(silent ? "SilentWarnAnnouncment" : "WarnAnnouncment", Variables);

                        // Send a message to the player
                        if (target.isOnline())
                        {
                            User u = Main.USERS.get(target.getUniqueId());
                            u.SetWarned(true, ((Player)target).getLocation(), WarnedMessage);
                            u.SendMessage(WarnedMessage);

                            // Send them a box as well. This will disallow them from sending move events.
                            // However, client-side enforcement is not guaranteed so we also enforce the
                            // same thing using the MovementListener, this just helps stop rubberbanding.
                            u.SpawnBox(true, null);
                        }
                    
                        // Log to console.
                        Bukkit.getConsoleSender().sendMessage(WarnAnnouncement);
                            
                        // Send the message to all online players.
                        for (Player p : Bukkit.getOnlinePlayers())
                        {
                            if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                                continue;

                            p.sendMessage(WarnAnnouncement);
                        }

                        // Send to Discord. (New method)
                        if (sender instanceof ConsoleCommandSender)
                            DiscordUtil.SendWarn(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, warnid, silent);
                        else
                        {
                            DiscordUtil.SendWarn(sender.getName().toString(), target.getName(), 
                                    ((Entity) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, warnid, silent);
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