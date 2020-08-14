package com.ristexsoftware.lolbans.bungeecord.provider;

import com.ristexsoftware.lolbans.api.provider.UserProvider;
import com.ristexsoftware.lolbans.api.User;

import java.net.InetSocketAddress;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;

/**
 * Resolves users for Bungee.
 */
public class BungeeUserProvider implements UserProvider {

    /**
     * Convert a user object to a player.
     */
    private ProxiedPlayer getProxiedPlayer(User user) {
        for (ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getPlayers()) {
            if (proxiedPlayer.getUniqueId().equals(user.getUniqueId()))
                return proxiedPlayer;
        }
        return null;
    }
    
    /**
     * Send a message to the specified user with the given content.
     */
    public void sendMessage(User user, String content) {
        ProxiedPlayer proxiedPlayer = getProxiedPlayer(user);
        if (proxiedPlayer != null) 
            proxiedPlayer.sendMessage(content);
    }

    /**
     * Disconnect a user for a given reason.
     */
    public void disconnect(User user, String reason) {
        ProxiedPlayer proxiedPlayer = getProxiedPlayer(user);
        if (proxiedPlayer != null)
            proxiedPlayer.disconnect(reason);
    }

    /**
     * Get the IP address of the specified user.
     */
    public InetSocketAddress getAddress(User user) {
        ProxiedPlayer proxiedPlayer = getProxiedPlayer(user);
        if (proxiedPlayer != null)
            return proxiedPlayer.getAddress();
        return null;
    }

    /**
     * Test whether the specified user has the given permission node.
     */
    public boolean hasPermission(User user, String permissionNode) {
        ProxiedPlayer proxiedPlayer = getProxiedPlayer(user);
        if (proxiedPlayer != null)
            return proxiedPlayer.hasPermission(permissionNode);
        return false;
    }
}