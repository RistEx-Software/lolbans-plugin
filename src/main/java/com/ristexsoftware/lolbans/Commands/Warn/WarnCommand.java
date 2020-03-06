package com.ristexsoftware.lolbans.Commands.Warn;

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
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
        if (!PermissionUtil.Check(sender, "lolbans.warn"))
            return true;

        try 
        {
            // just incase someone, magically has a 1 char name........
            if (!(args.length < 1 || args == null))
            {
                // FIXME: "-s" isn't parsed out of the message.
                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : args[1];
                reason = reason.replace(",", "").trim();
                OfflinePlayer target = User.FindPlayerByBanID(args[0]);
                String euuid = null;

                if (sender instanceof ConsoleCommandSender)
                    euuid = "console";

                else if (sender instanceof Player)
                    euuid = ((Player) sender).getUniqueId().toString();

                if (target == null)
                    return User.NoSuchPlayer(sender, args[0], true);

                // Prepare our reason
                boolean silent = args.length > 2 ? args[1].equalsIgnoreCase("-s") : false;
                final String FuckingJava = new String(reason);
                int i = 1;

                // Get the latest ID of the banned players to generate a BanID form it.
                String warnid = BanID.GenerateID(DatabaseUtil.GenID());

                /*
                // Preapre a statement
                PreparedStatement pst = self.connection.prepareStatement("INSERT INTO Warnings (UUID, PlayerName, IPAddress, Reason, Executioner, WarnID, Accepted) VALUES (?, ?, ?, ?, ?, ?, ?)");
                pst.setString(i++, target.getUniqueId().toString());
                pst.setString(i++, target.getName());
                if (target.isOnline())
                    pst.setString(i++, ((Player)target).getAddress().getAddress().getHostAddress());
                else
                    pst.setString(i++, "UNKNOWN");
                pst.setString(i++, reason);
                pst.setString(i++, sender.getName());
                pst.setString(i++, warnid);
                pst.setBoolean(i++, false);

                // Commit to the database.
                pst.executeUpdate();
                */
                // InsertWarn
                Future<Boolean> InsertWarn = DatabaseUtil.InsertWarn(target.getUniqueId().toString(), target.getName(), target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress() : "UNKNOWN", reason, sender, euuid, warnid);

                // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String BanID, Timestamp BanTime)
                if (!InsertWarn.get())
                {
                    sender.sendMessage(Messages.ServerError);
                    return true;
                }

                Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", FuckingJava);
                    put("warnid", warnid);
                    put("warner", sender.getName());
                }};
                    
                //String WarnedMessage = Messages.Translate("Warn.WarnedMessage", Variables);
                //String WarnAnnouncement = Messages.Translate(silent ? "Warn.SilentWarnAnnouncment" : "Warn.WarnAnnouncment", Variables);

                String WarnedMessage = Messages.Translate("Warn.WarnedMessage",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("warnid", warnid);
                        put("warner", sender.getName());
                    }}
                );

                String WarnAnnouncement = Messages.Translate("Warn.WarnAnnouncment",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("warnid", warnid);
                        put("warner", sender.getName());
                    }}
                );

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

                String SimplifiedMessage = Messages.Translate(silent ? "Discord.SimpMessageSilentWarn" : "Discord.SimpMessageWarn",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", FuckingJava);
                        put("banner", sender.getName());
                        put("warnid", warnid);
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
                    // Send to Discord. (New method)
                    DiscordUtil.SendDiscord(sender, "warned", target, reason, warnid, silent);
                }


                return true;

            }
            else
            {
                sender.sendMessage(Messages.InvalidSyntax);
                return false; // Show syntax.
            }
        }
        catch (SQLException | InvalidConfigurationException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
            return true;
        }
    }
}