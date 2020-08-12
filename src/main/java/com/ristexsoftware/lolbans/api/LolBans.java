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
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
// import java.util.ArrayList;

import com.ristexsoftware.lolbans.api.configuration.file.FileConfiguration;
// import com.ristexsoftware.lolbans.api.event.Event;
// import com.ristexsoftware.lolbans.api.event.Listener;
// import com.ristexsoftware.lolbans.api.event.RegisteredListener;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.runnables.CacheRunnable;
import com.ristexsoftware.lolbans.api.runnables.QueryRunnable;
import com.ristexsoftware.lolbans.api.utils.Cache;
import com.ristexsoftware.lolbans.api.utils.ServerType;

import org.jetbrains.annotations.NotNull;

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
public class LolBans extends JavaPlugin {
    @Getter
    private static LolBans plugin;
    
    @Getter
    @Setter
    private static ServerType serverType;

    public HashMap<Integer, Pattern> REGEX = new HashMap<Integer, Pattern>();

    @Deprecated
    public List<IPAddressString> BANNED_ADDRESSES = new Vector<IPAddressString>(); // Not sure why this is here, legacy? 

    @Getter private ExecutorService pool = Executors.newFixedThreadPool(3);

    @Setter
    @Getter
    private boolean chatMute = false;

    // Caches
    @Getter private Cache<User> userCache = new Cache<>(User.class);
    @Getter private Cache<User> onlineUserCache = new Cache<>(User.class);
    @Getter private Cache<Punishment> punishmentCache = new Cache<>(Punishment.class);

    public LolBans(@NotNull File dataFolder, @NotNull File file, ServerType type) throws FileNotFoundException {
        super(dataFolder, file);
        plugin = this;
        if (!this.getDataFolder().exists()) {
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            throw new FileNotFoundException("Please configure lolbans and restart the server! :)");
        }

        if (!(new File(this.getDataFolder(), "config.yml").exists())) {
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            throw new FileNotFoundException("Please configure lolbans and restart the server! :)");
        }

        FileConfiguration config = getPlugin().getConfig();

        userCache.setMaxSize(config.getInt("cache.user.entry-count"));
        userCache.setMaxMemoryUsage(config.getInt("cache.user.max-size") * 1000 * 8);
        userCache.setTtl(config.getLong("cache.user.ttl"));

        onlineUserCache.setTtl(0L);

        punishmentCache.setMaxSize(config.getInt("cache.punishment.entry-count"));
        punishmentCache.setMaxMemoryUsage(config.getInt("cache.punishment.max-size") * 1000 * 8);
        punishmentCache.setTtl(config.getLong("cache.punishment.ttl"));

        setServerType(type);
        getLogger().info("Running on server: " + type.name());

        // So, apparently Java gets all pissy and throws java.util.concurrent.RejectedExecutionException if spigot reloads.
        // I agree with Java, stop reloading spigot, it's bad.
        new Timer().scheduleAtFixedRate(new CacheRunnable(), 1000L, config.getLong("general.runnable-timer") * 1000L);
        new Timer().scheduleAtFixedRate(new QueryRunnable(), 1000L, config.getLong("general.runnable-timer") * 1000L);
    }

    /**
     * Get a user
     * 
     * @param username The username of the user to lookup
     * @return The user if found, if not found, null
     */
    public User getUser(UUID uuid) {
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
    public User getUser(String username) {
        return User.resolveUser(username);
    }

    /**
     * Get the USERS hashmap
     * @return The USERS hashmap from {@link com.ristexsoftware.lolbans.api.User}
     */
    public HashMap<UUID, User> getOnlineUsers() {
        return User.USERS;
    }

    /**
     * Register a new user
     * @param player The player to register as a user
     */
    public void registerUser(org.bukkit.entity.Player player) {
        User user = new User(player.getName(), player.getUniqueId());
        userCache.put(user);
        getOnlineUsers().put(player.getUniqueId(), user);
    }

    /**
     * Register a new user
     * @param user The user to register with LolBans
     */
    public void registerUser(User user) {
        userCache.put(user);
        getOnlineUsers().put(user.getUniqueId(), user);
    }

    /**
     * Register a new user
     * @param player The player to register as a user
     */
    public void registerUser(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        User user = new User(player.getName(), player.getUniqueId());
        userCache.put(user);
        getOnlineUsers().put(player.getUniqueId(), user);
    }

    /**
     * Unregister a user from the server
     * @param player The player to register as a user
     */
    public void removeUser(org.bukkit.entity.Player player) {
        getOnlineUsers().remove(player.getUniqueId());
    }

    /**
     * Unregister a user from the server
     * @param player The player to register as a user
     */
    public void removeUser(net.md_5.bungee.api.connection.ProxiedPlayer player) {
        getOnlineUsers().remove(player.getUniqueId());
    }

    /**
     * Send all online staff members a message
     * @param message The message to send
     */
    public void notifyStaff(String message) {
        for (User user : getOnlineUsers().values()) {
            if (user.hasPermission("lolbans.alerts"))
                user.sendMessage(message);
        }
    }

    /**
     * Broadcast a message to all online players
     * @param message The message to send
     */
    public void broadcastMessage(String message) {
        for (User user : getOnlineUsers().values()) {
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
    }

    // private HashMap<Class<? extends Event>, ArrayList<Listener>> eventHandlers = new HashMap<>();

    // /**
    //  * Call an event object.
    //  */
    // public void callEvent(@NotNull Event event) {
    //     ArrayList<Listener> handlers = eventHandlers.get(event.getClass());
        
    //     handlers.
    // }

    // @Override
    // public void registerEvents(@NotNull Listener listener) {

    //     for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(listener, plugin).entrySet()) {
    //         getEventListeners(getRegistrationClass(entry.getKey())).registerAll(entry.getValue());
    //     }

    // }

    // @Override
    // public void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor) {
    //     registerEvent(event, listener, priority, executor, plugin, false);
    // }

    // /**
    //  * Registers the given event to the specified listener using a directly
    //  * passed EventExecutor
    //  *
    //  * @param event Event class to register
    //  * @param listener PlayerListener to register
    //  * @param priority Priority of this event
    //  * @param executor EventExecutor to register
    //  * @param plugin Plugin to register
    //  * @param ignoreCancelled Do not call executor if event was already
    //  *     cancelled
    //  */
    // @Override
    // public void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin, boolean ignoreCancelled) {
    //     Validate.notNull(listener, "Listener cannot be null");
    //     Validate.notNull(priority, "Priority cannot be null");
    //     Validate.notNull(executor, "Executor cannot be null");
    
    //     getEventListeners(event).register(new RegisteredListener(listener, executor, priority, plugin, ignoreCancelled));
    // }

    // @NotNull
    // private HandlerList getEventListeners(@NotNull Class<? extends Event> type) {
    //     try {
    //         Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
    //         method.setAccessible(true);
    //         return (HandlerList) method.invoke(null);
    //     } catch (Exception e) {
    //         throw new IllegalPluginAccessException(e.toString());
    //     }
    // }

}  
