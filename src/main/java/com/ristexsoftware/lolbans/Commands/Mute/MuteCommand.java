package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
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

public class MuteCommand implements CommandExecutor
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.mute"))
            return User.PermissionDenied(sender, "lolbans.mute");

        // /mute [-s] <PlayerName> <TimePeriod|*> <Reason>
        if (args.length < 3)
            return false;
        
        try 
        {
            boolean silent = args.length > 3 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String TimePeriod = args[silent ? 2 : 1];
            String reason = Messages.ConcatenateRest(args, silent ? 3 : 2).trim();
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Timestamp mutetime = null;

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (!(sender instanceof ConsoleCommandSender) && target.getUniqueId().equals(((Player) sender).getUniqueId()))
                return User.PlayerOnlyVariableMessage("Mute.CannotMuteSelf", sender, target.getName(), true);

            if (User.IsPlayerMuted(target))
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsMuted", sender, target.getName(), true);

            // Parse ban time.
            if (!Messages.CompareMany(TimePeriod, new String[]{"*", "0"}))
            {
                Optional<Long> dur = TimeUtil.Duration(TimePeriod);
                if (dur.isPresent())
                    mutetime = new Timestamp((TimeUtil.GetUnixTime() + dur.get()) * 1000L);
                else
                    return false;
            }

            if (mutetime == null && !PermissionUtil.Check(sender, "lolbans.mute.perm"))
                return User.PermissionDenied(sender, "lolbans.mute.perm"); 

            // Because dumbfuck java and it's "ItS nOt FiNaL"
            final String FuckingJava2 = new String(mutetime != null ? String.format("%s (%s)", TimeUtil.TimeString(mutetime), TimeUtil.Expires(mutetime)) : "Never");
            final String FuckingJava3 = new String(mutetime != null ? TimeUtil.Expires(mutetime) : "Never");
            final String FuckingJava4 = new String(mutetime != null ? TimeUtil.TimeString(mutetime) : "Never");

            // Get our ban id based on the latest id in the database.
            String muteid = PunishID.GenerateID(DatabaseUtil.GenID("Punishments"));

            // Execute queries to get the bans.
            Future<Boolean> MuteSuccess = DatabaseUtil.InsertPunishment(PunishmentType.PUNISH_MUTE, target, sender, reason, muteid, mutetime);

            // InsertBan(String UUID, String PlayerName, String Reason, String Executioner, String PunishID, Timestamp BanTime)
            if (!MuteSuccess.get())
            {
                sender.sendMessage(Messages.ServerError);
                return true;
            }

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("muter", sender.getName());
                    put("punishid", muteid);
                    put("fullexpiry", FuckingJava2);
                    put("expiryduration", FuckingJava3);
                    put("dateexpiry", FuckingJava4);
                }};

            if (target.isOnline())
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereMuted", Variables));

            // Format our messages.
            String MuteAnnouncement = Messages.Translate(silent ? "Mute.SilentMuteAnnouncement" : "Mute.MuteAnnouncement", Variables);

            // Send it to the console.
            self.getLogger().info(MuteAnnouncement);
        
            // Send messages to all players (if not silent) or only to admins (if silent)
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!silent && (p.hasPermission("lolbans.alerts") || p.isOp()))
                    p.sendMessage(MuteAnnouncement);
            }

            // Send to Discord. (New method)
            if (DiscordUtil.UseSimplifiedMessage == true)
            {
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentMute" : "Discord.SimpMessageMute",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", target.getName());
                        put("reason", reason);
                        put("banner", sender.getName());
                        put("punishid", muteid);
                    }}
                ));
            }
            else
                DiscordUtil.SendDiscord(sender, "muted", target, reason, muteid, mutetime, silent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}