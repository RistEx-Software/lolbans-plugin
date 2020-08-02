/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  Copyright (C) 2019-2020 Skye Elliot <actuallyori@gmail.com>
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import inet.ipaddr.IPAddressString;

import lombok.Getter;

/**
 * <h2>LolBans Punishment Management Plugin</h2>
 *
 * @author Justin Crawford &amp; Zachery Coleman
 * @version 2.0.0
 * @since 2019-11-13
 */
public class LolBans extends JavaPlugin {
    @Getter private static LolBans plugin;

    public LolBans(@NotNull File dataFolder, @NotNull File file) {
        super(dataFolder, file);
        plugin = this;
    }

    public static HashMap<Integer, Pattern> REGEX = new HashMap<Integer, Pattern>();
    public static List<IPAddressString> BANNED_ADDRESSES = new Vector<IPAddressString>();

    public static ExecutorService pool = Executors.newFixedThreadPool(3);

    /**
     * Get a user
     * 
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public static User getUser(UUID uuid) {
        // If they are in the USERS HashMap, save some time and just return that
        if (User.USERS.containsKey(uuid))
            return User.USERS.get(uuid);
        return User.resolveUser(uuid.toString());
    }

    /**
     * Get a user
     * 
     * @deprecated Please use {@link #getUser(UUID)} as usernames are not unique past a single session
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public static User getUser(String username) {
        return User.resolveUser(username);
    }
}
