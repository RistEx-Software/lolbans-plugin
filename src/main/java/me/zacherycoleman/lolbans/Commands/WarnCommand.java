package me.zacherycoleman.lolbans.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import me.zacherycoleman.lolbans.Main;
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
                        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length )) : args[1];
                        reason = reason.replace(",", "");
                        OfflinePlayer target = User.FindPlayerByBanID(args[0]);

                        if (target == null)
                        {
                            sender.sendMessage(String.format("Player \"%s\" does not exist!", args[0]));
                            return true;
                        }

                        // Prepare our reason
                        boolean silent = reason.contains("-s");
                        reason = reason.replace("-s", "").trim();


                        // Get the latest ID of the banned players to generate a BanID form it.
                        ResultSet ids = self.connection.createStatement().executeQuery("SELECT MAX(id) FROM BannedPlayers");
                        int id = 1;
                        if (ids.next())
                        {
                            if (!ids.wasNull())
                                id = ids.getInt(1);
                        }
                        String warnid = BanID.GenerateID(id);

                        // Preapre a statement
                        PreparedStatement pst = self.connection.prepareStatement("INSERT INTO Warnings (UUID, PlayerName, Reason, Executioner, WarnID, Accepted) VALUES (?, ?, ?, ?, ?, ?)");
                        pst.setString(1, target.getUniqueId().toString());
                        pst.setString(2, target.getName());
                        pst.setString(3, reason);
                        pst.setString(4, sender.getName());
                        pst.setString(5, warnid);
                        pst.setBoolean(6, false);

                        // Commit to the database.
                        pst.executeUpdate();
                            
                        Configuration.WarnedMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("WarnedMessage").replace("%player%", target.getName())
                            .replace("%reason%", reason).replace("%warnid%", warnid).replace("%warner%", sender.getName()));

                        if (target.isOnline())
                        {
                            User u = Main.USERS.get(target.getUniqueId());
                            u.SetWarned(true, ((Player)target).getLocation(), Configuration.WarnedMessage);
                            u.SendMessage(Configuration.WarnedMessage);

                            // Send them a box as well. This will disallow them from sending move events.
                            // However, client-side enforcement is not guaranteed so we also enforce the
                            // same thing using the MovementListener, this just helps stop rubberbanding.
                            u.SpawnBox(true, null);
                        }
                    
                        // Log to console.
                        if (silent)
                        {
                            Configuration.SilentWarnAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("SilentWarnAnnouncment").replace("%player%", target.getName())
                            .replace("%reason%", reason).replace("%warner%", sender.getName()));
                            Bukkit.getConsoleSender().sendMessage(Configuration.SilentWarnAnnouncment);
                        }
                        else
                        {
                            Configuration.WarnAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("WarnAnnouncment").replace("%player%", target.getName())
                            .replace("%reason%", reason).replace("%warner%", sender.getName()));
                            Bukkit.getConsoleSender().sendMessage(Configuration.WarnAnnouncment);
                        }
                            
                        // Post that to the database.
                        for (Player p : Bukkit.getOnlinePlayers())
                        {
                            if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                                continue;

                            if (silent)
                            {
                                Configuration.SilentWarnAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("SilentWarnAnnouncment").replace("%player%", target.getName())
                                .replace("%reason%", reason).replace("%warner%", sender.getName()));
                                p.sendMessage(Configuration.SilentWarnAnnouncment);
                            }
                            else
                            {
                                Configuration.WarnAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("WarnAnnouncment").replace("%player%", target.getName())
                                .replace("%reason%", reason).replace("%warner%", sender.getName()));
                                p.sendMessage(Configuration.WarnAnnouncment);
                            }

                            //p.sendMessage(String.format("\u00A7c%s \u00A77has banned \u00A7c%s\u00A77: \u00A7c%s\u00A77%s\u00A7r", 
                            //                            sender.getName(), target.getName(), reason, (silent ? " [SILENT]" : "")));
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
            // They're denied perms, just return.
            return true;
        }
        // Invalid command.
        return false;
    }
}