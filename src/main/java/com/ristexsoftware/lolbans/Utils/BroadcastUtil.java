package com.ristexsoftware.lolbans.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastUtil
{

    public static void Broadcast(String Message, Object... args)
    {
        Bukkit.broadcastMessage(String.format(Message, args));
    }

    public static void BroadcastOps(String Message, Object... args)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (!p.hasPermission("lolbans.alerts") && !p.isOp())
                continue;

            p.sendMessage(String.format(Message, args));
        }
    }

}