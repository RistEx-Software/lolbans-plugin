package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.ristexsoftware.lolbans.Objects.User;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.Objects.RistExCommand;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

// TODO: Support cross-server broadcasts?
// TODO: Prefix broadcasts?
public class BroadcastCommand extends RistExCommand
{
    public BroadcastCommand(Plugin owner)
    {
        super("broadcast", owner);
        this.setDescription("Broadcast a message to all online players");
        this.setPermission("lolbans.broadcast");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.Broadcast", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!PermissionUtil.Check(sender, "lolbans.broadcast"))
            return User.PermissionDenied(sender, "lolbans.broadcast");

        // Syntax is really just "/broadcast This is the broadcasted message here"
        String message = Messages.ConcatenateRest(args, 1);

        if (message == null || message.isEmpty())
            return false;

        // Announce it to the 
        for (Player pl : Bukkit.getOnlinePlayers())
            pl.sendMessage(TranslationUtil.TranslateColors("&", message));
        
        return true;
    }
}