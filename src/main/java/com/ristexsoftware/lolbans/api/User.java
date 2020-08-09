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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ristexsoftware.lolbans.api.configuration.ConfigurationSection;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.utils.Cacheable;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
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
        Punishment punishment = LolBans.getPlugin().getPunishmentCache().find((it) -> (it.getType() == PunishmentType.BAN 
                                                    && !it.getAppealed() && it.getTarget().getUniqueId().toString() == uuid.toString()));

        if (punishment != null) {
            return true;
        }

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

    public void disconnect(Punishment punishment) {
        if (punishment != null) {
            PunishmentType type = punishment.getType();
            TreeMap<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", punishment.getTarget().getName());
                    put("reason", punishment.getReason());
                    put("arbiter", punishment.getPunisher().getName());
                    put("expiry", punishment.getExpiresAt() == null ? "" : punishment.getExpiresAt().toString());
                    put("silent", Boolean.toString(punishment.getSilent()));
                    put("appealed",Boolean.toString(punishment.getAppealed()));
                    put("expires",Boolean.toString(punishment.getExpiresAt() != null && !punishment.getAppealed()));
                    put("punishid", punishment.getPunishID());
                }
            };
            try {
                switch(type) {
                    case BAN:
                        disconnect(Messages.translate(punishment.getExpiresAt() == null ? "ban.perm-ban-message" : "ban.temp-ban-message", vars));
                        break;
                    case MUTE:
                        disconnect(Messages.translate("mute.you-were-muted", vars));
                        break;
                    case KICK:
                        disconnect(Messages.translate("kick.kick-message", vars));
                        break;
                    case WARN:
                        disconnect(Messages.translate("warn.warn-kick-message", vars));
                        break;
                    case IP:
                        disconnect(Messages.translate(punishment.getExpiresAt() == null ? "ip-ban.perm-ip-ban-message" : "ip-ban.temp-ip-ban-message", vars));
                        break;
                    case REGEX:
                        disconnect(Messages.translate(punishment.getExpiresAt() == null ? "regex-ban.perm-ban-message" : "regex-ban.temp-ban-message", vars));
                        break;
                    default:
                        break;
                }
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
                disconnect(Messages.serverError);
            }
        }
    }

    /**
     * Disconnect a user from the server
     * 
     * @param message The message to send
     */
    public void disconnect(String message) {
        if (message == null || message.equals(""))
            message = "You have been kicked by an operator!";

        final String msg = message; // MuSt Be FiNaL

        switch (LolBans.getServerType()) {
            case PAPER:
            case BUKKIT: {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(this.username);
                // player.kickPlayer(msg);
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                        com.ristexsoftware.lolbans.bukkit.Main.getPlugin(com.ristexsoftware.lolbans.bukkit.Main.class),
                        () -> player.kickPlayer(msg), 1L);
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
            System.out.println(message);
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
            final String uwuwuwuwuwuwuwuw = username;
            User cache = LolBans.getPlugin().getUserCache().find((t) -> t.username.equalsIgnoreCase(uwuwuwuwuwuwuwuw));
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

    /**
     * Send a message to a player from messages.yml whose only argument is the
     * player name
     * 
     * @param configNode The message node from messages.yml
     * @param playerName The name of the player to use as a placeholder
     * @param ret        The value to return from this function
     * @return The value provided as <code>ret</code>
     */
    public boolean sendReferencedLocalizedMessage(String configNode, String playerName, boolean ret) {
        try {
            sendMessage(Messages.translate(configNode, new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", playerName);
                    put("ipaddress", playerName);
                    put("sender", getName());
                }
            }));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return ret;
    }

        /**
     * Remove a punishment from a player
     * 
     * @param type   The punishment type to remove
     * @param reason The reason for removal
     * @param silent Is the punishment removal silent
     */
    public Punishment removeLatestPunishmentOfType(PunishmentType type, User unpunisher, String reason, boolean silent) {
        Punishment op = Punishment.findPunishment(type, this, false);
        op.setAppealReason(reason);
        op.setAppealed(true);
        op.setAppealedAt(TimeUtil.now());
        op.setAppealedBy(unpunisher);

        op.update(op.getPunishID());
  
        // try {
        //     DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        // } catch (InvalidConfigurationException e) {
        //     e.printStackTrace();
        // }

        return op;
    }

    public Timestamp getTimeGroup() {

        Timestamp defaultTime = TimeUtil.toTimestamp(LolBans.getPlugin().getConfig().getString("max-time.default"));
        ConfigurationSection configTimeGroups = LolBans.getPlugin().getConfig().getConfigurationSection("max-time");
        ArrayList<String> timeGroups = new ArrayList<String>();
        
        timeGroups.addAll(configTimeGroups.getKeys(false));
        Collections.reverse(timeGroups);

        for (String key : timeGroups) {
            if (hasPermission("lolbans.maxtime." + key)) {
                return TimeUtil.toTimestamp(LolBans.getPlugin().getConfig().getString("max-time." + key));
            }
        }

        return defaultTime == null ? TimeUtil.toTimestamp("7d") : defaultTime;
    }


}