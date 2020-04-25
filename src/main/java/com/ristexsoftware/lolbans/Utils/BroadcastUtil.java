package com.ristexsoftware.lolbans.Utils;

import com.ristexsoftware.lolbans.Main;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastUtil
{
    private static Main self = Main.getPlugin(Main.class);
    public static void Broadcast(String Message, Object... args)
    {
        Bukkit.broadcastMessage(String.format(Message, args));
    }

    public static void BroadcastOps(String Message, Object... args)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (p.hasPermission("lolbans.alerts"))
                p.sendMessage(String.format(Message, args));
        }
    }

    public static void BroadcastEvent(boolean silent, String Announcement)
    {
        BroadcastUtil.BroadcastEvent(silent, Announcement, "lolbans.alerts");
    }

    public static void BroadcastEvent(boolean silent, String Announcement, String Permission)
    {
        self.getLogger().info(Announcement);
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (silent && !p.hasPermission(Permission))
                continue;
            
            p.sendMessage(Announcement);
        }
    }
}