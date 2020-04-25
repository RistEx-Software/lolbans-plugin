package com.ristexsoftware.lolbans.Commands.Warn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

import java.sql.*;
import java.util.TreeMap;
import java.util.Map;


public class WarnCommand extends RistExCommand
{
    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Warn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.warn"))
            return User.PermissionDenied(sender, "lolbans.warn");

        // /warn [-s] <PlayerName> <Reason>
        if (args.length < 2)
            return false;

        try 
        {
            boolean silent = args.length > 2 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
            String reason = Messages.ConcatenateRest(args, silent ? 2 : 1).trim();
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            Punishment punish = new Punishment(PunishmentType.PUNISH_WARN, sender, target, reason, null);
            punish.Commit(sender);

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("punishid", punish.GetPunishmentID());
                put("arbiter", sender.getName());
                put("silent", Boolean.toString(silent));
            }};
                
            // If they're online, require acknowledgement immediately by freezing them and sending a message.
            if (target.isOnline())
            {
                String WarnedMessage = Messages.Translate("Warn.WarnedMessage", Variables);
                User u = Main.USERS.get(target.getUniqueId());
                u.SetWarned(true, ((Player) target).getLocation(), WarnedMessage);
                u.SendMessage(WarnedMessage);

                // Send them a box as well. This will disallow them from sending move events.
                // However, client-side enforcement is not guaranteed so we also enforce the
                // same thing using the MovementListener, this just helps stop rubberbanding.
                u.SpawnBox(true, null);
            }
            
            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Warn.WarnAnnouncment", Variables));
            DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        }
        catch (SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }

        return true;
    }
}