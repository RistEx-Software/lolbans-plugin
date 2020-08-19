package com.ristexsoftware.lolbans.api.provider;

import java.net.InetSocketAddress;

import com.ristexsoftware.lolbans.api.User;

import inet.ipaddr.IPAddress;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Provides an interface between the User class and the current server type.
 */
public interface UserProvider {
    /**
     * Send a message to the specified user with the given content.
     */
    public void sendMessage(User user, String content);

    /**
     * Send a message to the specified user with the given content.
     */
    public void sendMessage(User user, TextComponent content);

    /**
     * Disconnect a user for a given reason.
     */
    public void disconnect(User user, String reason);

    /**
     * Get the IP address of the specified user.
     */
    public IPAddress getAddress(User user);

    /**
     * Test whether the specified user has the given permission node.
     */
    public boolean hasPermission(User user, String permissionNode);
}