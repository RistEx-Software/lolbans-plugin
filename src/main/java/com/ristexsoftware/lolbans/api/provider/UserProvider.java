package com.ristexsoftware.lolbans.api.provider;

import java.net.InetSocketAddress;

import com.ristexsoftware.lolbans.api.User;

/**
 * Provides an interface between the User class and the current server type.
 */
public interface UserProvider {
    /**
     * Send a message to the specified user with the given content.
     */
    public void sendMessage(User user, String content);

    /**
     * Disconnect a user for a given reason.
     */
    public void disconnect(User user, String reason);

    /**
     * Get the IP address of the specified user.
     */
    public InetSocketAddress getAddress(User user);

    /**
     * Test whether the specified user has the given permission node.
     */
    public boolean hasPermission(User user, String permissionNode);
}