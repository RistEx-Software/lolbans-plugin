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
// import java.net.InetSocketAddress;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.utils.Cacheable;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.common.utils.Debug;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import com.ristexsoftware.knappy.configuration.ConfigurationSection;
import com.ristexsoftware.knappy.translation.Translation;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a player. Proxies bungee and bukkit methods.
 */
public class User implements Cacheable {
    public static HashMap<UUID, User> USERS = new HashMap<>();

    private String username;
    private UUID uuid;
    @Setter
    private IPAddress ipAddress;
    @Getter
    @Setter
    private boolean isFrozen = false;

    @Getter
    private boolean commandConfirm = false;

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
     * Check if this user has an active punishment of a specific type
     * 
     * @param type The punishment type to look up
     * @return True if this user is banned
     */
    public boolean isPunished(PunishmentType type) {
        Punishment punishment = LolBans.getPlugin().getPunishmentCache().find((it) -> (it.getType() == type && !it.getAppealed() && it.getTarget().getUniqueId().toString() == uuid.toString()));

        if (punishment != null) {
            LolBans.getPlugin().getPunishmentCache().put(punishment);
            return true;
        }

        try {
            return Database.isUserPunished(type, this.uuid).get();
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
        if (isConsole()) {
            return true;
        }
        return LolBans.getPlugin().getOnlineUserCache().contains(getUniqueId().toString());
    }

    public void setCommandConfirm(boolean commandConfirm) {
        this.commandConfirm = commandConfirm;
        // Start a timer for 10 seconds, then invalidate the confirmation
        new java.util.Timer().schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    setCommandConfirm(false);
                }
            }, 10000L);
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
    public IPAddress getAddress() {
        if (this.ipAddress != null)
            return this.ipAddress;
       
        return LolBans.getPlugin().getUserProvider().getAddress(this);
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

        return LolBans.getPlugin().getUserProvider().hasPermission(this, permission);
    }

    public void disconnect(Punishment punishment) {
        if (punishment != null) {
            PunishmentType type = punishment.getType();
            TreeMap<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", punishment.getTarget() == null ? "" : punishment.getTarget().getName());
                    put("ipaddress", punishment.getIpAddress() == null ? "#" : punishment.getIpAddress().toString());
                    put("censoredipaddress", punishment.getIpAddress() == null ? "#" : Translation.censorWord(punishment.getIpAddress().toString()));
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

        LolBans.getPlugin().getUserProvider().disconnect(this, message);
    }

    /**
     * Send a player a message
     * @param punishment The punishment for the message to send to the player
     */
    public void sendMessage(Punishment punishment) {
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
                    case MUTE: {
                        if (!punishment.getAppealed())
                            sendMessage(Messages.translate("mute.you-were-muted", vars));
                        else
                            sendMessage(Messages.translate("mute.you-were-unmuted", vars));
                    }
                        break;
                    case WARN:
                        sendMessage(Messages.translate("warn.warned-message", vars));
                        break;
                    default:
                        break;
                }
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
                sendMessage(Messages.serverError);
            }
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

        if (isConsole()) 
            System.out.println(message);
        
        if (isOnline()) {
            LolBans.getPlugin().getUserProvider().sendMessage(this, message);
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
                    // debug.print("Cached user " + u.getName());
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
                        "SELECT player_uuid, player_name FROM lolbans_users WHERE player_uuid = ? OR player_name = ?");
                
                ps.setString(1, username);
                ps.setString(2, username);
                  
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
            debug.print("Requesting user from API...");
                        
            URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
            JsonElement jsonResponse = new JsonParser().parse(new InputStreamReader(url.openStream()));
            String uuid = jsonResponse.getAsJsonObject().get("uuid").toString().replace("\"", "");
            username = jsonResponse.getAsJsonObject().get("username").toString().replace("\"", "");
            
            if (uuid == null) {
                debug.print("No user could be found");
                return null;
            }

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
                    put("affected", playerName);
                }
            }));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Fetch the latest punishment of a given type.
     */
    public Punishment getLatestPunishmentOfType(PunishmentType type) {
        Punishment punishment = LolBans.getPlugin().getPunishmentCache().find((it) -> (it.getType() == type && !it.getAppealed() && it.getTarget().getUniqueId().toString() == uuid.toString()));

        if (punishment != null) {
            LolBans.getPlugin().getPunishmentCache().put(punishment);
            return punishment;
        }

        return Punishment.findPunishment(type, uuid.toString(), false);
    }

    /**
     * Remove a punishment from a player
     * 
     * @param type   The punishment type to remove
     * @param reason The reason for removal
     * @param silent Is the punishment removal silent
     */
    public Punishment removeLatestPunishmentOfType(PunishmentType type, User unpunisher, String reason, boolean silent) {
        Punishment op = Punishment.findPunishment(type, uuid.toString(), false);
        op.appeal(unpunisher, reason, silent);
  
        // TODO: Discord util
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