package com.ristexsoftware.lolbans.Commands.Ban;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
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
import com.ristexsoftware.lolbans.Utils.TimeUtil;

import java.util.Optional;
import java.util.TreeMap;

public class UnbanCommand extends RistExCommand
{
    public UnbanCommand(Plugin owner)
    {
        super("unban", owner);
        this.setDescription("Remove a player ban");
        this.setPermission("lolbans.unban");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Unban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.unban"))
            return true;
        
        // Syntax: /unban [-s] <PlayerName|PunishID> <Reason>
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
            
            if (!User.IsPlayerBanned(target))
                return User.PlayerOnlyVariableMessage("Ban.PlayerIsNotBanned", sender, target.getName(), true);

            // Preapre a statement
            // We need to get the latest banid first.
            Optional<Punishment> op = Punishment.FindPunishment(PunishmentType.PUNISH_BAN, target, false);
            if (!op.isPresent())
            {
                sender.sendMessage("Congratulations!! You've found a bug!! Please report it to the lolbans developers to get it fixed! :D");
                return true;
            }

            Punishment punish = op.get();
            punish.SetAppealReason(reason);
            punish.SetAppealed(true);
            punish.SetAppealTime(TimeUtil.TimestampNow());
            punish.SetAppealStaff(sender);
            punish.Commit(sender);

            // Prepare our announce message
            TreeMap<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("arbiter", sender.getName());
                put("punishid", punish.GetPunishmentID());
                put("silent", Boolean.toString(silent));
                put("appealed", Boolean.toString(punish.GetAppealed()));
            }};
            
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Ban.BanAnnouncement", Variables));
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