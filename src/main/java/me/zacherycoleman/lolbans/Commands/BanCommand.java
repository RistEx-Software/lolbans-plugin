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
import me.zacherycoleman.lolbans.Utils.TranslationUtil;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;
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
        
        if (SenderHasPerms)
        {
            try 
            {
                // just incase someone, magically has a 1 char name........
                if (!(args.length < 2 || args == null))
                {
                    String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : args[1];
                    reason = reason.replace(",", "").trim();
                    OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                    Timestamp bantime = null;

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
                    String banid = BanID.GenerateID(DatabaseUtil.GenID());

                    // Execute queries to get the bans.
                    Future<Boolean> HistorySuccess = DatabaseUtil.InsertHistory(target.getUniqueId().toString(), target.getName(), target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN", reason, sender, banid, bantime);
                    Future<Boolean> BanSuccess = DatabaseUtil.InsertBan(target.getUniqueId().toString(), target.getName(), target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN", reason, sender, banid, bantime);

                    // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String BanID, Timestamp BanTime)
                    if (!BanSuccess.get() || !HistorySuccess.get())
                    {
                        sender.sendMessage(Messages.ServerError);
                        return true;
                    }

                    // Kick the player first, they're officially banned.
                    if (target instanceof Player)
                        User.KickPlayer(sender.getName(), (Player)target, banid, reason, bantime);


                    // Format our messages.
                    String BanAnnouncement = Messages.GetMessages().Translate(silent ? "Ban.SilentBanAnnouncement" : "Ban.BanAnnouncement",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
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

                    String SimplifiedMessage = Messages.GetMessages().Translate(silent ? "Discord.SimplifiedMessageSilent" : "Discord.SimplifiedMessage",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("prefix", Messages.Prefix);
                            put("player", target.getName());
                            put("reason", FuckingJava);
                            put("banner", sender.getName());
                            put("banid", banid);
                            put("fullexpiry", FuckingJava2);
                            put("expiryduration", FuckingJava3);
                            put("dateexpiry", FuckingJava4);
                        }}
                    );

                    // Send to Discord. (New method)
                    if (DiscordUtil.UseSimplifiedMessage == true)
                    {
                        DiscordUtil.SendFormatted(SimplifiedMessage);
                        return true;
                    }
                    else
                    {
                        DiscordUtil.Send(sender.getName().toString(), target.getName(),
                                // if they're the console, use a hard-defined UUID instead of the player's UUID.
                                (sender instanceof ConsoleCommandSender) ? "f78a4d8d-d51b-4b39-98a3-230f2de0c670" : ((Entity) sender).getUniqueId().toString(), 
                                target.getUniqueId().toString(), reason, banid, bantime, silent);
                        return true;
                    }
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
                return true;
            }
        }
        // They're denied perms, just return.
        return true;
    }
}