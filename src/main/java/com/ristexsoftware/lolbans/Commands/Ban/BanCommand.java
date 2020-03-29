package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.PunishID;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Future;

public class BanCommand implements CommandExecutor
{
    //private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (PermissionUtil.Check(sender, "lolbans.ban"))
        {
            try 
            {
                // just incase someone, magically has a 1 char name........
                if (!(args.length < 2 || args == null))
                {
                    String reason = Messages.ConcatenateRest(args, 2).trim();
                    OfflinePlayer target = User.FindPlayerByAny(args[0]);
                    Timestamp bantime = null;

                    // So, we need to get the UUID of punisher so we can display the icons on the website
                    // but, because console doesn't have a UUID, we have to make on, so if it's console
                    // that's executing the command, just make their uuid console, plus it's simple to type :P
                    String euuid = null;

                    if (sender instanceof ConsoleCommandSender)
                        euuid = "console";
                    else if (sender instanceof Player)
                        euuid = ((Player) sender).getUniqueId().toString();

                    // debugging
                    Long now = System.currentTimeMillis();

                    if (target == null)
                        return User.NoSuchPlayer(sender, args[0], true);
                    

                    if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                        return User.PlayerOnlyVariableMessage("Ban.CannotBanSelf", sender, target.getName(), true);
                        

                    if (User.IsPlayerBanned(target))
                        return User.PlayerOnlyVariableMessage("Ban.PlayerIsBanned", sender, target.getName(), true);
                    

                    // Parse ban time.
                    if (!args[1].trim().contentEquals("0") && !args[1].trim().contentEquals("*"))
                    {
                        Optional<Long> dur = TimeUtil.Duration(args[1]);
                        if (dur.isPresent())
                            bantime = new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                        else
                        {
                            sender.sendMessage(Messages.InvalidSyntax);
                            return false;
                        }
                    }
                    

                    // Prepare our reason
                    boolean silent = args.length > 3 ? args[2].equalsIgnoreCase("-s") : false;

                    // Because dumbfuck java and it's "ItS nOt FiNaL"
                    // but really? what the fuck java? Now I have to have all of these "fuckingjava" strings.. thanks.......
                    final String FuckingJava = new String(reason);
                    final String FuckingJava2 = new String(bantime != null ? String.format("%s (%s)", TimeUtil.TimeString(bantime), TimeUtil.Expires(bantime)) : "Never");
                    final String FuckingJava3 = new String(bantime != null ? TimeUtil.Expires(bantime) : "Never");
                    final String FuckingJava4 = new String(bantime != null ? TimeUtil.TimeString(bantime) : "Never");
                    

                    // Get our ban id based on the latest id in the database.
                    String banid = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));
                    

                    // Execute queries to get the bans.
                    Future<Boolean> BanSuccess = DatabaseUtil.InsertPunishment(PunishmentType.PUNISH_BAN, target.getUniqueId().toString(), target.getName(), target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN", reason, sender, euuid, banid, bantime);
                    
                    // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String PunishID, Timestamp BanTime)
                    if (!BanSuccess.get())
                    {
                        sender.sendMessage(Messages.ServerError);
                        return true;
                    } 
                    

                    // Kick the player first, they're officially banned.
                    if (target instanceof Player)
                        User.KickPlayer(sender.getName(), (Player)target, banid, reason, bantime);

                    
                    // Format our messages.
                    String BanAnnouncement = Messages.Translate(silent ? "Ban.SilentBanAnnouncement" : "Ban.BanAnnouncement",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("banner", sender.getName());
                            put("banid", banid);
                            put("fullexpiry", FuckingJava2);
                            put("expiryduration", FuckingJava3);
                            put("dateexpiry", FuckingJava4);
                        }}
                    );

                    // Send it to the console.
                    Bukkit.getConsoleSender().sendMessage(BanAnnouncement);
                
                    // Send messages to all players (if not silent) or only to admins (if silent)
                    for (Player p : Bukkit.getOnlinePlayers())
                    {
                        if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                            continue;

                        p.sendMessage(BanAnnouncement);
                    }

                    String SimplifiedMessage = Messages.Translate(silent ? "Discord.SimpMessageSilentBan" : "Discord.SimpMessageBan",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {
                            private static final long serialVersionUID = 1L;
                            {
                                put("player", target.getName());
                                put("reason", FuckingJava);
                                put("banner", sender.getName());
                                put("banid", banid);
                                put("fullexpiry", FuckingJava2);
                                put("expiryduration", FuckingJava3);
                                put("dateexpiry", FuckingJava4);
                            }
                        }
                    );

                    // debugging
                    Long later = System.currentTimeMillis();
                    Long thingy = later - now;
                    sender.sendMessage(ChatColor.GRAY + "Done! " + ChatColor.RED + thingy + "ms");

                    // Send to Discord. (New method)
                    if (DiscordUtil.UseSimplifiedMessage == true)
                        DiscordUtil.SendFormatted(SimplifiedMessage);
                    else
                        DiscordUtil.SendDiscord(sender, "banned", target, reason, banid, bantime, silent);
                }
                else
                {
                    sender.sendMessage(Messages.InvalidSyntax);
                    return false; // Show syntax.
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                sender.sendMessage(Messages.ServerError);
            }
        }
        // They're denied perms, just return.
        return true;
    }
}