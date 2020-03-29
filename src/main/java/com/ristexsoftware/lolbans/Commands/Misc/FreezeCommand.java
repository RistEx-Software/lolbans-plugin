package com.ristexsoftware.lolbans.Commands.Misc;

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

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;


public class FreezeCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean SenderHasPerms = (sender instanceof ConsoleCommandSender || 
                                 (!(sender instanceof ConsoleCommandSender) && (((Player)sender).hasPermission("lolbans.freeze") || ((Player)sender).isOp())));
        
        if (command.getName().equalsIgnoreCase("freeze"))
        {
            if (SenderHasPerms)
            {
                try 
                {
                    // just incase someone, magically has a 1 char name........
                    if (args.length >= 1)
                    {
                        OfflinePlayer target = User.FindPlayerByAny(args[0]);

                        if (target == null)
                            return User.NoSuchPlayer(sender, args[0], true);

                        boolean silent = args.length > 1 ? args[1].equalsIgnoreCase("-s") : false;
                        User u = Main.USERS.get(target.getUniqueId());

                        if (!u.IsWarn)
                        {
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                            {{
                                put("player", target.getName());
                                put("freezer", sender.getName());
                            }};
                                
                            String FrozenMessage = Messages.Translate("Freeze.FrozenMessage", Variables);
                            String FrozenAnnouncement = Messages.Translate(silent ? "Freeze.SilentFreezeAnnouncement" : "Freeze.FreezeAnnouncement", Variables);
    
                            // Send a message to the player
                            if (target.isOnline())
                            {
                                //u.SetFrozen(true, ((Player)target).getLocation());
                                u.SendMessage(FrozenMessage);
    
                                // Send them a box as well. This will disallow them from sending move events.
                                // However, client-side enforcement is not guaranteed so we also enforce the
                                // same thing using the MovementListener, this just helps stop rubberbanding.
                                u.SpawnBox(true, null);

                                // Log to console.
                                Bukkit.getConsoleSender().sendMessage(FrozenAnnouncement);
                                    
                                // Send the message to all online players.
                                for (Player p : Bukkit.getOnlinePlayers())
                                {
                                    if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                                        continue;
        
                                    p.sendMessage(FrozenAnnouncement);
                                }
        
                                // Send to Discord. (New method)
                                // TODO: actually make another discord method for this.........
                                /*
                                if (sender instanceof ConsoleCommandSender)
                                    DiscordUtil.SendWarn(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, warnid, silent);
                                else
                                {
                                    DiscordUtil.SendWarn(sender.getName().toString(), target.getName(), 
                                            ((Entity) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, warnid, silent);
                                }
                                */
                            }
                            return true;
                        }
                        else
                        {
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                            {{
                                put("player", target.getName());
                                put("freezer", sender.getName());
                            }};
                                
                            String FrozenMessage = Messages.Translate("Freeze.UnFrozenMessage", Variables);
                            String FrozenAnnouncement = Messages.Translate(silent ? "Freeze.SilentUnfreezeAnnouncement" : "Freeze.UnfreezeAnnouncement", Variables);
    
                            // Send a message to the player
                            if (target.isOnline())
                            {
                                
                                //u.SetFrozen(false, null);
                                u.SendMessage(FrozenMessage);
    
                                // Send them a box as well. This will disallow them from sending move events.
                                // However, client-side enforcement is not guaranteed so we also enforce the
                                // same thing using the MovementListener, this just helps stop rubberbanding.
                                u.SpawnBox(true, null);

                                // Log to console.
                                Bukkit.getConsoleSender().sendMessage(FrozenAnnouncement);
                                    
                                // Send the message to all online players.
                                for (Player p : Bukkit.getOnlinePlayers())
                                {
                                    if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                                        continue;
        
                                    p.sendMessage(FrozenAnnouncement);
                                }
        
                                // Send to Discord. (New method)
                                // TODO: actually make another discord method for this.........
                                /*
                                if (sender instanceof ConsoleCommandSender)
                                    DiscordUtil.SendWarn(sender.getName().toString(), target.getName(), "f78a4d8d-d51b-4b39-98a3-230f2de0c670", target.getUniqueId().toString(), reason, warnid, silent);
                                else
                                {
                                    DiscordUtil.SendWarn(sender.getName().toString(), target.getName(), 
                                            ((Entity) sender).getUniqueId().toString(), target.getUniqueId().toString(), reason, warnid, silent);
                                }
                                */
                            }
                            return true;
                        }

                    }
                    else
                    {
                        sender.sendMessage(Messages.InvalidSyntax);
                        return false; // Show syntax.
                    }
                }
                catch (InvalidConfigurationException e)
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