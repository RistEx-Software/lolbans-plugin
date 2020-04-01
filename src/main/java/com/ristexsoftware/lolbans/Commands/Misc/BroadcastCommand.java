package com.ristexsoftware.lolbans.Commands.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

// TODO: Support cross-server broadcasts?
// TODO: Prefix broadcasts?
public class BroadcastCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.broadcast"))
            return User.PermissionDenied(sender, "lolbans.broadcast");

        // Syntax is really just "/broadcast This is the broadcasted message here"
        String message = Messages.ConcatenateRest(args, 1);

        // Announce it to the 
        for (Player pl : Bukkit.getOnlinePlayers())
            pl.sendMessage(TranslationUtil.TranslateColors("&", message));
        
        return true;
    }
}