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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.utils.Cacheable;
import com.ristexsoftware.lolbans.common.utils.Debug;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import lombok.Setter;

/**
 * Represents a player. Proxies bungee and bukkit methods.
 */
@SuppressWarnings("deprecation")
public class User implements Cacheable {
    public static HashMap<UUID, User> USERS = new HashMap<>();

    private String username;
    private UUID uuid;
    @Setter
    IPAddress ipAddress;

    public User(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    /**
     * Get the name of this user
     * 
     * @return The name of this user
     */
    public String getName() {
        if (this.isConsole()) {
            return "CONSOLE";
        }

        return this.username;
    }

    /**
     * Get the unique id of this user
     * 
     * @return The unique id of this user
     */
    public UUID getUniqueId() {
        return this.uuid;
    }

    public String getKey() {
        return this.uuid.toString();
    }

    /**
     * Check if this user is banned
     * 
     * @return True if this user is banned
     */
    public boolean isBanned() {
        try {
            return Database.isUserBanned(this.uuid).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if this user is online
     * 
     * @return True if the user is online
     */
    public boolean isOnline() {
        return USERS.containsKey(this.uuid);
    }

    /**
     * Check if this user is actually the console
     * 
     * @return True if this user is actually the console
     */
    public boolean isConsole() {
        return this.uuid.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    /**
     * Get the address of this user
     * 
     * @return The player's address, or null if an address can't be found
     */
    public String getAddress() {
        if (this.ipAddress != null)
            return this.ipAddress.toString();
        IPAddress ip = null;
        // Let's cache this.
        try {
            switch (LolBans.getServerType()) {
                case PAPER:
                case BUKKIT: {
                    org.bukkit.entity.Player player;
                    try {
                        Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                        player = (org.bukkit.entity.Player) bukkit.getDeclaredMethod("getPlayer", UUID.class)
                                .invoke(bukkit, this.uuid);
                        if (player != null)
                            ip = new IPAddressString(player.getAddress().getAddress().getHostAddress()).toAddress();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case BUNGEECORD: {
                    net.md_5.bungee.api.connection.ProxiedPlayer player;
                    try {
                        Class<?> proxy = Class.forName("net.md_5.bungee.api.ProxyServer");
                        player = (net.md_5.bungee.api.connection.ProxiedPlayer) proxy
                                .getDeclaredMethod("getPlayer", UUID.class).invoke(proxy, this.uuid);
                        if (player != null)
                            ip = new IPAddressString(player.getAddress().getAddress().getHostAddress()).toAddress();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                default:
                    ip = new IPAddressString(Database.getLastAddress(this.uuid.toString())).toAddress();
            }
        } catch (AddressStringException | IncompatibleAddressException e) {
            e.printStackTrace();
        }
        this.ipAddress = ip;
        return ip == null ? null : ip.toString();
    }

    /**
     * Check if a user has a permission
     * 
     * @return True if player has the permission
     */
    public Boolean hasPermission(String permission) {
        // Console always has full perms
        if (isConsole())
            return true;

        try {
            switch (LolBans.getServerType()) {
                case PAPER:
                case BUKKIT: {
                    org.bukkit.entity.Player player;
                    try {
                        Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                        player = (org.bukkit.entity.Player) bukkit.getDeclaredMethod("getPlayer", UUID.class)
                                .invoke(bukkit, this.uuid);
                        if (player != null) {
                            if (LolBans.getPlugin().getConfig().getBoolean("general.ops-bypass-permissions")
                                    && player.isOp())
                                return true;
                            return player.hasPermission(permission);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case BUNGEECORD: {
                    net.md_5.bungee.api.connection.ProxiedPlayer player;
                    try {
                        // Class<?> proxy = Class.forName("net.md_5.bungee.api.ProxyServer");
                        player = (net.md_5.bungee.api.connection.ProxiedPlayer) com.ristexsoftware.lolbans.bungeecord.Main
                                .getPlayer(this.uuid);
                        if (player != null)
                            return player.hasPermission(permission);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Disconnect a user from the server
     * 
     * @param message The message to send
     */
    public void disconnect(String message) {
        if (message == null || message.equals(""))
            message = "You have been kicked by an operator!";

        switch (LolBans.getServerType()) {
            case PAPER:
            case BUKKIT: {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(this.username);
                player.kickPlayer(message);
            }
            case BUNGEECORD: {
                net.md_5.bungee.api.connection.ProxiedPlayer player = net.md_5.bungee.api.ProxyServer.getInstance()
                        .getPlayer(this.uuid);
                player.disconnect(message);
            }
            default:
                throw new UnknownError("something is horribly wrong");
        }
    }

    /**
     * Send a message to a user.
     * 
     * @param message The message to send
     * @return If the user is not online
     * @return If the player is not found
     * @throws NullPointerException if <code>message</code> is null
     */
    public void sendMessage(String message) {

        if (isConsole()) {
            return;
        }

        if (!isOnline()) {
            return;
        }

        switch (LolBans.getServerType()) {
            case PAPER:
            case BUKKIT: {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(this.uuid);
                if (player != null) {
                    player.sendMessage(message);
                }
            }
            case BUNGEECORD: {
                net.md_5.bungee.api.connection.ProxiedPlayer player = com.ristexsoftware.lolbans.bungeecord.Main
                        .getPlayer(this.uuid);
                if (player != null) {
                    player.sendMessage(message);
                }
            }
            default:
                throw new UnknownError("something is horribly wrong");
        }
    }

    public static User resolveUser(String username) {
        Debug debug = new Debug(User.class);

        if (username == null || username.equals(""))
            return null;

        try {
            boolean isuuid = Pattern
                    .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", username);

            // Let's check our users hashmap first, save a lot of time.
            for (User u : USERS.values()) {
                if (u.getName().equals(username) || isuuid && u.getUniqueId().equals(UUID.fromString(username))) {
                    debug.print(String.format("Pulled entry for %s from users hashmap", u.getName()));
                    LolBans.getPlugin().getUserCache().put(u);
                    debug.print("Cached user " + u.getName());
                    return u;
                }
            }

            // Lets also check our cache to save even more time.
            final String fuckyoujava = username;
            User cache = LolBans.getPlugin().getUserCache().find((t) -> t.username == fuckyoujava);
            if (cache != null) {
                debug.print("Pulled entry for " + cache.getName() + " from cache");
                return cache;
            }

            try {
                PreparedStatement ps = Database.connection.prepareStatement(
                        "SELECT player_uuid, player_name FROM lolbans_users WHERE player_uuid OR player_name = ?");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    User user = new User(rs.getString("player_name"), UUID.fromString(rs.getString("player_uuid")));
                    debug.print(String.format("Pulled entry for %s from database", user.getName()));
                    LolBans.getPlugin().getUserCache().put(user);
                    debug.print("Cached user " + user.getName());
                    return user;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                // ignore and continue
            }
            // Alright lets do the more expensive operation of querying an API for the UUID
            // to fetch the Username.
            // api.mojang.com is slow as fuck, but i'll make this a config option
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
            JsonElement jsonResponse = new JsonParser().parse(new InputStreamReader(url.openStream()));
            String uuid = jsonResponse.getAsJsonObject().get("uuid").toString().replace("\"", "");
            username = jsonResponse.getAsJsonObject().get("username").toString().replace("\"", "");

            debug.print("Requesting user from API");
            if (uuid == null)
                return null;
            User user = new User(username, UUID.fromString(uuid));
            debug.print(String.format("Pulled entry for %s from API", user.getName()));
             LolBans.getPlugin().getUserCache().put(user);
            debug.print("Cached user " + user.getName());
            return user;

        } catch (IOException e) {
            // e.printStackTrace();
            return null;
        }
    }

    public static User getConsoleUser() {
        return new User("CONSOLE", UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    /**
     * Send the player a permission denied message
     * 
     * @param permissionNode The permission node they're being denied for
     * @return always true, for use in the command classes.
     */
    public boolean permissionDenied(String permissionNode) {
        try {
            sendMessage(Messages.translate("no-permission", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("sender", getName());
                    put("permission", permissionNode);
                }
            }));
        } catch (Exception ex) {
            ex.printStackTrace();
            sendMessage("Permission Denied!");
        }
        return true;
    }

    public Timestamp getLastLogin() {
        FutureTask<Timestamp> t = new FutureTask<>(new Callable<Timestamp>() {
            @Override
            public Timestamp call() {
                // This is where you should do your database interaction
                try {
                    PreparedStatement ps = Database.connection
                            .prepareStatement("SELECT last_login FROM lolbans_users WHERE player_uuid = ? LIMIT 1");
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    return rs.next() ? rs.getTimestamp("last_login") : null;
                } catch (Throwable e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        LolBans.getPlugin().getPool().execute(t);
        try {
            return t.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}