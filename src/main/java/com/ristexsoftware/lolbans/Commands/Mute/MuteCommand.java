package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Main;
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

        // /mute [-s, -o] <PlayerName> <TimePeriod|*> <Reason>
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.OptionalFlag("Overwrite", "-o");
            a.RequiredString("PlayerName", 0);
            a.RequiredString("TimePeriod", 1);
            a.RequiredSentence("Reason", 2);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            boolean ow = a.get("Overwrite") != null;
            String PlayerName = a.get("PlayerName");
            String reason = a.get("Reason");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Timestamp mutetime = TimeUtil.ParseToTimestamp(a.get("TimePeriod"));

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (mutetime == null && !PermissionUtil.Check(sender, "lolbans.mute.perm"))
            return User.PermissionDenied(sender, "lolbans.mute.perm");
            
            Punishment punish = new Punishment(PunishmentType.PUNISH_MUTE, sender, target, reason, mutetime);
            if(ow && !sender.hasPermission("lolbans.mute.overwrite")) {
                return User.PermissionDenied(sender, "lolbans.mute.overwrite");
            } else if(ow) {
                User.removePunishment(PunishmentType.PUNISH_MUTE, sender, target, "Overwritten by #" + punish.GetPunishmentID(), silent);
            }

            punish.Commit(sender);
            
            if (User.IsPlayerMuted(target) && !ow)
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsMuted", sender, target.getName(), true);
            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("arbiter", sender.getName());
                    put("punishid", punish.GetPunishmentID());
                    put("expiry", mutetime == null ? null : punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                    put("appealed", Boolean.toString(punish.GetAppealed()));
                }};

            if (target.isOnline()) {
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereMuted", Variables));
                User.playSound((Player)target, Main.getPlugin(Main.class).getConfig().getString("MuteSettings.Sound"));
            }

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