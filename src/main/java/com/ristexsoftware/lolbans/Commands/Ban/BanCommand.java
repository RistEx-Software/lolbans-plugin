package com.ristexsoftware.lolbans.Commands.Ban;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.Timing;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

public class BanCommand extends RistExCommandAsync {
    private static Main self = Main.getPlugin(Main.class);

    public BanCommand(Plugin owner) {
        super("ban", owner);
        this.setDescription("Ban a player");
        this.setPermission("lolbans.ban");
        this.setAliases(Arrays.asList(new String[] { "eban","tempban" }));
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args) {
        try {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(
                    Messages.Translate("Syntax.Ban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args) {
        if (!PermissionUtil.Check(sender, "lolbans.ban"))
            return User.PermissionDenied(sender, "lolbans.ban");

        try {
            Timing t = new Timing();

            // /ban [-s, -o] <PlayerName> <Time|*> <Reason>
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.OptionalFlag("Overwrite", "-o");
            a.RequiredString("PlayerName", 0);
            a.OptionalString("TimePeriod", 1);
            a.RequiredSentence("Reason", a.get("TimePeriod")==null?0:1);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            boolean ow = a.get("Overwrite") != null;
            String PlayerName = a.get("PlayerName");
            Timestamp punishtime = TimeUtil.ParseToTimestamp(a.get("TimePeriod"));
            String reason = punishtime == null ? a.get("TimePeriod")+" "+ a.get("Reason") : a.get("Reason");

            OfflinePlayer target = User.FindPlayerByAny(PlayerName);
            Punishment punish = new Punishment(PunishmentType.PUNISH_BAN, sender, target, reason, punishtime, silent);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (ow && !sender.hasPermission("lolbans.ban.overwrite"))
                return User.PermissionDenied(sender, "lolbans.ban.overwrite");
            else if (ow) {
                User.removePunishment(PunishmentType.PUNISH_BAN, sender, target,
                        "Overwritten by #" + punish.GetPunishmentID(), silent);
            }

            if (User.IsPlayerBanned(target) && !ow)
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsBanned", sender, target.getName(), true);

            if (punishtime == null && !PermissionUtil.Check(sender, "lolbans.ban.perm"))
                return User.PermissionDenied(sender, "lolbans.ban.perm");

            if (punishtime != null
                    && TimeUtil.ParseToTimestamp(a.get("TimePeriod")).getTime() > User.getTimeGroup(sender).getTime())
                return User.PermissionDenied(sender, "lolbans.maxtime." + a.get("TimePeriod"));

            punish.Commit(sender);

            // Kick the player first, they're officially banned.
            if (target.isOnline())
                Bukkit.getScheduler().runTaskLater(self, () -> User.KickPlayer(punish), 1L);

            // Format our messages.
            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", punish.GetPlayerName());
                    put("reason", punish.GetReason());
                    put("arbiter", punish.GetExecutionerName());
                    put("punishid", punish.GetPunishmentID());
                    put("expiry", punishtime == null ? "" : punish.GetExpiryString());
                    put("silent", Boolean.toString(silent));
                    put("appealed", Boolean.toString(punish.GetAppealed()));
                    put("expires", Boolean.toString(punishtime != null && !punish.GetAppealed()));
                }
            };

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Ban.BanAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
            t.Finish(sender);
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}