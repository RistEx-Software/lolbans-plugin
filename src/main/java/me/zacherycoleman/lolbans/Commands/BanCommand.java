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
import me.zacherycoleman.lolbans.Utils.DatabaseUtil;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.User;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.lang.model.util.ElementScanner6;


public class BanCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.ban") || ((Player)sender).isOp())));
        
        if (command.getName().equalsIgnoreCase("ban"))
        {
            if (SenderHasPerms)
            {
                try 
                {
                    // just incase someone, magically has a 1 char name........
                    if (!(args.length < 2 || args == null))
                    {
                        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length )) : args[1];
                        reason = reason.replace(",", "");
                        OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                        Timestamp bantime = null;

                        if (target == null)
                        {
                            sender.sendMessage(String.format("Player \"%s\" does not exist!", args[0]));
                            return true;
                        }

                        if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                        {
                            sender.sendMessage(Configuration.CannotBanSelf);
                            return true;
                        }

                        if (User.IsPlayerBanned(target))
                        {
                            sender.sendMessage(String.format("Player \"%s\" is already banned!", target.getName()));
                            return true;
                        }

                        // Parse ban time.
                        if (!args[1].trim().contentEquals("0") && !args[1].trim().contentEquals("*"))
                        {
                            Optional<Long> dur = TimeUtil.Duration(args[1]);
                            if (dur.isPresent())
                                bantime = new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                            else
                            {
                                sender.sendMessage(Configuration.Prefix + Configuration.InvalidSyntax);
                                return false;
                            }
                        }

                        // Prepare our reason
                        boolean silent = reason.contains("-s");
                        reason = reason.replace("-s", "").trim();

                        String banid = BanID.GenerateID(DatabaseUtil.GenID());

                        Future<Boolean> HistorySuccess = DatabaseUtil.InsertHistory(target.getUniqueId().toString(), target.getName(), reason, sender, banid, bantime);
                        Future<Boolean> BanSuccess = DatabaseUtil.InsertBan(target.getUniqueId().toString(), target.getName(), reason, sender, banid, bantime);

                        // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String BanID, Timestamp BanTime)
                        if (!BanSuccess.get())
                        {
                            sender.sendMessage("\u00A7CThe server encountered an error, please try again later.3");
                            return true;
                        }

                        // Add everything to the history DB
                        if (!HistorySuccess.get())
                        {
                            sender.sendMessage("\u00A7CThe server encountered an error, please try again later.4");
                            return true;
                        }

                        // Kick the player first
                        if (target instanceof Player)
                            User.KickPlayer(sender.getName(), (Player)target, banid, reason, bantime);
                    
                        // Log to console.
                        if (silent)
                        {
                            Configuration.BanAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("BanAnnouncment").replace("%player%", target.getName()));
                            Bukkit.getConsoleSender().sendMessage(Configuration.SilentBanAnnouncment);
                        }
                        else
                        {
                            Configuration.BanAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("BanAnnouncment").replace("%player%", target.getName())
                            .replace("%reason%", reason).replace("%banner%", sender.getName()));
                            Bukkit.getConsoleSender().sendMessage(Configuration.BanAnnouncment);
                        }
                            
                        for (Player p : Bukkit.getOnlinePlayers())
                        {
                            if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                                continue;

                            if (silent)
                            {
                                Configuration.SilentBanAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("SilentBanAnnouncment").replace("%player%", target.getName())
                                .replace("%reason%", reason).replace("%banner%", sender.getName()));

                                p.sendMessage(Configuration.SilentBanAnnouncment);                                
                            }
                            else
                            {
                                Configuration.BanAnnouncment = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("BanAnnouncment").replace("%player%", target.getName())
                                .replace("%reason%", reason).replace("%banner%", sender.getName()));

                                p.sendMessage(Configuration.BanAnnouncment);                                
                            }

                            //p.sendMessage(String.format("\u00A7c%s \u00A77has banned \u00A7c%s\u00A77: \u00A7c%s\u00A77%s\u00A7r", 
                            //                            sender.getName(), target.getName(), reason, (silent ? " [SILENT]" : "")));
                        }

                        // Send to Discord. (New method)
                        if (sender instanceof ConsoleCommandSender)
                            DiscordUtil.Send(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, banid, bantime, silent);
                        else
                        {
                            DiscordUtil.Send(sender.getName().toString(), target.getName(), 
                                    ((Entity) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason,
                                    banid, bantime, silent);
                        }

                        return true;

                    }
                    else
                    {
                        sender.sendMessage("\u00A7CInvalid Syntax!");
                        return false; // Show syntax.
                    }
                }
                catch (Exception e)
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