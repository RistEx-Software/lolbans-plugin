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
import java.util.Map;
import java.util.concurrent.Future;

public class BanCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.ban"))
            return User.PermissionDenied(sender, "lolbans.ban");

        // /ban [-s] <PlayerName> <Time|*> <Reason>
        if (args.length < 2)
            return false;
        
        try 
        {
            // debugging
            Long now = System.currentTimeMillis();

            boolean silent = args.length > 3 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = silent ? args[1] : args[0];
            String TimePeriod = silent ? args[2] : args[1];
            String reason = Messages.ConcatenateRest(args, silent ? 3 : 2).trim();
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                return User.PlayerOnlyVariableMessage("Ban.CannotBanSelf", sender, target.getName(), true);  

            if (User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsBanned", sender, target.getName(), true);
            

            Timestamp bantime = null;

            // Parse ban time.
            if (!Messages.CompareMany(TimePeriod, new String[]{"*", "0"}))
            {
                Optional<Long> dur = TimeUtil.Duration(TimePeriod);
                if (dur.isPresent())
                    bantime = new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                else
                    return false;
            }

            if (bantime == null && !PermissionUtil.Check(sender, "lolbans.ban.perm"))
                return User.PermissionDenied(sender, "lolbans.ban.perm"); 
            
            // Because dumbfuck java and it's "ItS nOt FiNaL"
            // but really? what the fuck java? Now I have to have all of these "fuckingjava" strings.. thanks.......
            final String FuckingJava2 = new String(bantime != null ? String.format("%s (%s)", TimeUtil.TimeString(bantime), TimeUtil.Expires(bantime)) : "Never");
            final String FuckingJava3 = new String(bantime != null ? TimeUtil.Expires(bantime) : "Never");
            final String FuckingJava4 = new String(bantime != null ? TimeUtil.TimeString(bantime) : "Never");
            

            // Get our ban id based on the latest id in the database.
            String banid = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));

            // Execute queries to get the bans.
            Future<Boolean> BanSuccess = DatabaseUtil.InsertPunishment(PunishmentType.PUNISH_BAN, target, sender, reason, banid, bantime);
            
            // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String PunishID, Timestamp BanTime)
            if (!BanSuccess.get())
            {
                sender.sendMessage(Messages.ServerError);
                return true;
            } 

            // Kick the player first, they're officially banned.
            if (target.isOnline())
                User.KickPlayer(sender.getName(), (Player)target, banid, reason, bantime);
            
            // Format our messages.
            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("banner", sender.getName());
                    put("punishid", banid);
                    put("fullexpiry", FuckingJava2);
                    put("expiryduration", FuckingJava3);
                    put("dateexpiry", FuckingJava4);
                }};

            String BanAnnouncement = Messages.Translate(silent ? "Ban.SilentBanAnnouncement" : "Ban.BanAnnouncement", Variables);

            // Send it to the console.
            self.getLogger().info(BanAnnouncement);
        
            // Send messages to all players (if not silent) or only to admins (if silent)
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!silent && (p.hasPermission("lolbans.alerts") || p.isOp()))
                    p.sendMessage(BanAnnouncement);
            }

            // debugging
            Long later = System.currentTimeMillis();
            Long thingy = later - now;
            sender.sendMessage(ChatColor.GRAY + "Done! " + ChatColor.RED + thingy + "ms");

            // Send to Discord. (New method)
            if (DiscordUtil.UseSimplifiedMessage == true)
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentBan" : "Discord.SimpMessageBan", Variables));
            else
                DiscordUtil.SendDiscord(sender, "banned", target, reason, banid, bantime, silent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}