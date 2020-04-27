package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.util.TreeMap;
import java.util.Map;


public class FreezeCommand extends RistExCommand
{
    public FreezeCommand(Plugin owner)
    {
        super("freeze", owner);
        this.setDescription("Prevent a player from doing any actions");
        this.setPermission("lolbans.freeze");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Freeze", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.freeze"))
            return User.PermissionDenied(sender, "lolbans.freeze");

        // /freeze [-s] <PlayerName>
        try 
        {
            ArgumentUtil a = new ArgumentUtil(args);
            a.OptionalFlag("Silent", "-s");
            a.RequiredString("PlayerName", 0);

            if (!a.IsValid())
                return false;

            boolean silent = a.get("Silent") != null;
            String PlayerName = a.get("PlayerName");
            OfflinePlayer target = User.FindPlayerByAny(PlayerName);

            if (target == null)
                return User.NoSuchPlayer(sender, PlayerName, true);

            if (!target.isOnline())
                return User.PlayerOnlyVariableMessage("Freeze.PlayerOffline", sender, PlayerName, true);

            User u = Main.USERS.get(target.getUniqueId());

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
            {{
                put("player", target.getName());
                put("arbiter", sender.getName());
                put("silent", Boolean.toString(silent));
            }};
            
            u.SetFrozen(!u.IsFrozen());

            u.SendMessage(Messages.Translate(u.IsFrozen() ? "Freeze.FrozenMessage" : "Freeze.UnFrozenMessage", Variables));

            // Send them a box as well. This will disallow them from sending move events.
            // However, client-side enforcement is not guaranteed so we also enforce the
            // same thing using the MovementListener, this just helps stop rubberbanding.
            u.SpawnBox(true, null);

            BroadcastUtil.BroadcastEvent(silent, Messages.Translate(u.IsFrozen() ? "Freeze.FreezeAnnouncement" : "Freeze.UnfreezeAnnouncement", Variables));
            // TODO: Discord
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}