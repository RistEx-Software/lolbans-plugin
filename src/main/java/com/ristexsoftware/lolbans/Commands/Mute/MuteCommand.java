package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.lang.Long;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Map;

public class MuteCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        sender.sendMessage(Messages.InvalidSyntax);
        sender.sendMessage("Usage: /mute [-s] <Player> <Time|*> <Reason>");
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
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

            Punishment punish = new Punishment(PunishmentType.PUNISH_MUTE, sender, target, reason, mutetime);
            punish.Commit(sender);

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("punishid", punish.GetPunishmentID());
                    put("fullexpiry", punish.GetExpiryDateAndDuration());
                    put("expiryduration", punish.GetExpiryDuration());
                    put("dateexpiry", punish.GetExpiryDate());
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
            if (DiscordUtil.UseSimplifiedMessage)
                DiscordUtil.SendFormatted(Messages.Translate(silent ? "Discord.SimpMessageSilentMute" : "Discord.SimpMessageMute", Variables));
            else
                DiscordUtil.SendDiscord(punish, silent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}