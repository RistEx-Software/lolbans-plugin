package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BanID;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.sql.*;
import java.util.Arrays;
import java.time.Duration;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javax.lang.model.util.ElementScanner6;


public class MuteCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.mute"))
            return true;
        try 
        {
            // just incase someone, magically has a 1 char name........
            if (!(args.length < 2 || args == null))
            {
                String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length )) : args[1];
                reason = reason.replace(",", "").trim();
                OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                Timestamp mutetime = null;
                String euuid = null;
                System.out.println("!");

                if (sender instanceof ConsoleCommandSender)
                    euuid = "console";
                
                else if (sender instanceof Player)
                    euuid = ((Player) sender).getUniqueId().toString();

                if (target == null)
                    return User.NoSuchPlayer(sender, args[0], true);

                if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                    return User.PlayerOnlyVariableMessage("Mute.CannotMuteSelf", sender, target.getName(), true);

                if (User.IsPlayerMuted(target))
                    return User.PlayerOnlyVariableMessage("Mute.PlayerIsMuted", sender, target.getName(), true);

                // Parse ban time.
                if (!args[1].trim().contentEquals("0") && !args[1].trim().contentEquals("*"))
                {

                    Optional<Long> dur = TimeUtil.Duration(args[1]);
                    if (dur.isPresent())
                    mutetime = new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                    else
                    {
                        sender.sendMessage(Messages.InvalidSyntax);
                        return false;
                    }
                }

                // Prepare our reason
                boolean silent = false;
                if (args.length > 3)
                    silent = args[2].equalsIgnoreCase("-s");

                // Because dumbfuck java and it's "ItS nOt FiNaL"
                final String FuckingJava = new String(reason);
                final String FuckingJava2 = new String(mutetime != null ? String.format("%s (%s)", TimeUtil.TimeString(mutetime), TimeUtil.Expires(mutetime)) : "Never");
                final String FuckingJava3 = new String(mutetime != null ? TimeUtil.Expires(mutetime) : "Never");
                final String FuckingJava4 = new String(mutetime != null ? TimeUtil.TimeString(mutetime) : "Never");

                // Get our ban id based on the latest id in the database.
                String muteid = BanID.GenerateID(DatabaseUtil.GenID());

                // Execute queries to get the bans.
                Future<Boolean> HistorySuccess = DatabaseUtil.InsertMuteHistory(target.getUniqueId().toString(), target.getName(), target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN", reason, sender, euuid, muteid, mutetime);
                Future<Boolean> MuteSuccess = DatabaseUtil.InsertMute(target.getUniqueId().toString(), target.getName(), target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN", reason, sender, euuid, muteid, mutetime);

                // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String BanID, Timestamp BanTime)
                if (!MuteSuccess.get() || !HistorySuccess.get())
                {
                    sender.sendMessage(Messages.ServerError);
                    return true;
                }

                // Kick the player first, they're officially banned.
                //if (target instanceof Player)
                //    User.KickPlayer(sender.getName(), (Player)target, muteid, reason, bantime);
                // TODO: Send mute message?
                String YouWereMuted = Messages.Translate("Mute.YouWereMuted",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("muter", sender.getName());
                        put("muteid", muteid);
                        put("fullexpiry", FuckingJava2);
                        put("expiryduration", FuckingJava3);
                        put("dateexpiry", FuckingJava4);
                    }}
                );

                if (target instanceof Player)
                {
                    Player target2 = (Player) target;
                    target2.sendMessage(YouWereMuted);
                }
                else if (!(target instanceof OfflinePlayer))
                {
                    // You cannot mute console.
                    sender.sendMessage(Messages.GetMessages().GetConfig().getString("Mute.CannotMuteConsole"));
                    return true;
                }

                // Format our messages.
                String MuteAnnouncement = Messages.Translate(silent ? "Mute.SilentMuteAnnouncement" : "Mute.MuteAnnouncement",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("muter", sender.getName());
                        put("muteid", muteid);
                        put("fullexpiry", FuckingJava2);
                        put("expiryduration", FuckingJava3);
                        put("dateexpiry", FuckingJava4);
                    }}
                );

                // Send it to the console.
                Bukkit.getConsoleSender().sendMessage(MuteAnnouncement);
            
                // Send messages to all players (if not silent) or only to admins (if silent)
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp()))
                        continue;

                    p.sendMessage(MuteAnnouncement);
                }

                String SimplifiedMessage = Messages.Translate(silent ? "Discord.SimpMessageSilentMute" : "Discord.SimpMessageMute",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("banner", sender.getName());
                        put("muteid", muteid);
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
                    DiscordUtil.SendDiscord(sender, "muted", target, reason, muteid, mutetime, silent);
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
}