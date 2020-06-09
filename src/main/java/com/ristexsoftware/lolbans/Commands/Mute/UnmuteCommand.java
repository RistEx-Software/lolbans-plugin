package com.ristexsoftware.lolbans.Commands.Mute;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.util.Optional;
import java.util.TreeMap;

public class UnmuteCommand extends RistExCommand
{
    public UnmuteCommand(Plugin owner)
    {
        super("unmute", owner);
        this.setDescription("Allow the player to send chat messages");
        this.setPermission("lolbans.unmute");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Unmute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.unmute"))
            return User.PermissionDenied(sender, "lolbans.unmute");

        // /unmute [-s] <PlayerName> <Reason>
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("PlayerName", 0);
            a.RequiredSentence("Reason", 1);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            String PlayerName = a.get("PlayerName");
            String reason = a.get("Reason");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);
            
            if (!User.IsPlayerMuted(target))
                return User.PlayerOnlyVariableMessage("Mute.PlayerIsNotMuted", sender, target.getName(), true);

            Optional<Punishment> op = Punishment.FindPunishment(PunishmentType.PUNISH_MUTE, target, false);
            if (!op.isPresent())
            {
                sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
            }

            Punishment punish = op.get();
            punish.SetAppealReason(reason);
            punish.SetAppealed(true);
            punish.SetAppealStaff(sender);
            punish.Commit(sender);

            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("arbiter", sender.getName());
                put("punishid", punish.GetPunishmentID());
                put("silent", Boolean.toString(silent));
                put("appealed", Boolean.toString(punish.GetAppealed()));
            }};

            if (target.isOnline())
                ((Player)target).sendMessage(Messages.Translate("Mute.YouWereUnMuted", Variables));

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Mute.MuteAnnouncement", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}