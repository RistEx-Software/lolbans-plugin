package com.ristexsoftware.lolbans.bukkit.provider;

import com.ristexsoftware.lolbans.api.provider.UserProvider;
import com.ristexsoftware.lolbans.bukkit.Main;
import com.ristexsoftware.lolbans.api.User;

import java.net.InetSocketAddress;

import com.ristexsoftware.knappy.util.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

/**
 * Resolves users for Bukkit.
 */
public class BukkitUserProvider implements UserProvider {
    private Debugger debug = new Debugger(getClass());

    /**
     * Convert a user object to a player.
     */
    private Player getPlayer(User user) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(user.getUniqueId())) 
                return player;
        }
        return null;
    }
    
    /**
     * Send a message to the specified user with the given content.
     */
    public void sendMessage(User user, String content) {
        Player player = getPlayer(user);
        if (player != null) 
            player.sendMessage(content);
    }

    /**
     * Disconnect a user for a given reason.
     */
    public void disconnect(User user, String reason) {
        Player player = getPlayer(user);
        // We want to run this task synchronously with the main thread, or bukkit complains.
        if (player != null)
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(Main.class), () -> player.kickPlayer(reason), 0L);
    }

    /**
     * Get the IP address of the specified user.
     */
    public InetSocketAddress getAddress(User user) {
        Player player = getPlayer(user);
        if (player != null)
            return player.getAddress();
        return null;
    }

    /**
     * Test whether the specified user has the given permission node.
     */
    public boolean hasPermission(User user, String permissionNode) {
        Player player = getPlayer(user);
        if (player != null)
            return player.hasPermission(permissionNode);
        return false;
    }
}