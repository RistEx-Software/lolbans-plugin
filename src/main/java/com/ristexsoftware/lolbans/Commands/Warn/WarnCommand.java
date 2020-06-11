package com.ristexsoftware.lolbans.Commands.Warn;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
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
    public WarnCommand(Plugin owner)
    {
        super("warn", owner);
        this.setDescription("Issue a warning against a player");
        this.setPermission("lolbans.warn");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
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
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.warn"))
            return User.PermissionDenied(sender, "lolbans.warn");

        // /warn [-s] <PlayerName> <Reason>
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

            Punishment punish = new Punishment(PunishmentType.PUNISH_WARN, sender, target, reason, null);
            punish.Commit(sender);

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("reason", reason);
                put("punishid", punish.GetPunishmentID());
                put("arbiter", sender.getName());
                put("silent", Boolean.toString(silent));
                put("appealed", Boolean.toString(punish.GetAppealed()));
            }};
                
            // If they're online, require acknowledgement immediately by freezing them and sending a message.
            if (target.isOnline())
            {
                String WarnedMessage = Messages.Translate("Warn.WarnedMessage", Variables);
                User.PlaySound((Player)target, Main.getPlugin(Main.class).getConfig().getString("WarningSettings.Sound"));
                if (Main.getPlugin(Main.class).getConfig().getBoolean("WarningSettings.SimpleWarning"))
                    return true;
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