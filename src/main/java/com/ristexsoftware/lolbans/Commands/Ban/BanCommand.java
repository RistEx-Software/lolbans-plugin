package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.Timing;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.util.TreeMap;
import java.util.Map;

public class BanCommand extends RistExCommandAsync
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args) 
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Ban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.ban"))
            return User.PermissionDenied(sender, "lolbans.ban");

        // /ban [-s] <PlayerName> <Time|*> <Reason>
        if (args.length < 2)
            return false;
        
        try 
        {
            Timing t = new Timing();

            boolean silent = args.length > 3 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = silent ? args[1] : args[0];
            String TimePeriod = silent ? args[2] : args[1];
            String reason = Messages.ConcatenateRest(args, silent ? 3 : 2).trim();
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Timestamp bantime = TimeUtil.ParseToTimestamp(TimePeriod);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsBanned", sender, target.getName(), true);

            if (bantime == null && !PermissionUtil.Check(sender, "lolbans.ban.perm"))
                return User.PermissionDenied(sender, "lolbans.ban.perm"); 
            
            Punishment punish = new Punishment(PunishmentType.PUNISH_BAN, sender, target, reason, bantime);
            punish.Commit(sender);

            // Kick the player first, they're officially banned.
            if (target.isOnline())
                Bukkit.getScheduler().runTaskLater(self, () -> User.KickPlayer(punish) , 1L);
            
            // Format our messages.
            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", punish.GetPlayerName());
                    put("reason", punish.GetReason());
                    put("arbiter", punish.GetExecutionerName());
                    put("punishid", punish.GetPunishmentID());
                    put("expiry", punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                }};

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Ban.BanAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
            t.Finish(sender);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}