/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import net.md_5.bungee.api.chat.TextComponent;

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
        // We want to run this task synchronously with the main thread, or bukkit
        // complains.
        if (player != null)
            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(Main.class), () -> player.kickPlayer(reason),
                    0L);
    }

    /**
     * Get the IP address of the specified user.
     */
    public IPAddress getAddress(User user) {
        Player player = getPlayer(user);
        try {
            if (player != null)
                return new IPAddressString(player.getAddress().getAddress().getHostAddress()).toAddress();
        } catch (Exception e) {
            return null;
        }
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

    @Override
    public void sendMessage(User user, TextComponent content) {
        Player player = getPlayer(user);
        player.spigot().sendMessage(content);

    }
}