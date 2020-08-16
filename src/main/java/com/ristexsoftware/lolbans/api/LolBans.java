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

import java.io.FileNotFoundException;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.UUID;
import java.util.Vector;

import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.runnables.CacheRunnable;
import com.ristexsoftware.lolbans.api.runnables.QueryRunnable;
import com.ristexsoftware.lolbans.api.utils.Cache;
import com.ristexsoftware.lolbans.api.utils.Discord;
import com.ristexsoftware.lolbans.api.provider.ConfigProvider;
import com.ristexsoftware.lolbans.api.provider.UserProvider;
import com.ristexsoftware.knappy.configuration.file.FileConfiguration;
import com.ristexsoftware.knappy.util.Version.ServerType;
import com.ristexsoftware.knappy.translation.LocaleProvider;

import inet.ipaddr.IPAddressString;
import lombok.Getter;
import lombok.Setter;

/**
 * <h2>LolBans Punishment Management Plugin</h2>
 *
 * @author Justin Crawford &amp; Zachery Coleman
 * @version 2.0.0
 * @since 2019-11-13
 */
public class LolBans {
    @Getter
    private static LolBans plugin;
    
    @Getter
    @Setter
    private static ServerType serverType;

    public HashMap<Integer, Pattern> REGEX = new HashMap<Integer, Pattern>();

    @Deprecated
    public List<IPAddressString> BANNED_ADDRESSES = new Vector<IPAddressString>(); // Not sure why this is here, legacy? 

    @Getter private ExecutorService pool = Executors.newFixedThreadPool(3);

    /**
     * User provider - interfaces with the current Minecraft server to retrieve user data.
     */
    @Getter
    @Setter
    private UserProvider userProvider = null;

    /**
     * Plugin's config provider - used for storing/retrieval of persistent data.
     */
    @Getter
    @Setter
    private ConfigProvider configProvider = null;

    @Getter
    @Setter
    private static Logger logger = Logger.getLogger("LolBans"); 

    @Setter
    @Getter
    private boolean chatMute = false;

    @Getter
    private Discord discord = null;

    @Getter
    private FileConfiguration config;

    @Getter
    private LocaleProvider localeProvider;

    @Getter
    private boolean enabled = false;

    @Getter
    @Setter
    private boolean chatEnabled = true;

    @Getter
    @Setter
    private MaintenanceLevel maintenanceLevel = MaintenanceLevel.HIGH;

    @Getter
    @Setter
    private Boolean maintenanceModeEnabled = false;

    // Caches
    @Getter private Cache<User> userCache = new Cache<>(User.class);
    @Getter private Cache<User> onlineUserCache = new Cache<>(User.class);
    @Getter private Cache<Punishment> punishmentCache = new Cache<>(Punishment.class);

    public LolBans(ConfigProvider configProvider, UserProvider userProvider, ServerType type) throws FileNotFoundException {
        // super(configProvider.getDataFolder(), configProvider.getConfigFile())

        // Set these early on bc they're important (like you~)
        plugin = this;
        this.configProvider = configProvider;
        this.userProvider = userProvider;
        localeProvider = new LocaleProvider(new File(configProvider.getDataFolder(), "locale"));
        if (!configProvider.dataFolderExists()) {
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            getConfigProvider().getDataFolder().mkdirs();
            getConfigProvider().saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            throw new FileNotFoundException("Please configure lolbans and restart the server! :)");
        }
        
        if (!configProvider.configExists()) {
            configProvider.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            throw new FileNotFoundException("Please configure lolbans and restart the server! :)");
        }
        this.config = configProvider.getConfig();

        
        LolBans.serverType = type;
        this.maintenanceLevel = MaintenanceLevel.fromOrdinal(getConfig().getInt("general.maintenance"));
        
        if (this.config.getBoolean("discord.enabled"))
            this.discord = new Discord(this.config.getString("discord.punishment-webhook"), this.config.getString("discord.report-webhook"));

            
        FileConfiguration config = configProvider.getConfig();
            
        userCache.setMaxSize(config.getInt("cache.user.entry-count"));
        userCache.setMaxMemoryUsage(config.getInt("cache.user.max-size") * 1000 * 8);
        userCache.setTtl(config.getLong("cache.user.ttl"));

        onlineUserCache.setTtl(0L);

        punishmentCache.setMaxSize(config.getInt("cache.punishment.entry-count"));
        punishmentCache.setMaxMemoryUsage(config.getInt("cache.punishment.max-size") * 1000 * 8);
        punishmentCache.setTtl(config.getLong("cache.punishment.ttl"));

        getLogger().info("Running on server: " + type.name());

        // So, apparently Java gets all pissy and throws java.util.concurrent.RejectedExecutionException if spigot reloads.
        // I agree with Java, stop reloading spigot, it's bad.
        new Timer().scheduleAtFixedRate(new CacheRunnable(), 1000L, config.getLong("general.runnable-timer") * 1000L);
        new Timer().scheduleAtFixedRate(new QueryRunnable(), 1000L, config.getLong("general.runnable-timer") * 1000L);
        
        enabled = true;
    }

    /**
     * Get a user
     * 
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public User getUser(UUID uuid) {
        // If they are in the user cache

        if (userCache.contains(uuid.toString()))
            return userCache.get(uuid.toString());

        return User.resolveUser(uuid.toString());
    }

    /**
     * Get an online user
     * 
     * @param UUID the UUID of the user to lookup
     * @return The user if found, otherwise null
     */
    public User getOnlineUser(UUID uuid) {
        for (User user : getOnlineUserCache().getAll()) {
            if (user.getUniqueId().equals(uuid));
        }
        return null;
    }
    /**
     * Get an online user
     * 
     * @param username The username of the user to lookup
     * @return The user if found, otherwise null
     */
    public User getOnlineUser(String username) {
        for (User user : getOnlineUserCache().getAll()) {
            if (user.getName().equalsIgnoreCase(username));
                return user;
        }
        return null;
    }

    /**
     * Get a user
     * 
     * @deprecated Please use {@link #getUser(UUID)} as usernames are not unique past a single session
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public User getUser(String username) {
        final String name = username;
        User user = userCache.find((it) -> it.getName().equals(name));

        if (user != null)
            return user;

        return User.resolveUser(username);
    }

    /**
     * Get the USERS hashmap
     * @return The USERS hashmap from {@link com.ristexsoftware.lolbans.api.User}
     */
    public Collection<User> getOnlineUsers() {
        return onlineUserCache.getAll();
    }

    /**
     * Register a new user
     * @param player The player to register as a user
     */
    public void registerUser(org.bukkit.entity.Player player) {
        User user = new User(player.getName(), player.getUniqueId());
        userCache.put(user);
        onlineUserCache.put(user);
    }

    /**
     * Register a new user
     * @param user The user to register with LolBans
     */
    public void registerUser(User user) {
        userCache.put(user);
        onlineUserCache.put(user);
    }

    /**
     * Register a new user
     * @param player The player to register as a user
     */
    public void registerUser(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        User user = new User(player.getName(), player.getUniqueId());
        userCache.put(user);
        onlineUserCache.put(user);
    }

    /**
     * Unregister a user from the server
     * @param player The player to register as a user
     */
    public void removeUser(org.bukkit.entity.Player player) {
        for (User user : getOnlineUsers()) {
            if (user.getUniqueId() == player.getUniqueId())
                getOnlineUsers().remove(user);
        }
    }

    /**
     * Unregister a user from the server
     * @param player The player to register as a user
     */
    public void removeUser(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        for (User user : getOnlineUsers()) {
            if (user.getUniqueId() == player.getUniqueId())
                getOnlineUsers().remove(user);
        }
    }

    /**
     * Send all online staff members a message
     * @param message The message to send
     */
    public void notifyStaff(String message) {
        for (User user : getOnlineUsers()) {
            if (user.hasPermission("lolbans.alerts"))
                user.sendMessage(message);
        }
    }

    /**
     * Send all online staff members a message
     * @param message The message to send
     */
    public void notifyStaff(String message, String permission) {
        for (User user : getOnlineUsers()) {
            if (user.hasPermission(permission))
                user.sendMessage(message);
        }
    }

    /**
     * Broadcast a message to all online players
     * @param message The message to send
     */
    public void broadcastMessage(String message) {
        for (User user : getOnlineUsers()) {
            user.sendMessage(message);
        }
    }

    /**
     * Send a message to all users on the server depending on whether it is
     * silent and they have the alerts permission present.
     * @param message The message to send to all users on the server
     * @param silent Whether the announcement should be sent to everyone in the server or just people with the alerts permission
     */
    public void broadcastEvent(String message, boolean silent) {
        getLogger().info(message);
        if (silent)
            notifyStaff(message);
        else
            broadcastMessage(message);
    }

    public void destroy() {
        getLogger().info("Shutting down executor pool...");
        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        configProvider.saveConfig();
        enabled = false;
    }
    
    // private static LolBans instance = null;

    // /**
    //  * Fetch a static reference to the LolBans singelton instance.
    //  */
    // public static LolBans getPlugin() {
    //     if (instance == null) 
    //         throw new RuntimeException("Cannot get plugin as it hasn't been instantiated");
    //     return instance;
    // }
}  
