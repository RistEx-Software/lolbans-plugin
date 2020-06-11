package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
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

    public BanCommand(Plugin owner)
    {
        super("ban", owner);
        this.setDescription("Ban a player");
        this.setPermission("lolbans.ban");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args) 
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
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.ban"))
            return User.PermissionDenied(sender, "lolbans.ban");
        
        try 
        {
            Timing t = new Timing();

            // /ban [-s] <PlayerName> <Time|*> <Reason>
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("PlayerName", 0);
            a.RequiredString("TimePeriod", 1);
            a.RequiredSentence("Reason", 2);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            String PlayerName = a.get("PlayerName");
            String reason = a.get("Reason");

            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Timestamp bantime = TimeUtil.ParseToTimestamp(a.get("TimePeriod"));

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
                    put("expiry", bantime == null ? "" : punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                    put("appealed", Boolean.toString(punish.GetAppealed()));
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