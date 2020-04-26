package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.util.TreeMap;
import java.util.Map;

public class MuteCommand extends RistExCommand
{
    public MuteCommand(Plugin owner)
    {
        super("mute", owner);
        this.setDescription("Prevent a player from sending messages in chat");
        this.setPermission("lolbans.mute");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Mute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args)
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
            Timestamp mutetime = TimeUtil.ParseToTimestamp(TimePeriod);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (User.IsPlayerMuted(target))
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsMuted", sender, target.getName(), true);

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
                    put("expiry", punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                }};

            if (target.isOnline())
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereMuted", Variables));

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Mute.MuteAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}