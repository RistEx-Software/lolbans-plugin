package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.ristexsoftware.lolbans.Main;


public class PermissionUtil
{
    private static Main self = Main.getPlugin(Main.class);

    public static boolean Check(CommandSender sender, String Perm)
    {
        // Console ALWAYS has full perms
        if (sender instanceof ConsoleCommandSender)
            return true;

        if (sender instanceof Player)
        {
            Player p = (Player)sender;
            
            // If configured to allow ops to bypass all permission checks
            if (self.getConfig().getBoolean("General.OpsBypassPermissions") && p.isOp())
                return true;

            // Otherwise check if they actually have the permission
            return p.hasPermission(Perm);
        }
        else
            return false; // Something that isnt a player or a command sender
    }
}