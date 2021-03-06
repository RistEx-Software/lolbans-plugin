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