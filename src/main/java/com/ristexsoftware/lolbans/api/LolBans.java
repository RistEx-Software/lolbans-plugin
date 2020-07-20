/* 
 *  LolBans - The advanced banning system for Minecraft
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

package com.ristexsoftware.lolbans.api;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ristexsoftware.lolbans.api.utils.logger.PluginLogger;

import inet.ipaddr.IPAddressString;

public class LolBans extends PluginLogger {
    public static HashMap<Integer, Pattern> REGEX = new HashMap<Integer, Pattern>();
    public static List<IPAddressString> BannedAddresses = new Vector<IPAddressString>();

    public static ExecutorService pool = Executors.newFixedThreadPool(3);

    /**
     * Get an offline user
     * 
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public static User getOfflineUser(UUID uuid) {
        return User.resolveUser(uuid.toString());
    }

    /**
     * Get an offline user
     * 
     * @deprecated Please use {@link #getOfflineUser(UUID)} as usernames are not unique past a single session
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public static User getOfflineUser(String username) {
        return User.resolveUser(username);
    }

    /**
     * Get a currently online user by username
     * 
     * @deprecated Please use {@link #getUser(UUID)} instead
     * @param username The username of the user to get
     * @return null if user is not online
     */
    public static User getUser(String username) {
        User user = User.resolveUser(username);
        if (user.isOnline())
            return user;
        return null;
    }

    /**
     * Get a currently online user by uuid
     * 
     * @param uuid The uuid of the user to get
     * @return null if user is not online
     */
    public static User getUser(UUID uuid) {
        if (User.USERS.containsKey(uuid))
            return User.USERS.get(uuid);
        return null;
    }

    /**
     * Convenience function to get LolBans logger
     * @return LolBans plugin logger
     */
    public static Logger getLogger() {
        return getLogger("LolBans");
    }
}