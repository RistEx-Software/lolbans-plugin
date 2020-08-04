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

package com.ristexsoftware.lolbans.api;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.lang.Deprecated;

import com.mojang.brigadier.Command;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.utils.ServerType;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import inet.ipaddr.IPAddressString;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;

/**
 * <h2>LolBans Punishment Management Plugin</h2>
 *
 * @author Justin Crawford &amp; Zachery Coleman
 * @version 2.0.0
 * @since 2019-11-13
 */
public class LolBans extends JavaPlugin {
    @Getter private static LolBans plugin;
    @Getter @Setter static private ServerType server;

    /**
     * @deprecated Please use {@link com.ristexsoftware.lolbans.api.LolBans#LolBans(File, File, ServerType)}
     */
    public LolBans(@NotNull File dataFolder, @NotNull File file) {
        super(dataFolder, file);
        plugin = this;
        if (!this.getDataFolder().exists()) {
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            return;
        }

        if (!(new File(this.getDataFolder(), "config.yml").exists())) {
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            return;
        }
    }
    public LolBans(@NotNull File dataFolder, @NotNull File file, ServerType type) {
        this(dataFolder, file);
        setServer(type);
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

    /**
     * Get the USERS hashmap
     * @return The USERS hashmap from {@link com.ristexsoftware.lolbans.api.User}
     */
    public static HashMap<UUID, User> getOnlineUsers() {
        return User.USERS;
    }

    /**
     * Register a new user
     * @param player The player to register as a user
     */
    public static void registerUser(org.bukkit.entity.Player player) {
        getOnlineUsers().put(player.getUniqueId(), new User(player.getName(), player.getUniqueId()));
    }

    /**
     * Register a new user
     * @param player The player to register as a user
     */
    public static void registerUser(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        getOnlineUsers().put(player.getUniqueId(), new User(player.getName(), player.getUniqueId()));
    }

    /**
     * Unregister a user from the server
     * @param player The player to register as a user
     */
    public static void removeUser(org.bukkit.entity.Player player) {
        getOnlineUsers().remove(player.getUniqueId());
    }

    /**
     * Unregister a user from the server
     * @param player The player to register as a user
     */
    public static void removeUser(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        getOnlineUsers().remove(player.getUniqueId());
    }
}
