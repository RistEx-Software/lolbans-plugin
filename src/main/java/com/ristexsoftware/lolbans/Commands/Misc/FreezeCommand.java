package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

import java.util.TreeMap;
import java.util.Map;


public class FreezeCommand extends RistExCommand
{
    private static Main self = Main.getPlugin(Main.class);

    @Override
    public void onSyntaxError(CommandSender sender, Command command, String label, String[] args)
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
    public boolean Execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.freeze"))
            return User.PermissionDenied(sender, "lolbans.freeze");

        // /freeze [-s] <PlayerName>
        if (args.length < 2)
            return false;
        
        try 
        {
            boolean silent = args.length > 1 ? args[0].equalsIgnoreCase("-s") : false;
            String PlayerName = args[silent ? 1 : 0];
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

            String FrozenMessage = Messages.Translate(u.IsFrozen() ? "Freeze.FrozenMessage" : "Freeze.UnFrozenMessage", Variables);
            String FrozenAnnouncement = Messages.Translate(u.IsFrozen() ? "Freeze.FreezeAnnouncement" : "Freeze.UnfreezeAnnouncement", Variables);
            
            u.SendMessage(FrozenMessage);

            // Send them a box as well. This will disallow them from sending move events.
            // However, client-side enforcement is not guaranteed so we also enforce the
            // same thing using the MovementListener, this just helps stop rubberbanding.
            u.SpawnBox(true, null);

            self.getLogger().info(FrozenAnnouncement);

            // Send the message to all online players.
            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (silent && (!p.hasPermission("lolbans.alerts") && !p.isOp() && p == target))
                    p.sendMessage(FrozenAnnouncement);
            }
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}