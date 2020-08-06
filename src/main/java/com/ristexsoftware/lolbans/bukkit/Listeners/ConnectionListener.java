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

package com.ristexsoftware.lolbans.bukkit.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.utils.IPUtil;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.common.utils.CacheUtil;
import com.ristexsoftware.lolbans.common.utils.Debug;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import inet.ipaddr.IPAddressString;

public class ConnectionListener implements Listener {

    /************************************************************************************
     * Our event listeners.
     */

    // I changed this this PlayerLoginEvent instead of PlayerPreLoginEvent because
    // you're actually suppose to do ban checks and whatnot here
    // And I needed the Player object to do permission checking
    private static LolBans self = LolBans.getPlugin();

    @EventHandler
    public static void OnPlayerConnectAsync(PlayerLoginEvent event) {
        // Since PlayerLogineEvent is not asynchronous, we need to run this on a new
        // thread, we don't want to make the main thread wait for this to complete
        Debug debug = new Debug(ConnectionListener.class);
        debug.print(String.format("User %s (%s) is joining the server", event.getPlayer().getName(),
                event.getPlayer().getUniqueId().toString()));
        Player player = event.getPlayer();
        User user = new User(player.getName(), player.getUniqueId());
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Timestamp login = TimeUtil.TimestampNow();

                    // Before we do anything, make sure they're abiding the ratelimit
                    if (self.getConfig().getBoolean("connection.rate-limiting.enabled")
                            && !player.hasPermission("lolbans.ratelimit.bypass")) {
                        debug.print("Rate limiting is enabled, " + player.getName()
                                + " does not have 'lolbans.ratelimit.bypass'");

                        Integer limit = self.getConfig().getInt("connection.rate-limiting.limit");
                        if (limit <= 0)
                            limit = 6;

                        Timestamp lastLogin = user.getLastLogin();
                        Timestamp limitStamp = new Timestamp(TimeUtil.getUnixTime() * 1000L + limit);

                        if (lastLogin.getTime() + (limit * 1000) >= limitStamp.getTime()) {
                            debug.print(String.format("%s has reached the rate limit threshold, kicking player",
                                    player.getName()));
                            final Integer ihatejava = limit;
                            Map<String, String> variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("ipaddress", event.getAddress().toString());
                                    put("player", player.getName());
                                    put("rate", String.valueOf(
                                            ihatejava - ((limitStamp.getTime() - lastLogin.getTime()) / 1000)));
                                }
                            };
                            event.disallow(Result.KICK_OTHER,
                                    Messages.translate("rate-limit.limit-reached", variables));
                            debug.print("PlayerLoginEvent complete");
                            return true; // Lets return so we don't update the login time!
                        }
                    }

                    // lets do this too
                    // InsertUser has a check to see if they've joined before, so it's safe to use
                    // as the update event aswell.
                    Database.insertUser(player.getUniqueId().toString(), player.getName(),
                            event.getAddress().getHostAddress().toString(), login, login);
                    // To save time, we do a few things here:
                    // We make a query to the database asking for every punishment
                    // First, we check if the player is IP Banned,
                    // Then, we check if they are normally banned,
                    // And finally, we check if they are warned

                    boolean foundIP = false;
                    boolean foundBan = false;
                    boolean foundWarn = false;

                    for (Punishment punish : CacheUtil.getPunishments().values()) {
                        Map<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                            {
                                put("punishid", punish.getPunishId());
                                put("ipaddress", event.getRealAddress().getHostAddress());
                                put("player", user.getName());
                                put("reason", punish.getReason());
                                put("arbiter", punish.getPunisher().getName());
                                Timestamp expiresAt = punish.getExpiresAt();
                                put("expiry", expiresAt != null ? expiresAt.toString() : "Never");
                            }
                        };
                        if (!punish.getRegexBan() && (punish.getTarget() == null ? punish.getIpAddress().toString().equals(event.getRealAddress().getHostAddress()) && !punish.getAppealed() : punish.getTarget().getUniqueId()) == user.getUniqueId()) {
                            PunishmentType type = punish.getType();
                            if (!foundIP && type == PunishmentType.IP && punish.getIpAddress().toString().equals(event.getRealAddress().getHostAddress())) {
                                foundIP = true;

                                String disconnectReason = Messages.translate(
                                        punish.getExpiresAt() != null ? "ip-ban.temp-ip-ban-message"
                                                : "ip-ban.perm-ip-ban-message",
                                        vars);
                                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, disconnectReason);

                                continue;
                            }
                        }
                    }

                    final PreparedStatement punishmentQuery = Database.connection.prepareStatement(
                            "SELECT * FROM lolbans_punishments WHERE target_uuid = ? OR target_ip_address = ? AND appealed = FALSE AND warning_ack = FALSE OR (expires_at IS NOT NULL >= NOW() OR expires_at IS NULL)");
                    punishmentQuery.setString(1, player.getUniqueId().toString());
                    punishmentQuery.setString(2, event.getRealAddress().getHostAddress());

                    ResultSet punishmentRecord = Database.executeLater(punishmentQuery).get().get();

                    // So basically, we need to go in this order for punishments
                    // IP Ban check > Normal ban check > Warning check
                    // If the first result in the resultset isn't an IP Ban, we want to ignore it
                    // until we've found an IP Ban
                    while (punishmentRecord.next()) {

                        PunishmentType type = PunishmentType.fromOrdinal(punishmentRecord.getInt("type"));
                        Map<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                            {
                                put("punishid", punishmentRecord.getString("punish_id"));
                                put("ipaddress", event.getRealAddress().getHostAddress());
                                put("player", user.getName());
                                put("reason", punishmentRecord.getString("reason"));
                                put("arbiter", punishmentRecord.getString("punished_by_name"));
                                Timestamp expiresAt = punishmentRecord.getTimestamp("expires_at");
                                put("expiry", expiresAt != null ? expiresAt.toString() : "Never");
                            }
                        };

                        // We want to check for ip bans first.
                        if (!foundIP && PunishmentType.fromOrdinal(punishmentRecord.getInt("type")) == PunishmentType.IP
                                && punishmentRecord.getString("target_ip_address")
                                        .equals(event.getRealAddress().getHostAddress())) {
                            foundIP = true;

                            String disconnectReason = Messages.translate(
                                    punishmentRecord.getTimestamp("expires_at") != null ? "ip-ban.temp-ip-ban-message"
                                            : "ip-ban.perm-ip-ban-message",
                                    vars);
                            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, disconnectReason);

                            punishmentRecord.beforeFirst();
                            continue;
                        }

                        if (!foundBan
                                && PunishmentType.fromOrdinal(punishmentRecord.getInt("type")) == PunishmentType.BAN) {
                            foundBan = true;

                            String disconnectReason = Messages.translate(
                                    punishmentRecord.getTimestamp("expires_at") != null ? "ban.temp-ban-message"
                                            : "ban.perm-ban-message",
                                    vars);
                            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, disconnectReason);

                            punishmentRecord.beforeFirst();
                            continue;
                        }

                        if (!foundWarn
                                && PunishmentType.fromOrdinal(punishmentRecord.getInt("type")) == PunishmentType.WARN) {
                            foundWarn = true;

                            String disconnectReason = Messages.translate("warn.warn-kick-message", vars);
                            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, disconnectReason);

                            punishmentRecord.beforeFirst();
                            continue;
                        }

                        // Let's cache these!
                        switch (type) {
                            case IP:
                                CacheUtil
                                        .putPunishment(
                                                new Punishment(
                                                        new User(punishmentRecord.getString("punished_by_name"),
                                                                UUID.fromString(punishmentRecord
                                                                        .getString("punished_by_uuid"))),
                                                        punishmentRecord.getString("reason"),
                                                        punishmentRecord.getTimestamp("expires_at"),
                                                        punishmentRecord.getBoolean("silent"),
                                                        punishmentRecord.getBoolean("appealed"),
                                                        new IPAddressString(
                                                                punishmentRecord.getString("target_ip_address"))
                                                                        .toAddress()));
                                break;
                            case BAN:
                            case MUTE:
                                CacheUtil.putPunishment(new Punishment(type,
                                        new User(punishmentRecord.getString("punished_by_name"),
                                                UUID.fromString(punishmentRecord.getString("punished_by_uuid"))),
                                        user, punishmentRecord.getString("reason"),
                                        punishmentRecord.getTimestamp("expires_at"),
                                        punishmentRecord.getBoolean("silent"),
                                        punishmentRecord.getBoolean("appealed")));
                                break;
                            case WARN:
                                Punishment warn = new Punishment(type,
                                        new User(punishmentRecord.getString("punished_by_name"),
                                                UUID.fromString(punishmentRecord.getString("punished_by_uuid"))),
                                        user, punishmentRecord.getString("reason"),
                                        punishmentRecord.getTimestamp("expires_at"),
                                        punishmentRecord.getBoolean("silent"), punishmentRecord.getBoolean("appealed"));

                                if (!foundWarn) {
                                    warn.setWarningAck(true);
                                    warn.update(punishmentRecord.getString("punish_id"));
                                }
                                CacheUtil.putPunishment(warn);
                                break;
                        }
                    }

                    String rDNS = IPUtil.rDNSQUery(event.getRealAddress().getHostAddress());
                    // Do Regex matches since they're pre-compiled
                    Iterator<?> it = LolBans.REGEX.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<?, ?> pair = (Map.Entry<?, ?>) it.next();
                        Pattern regex = (Pattern) pair.getValue();
                        // Matchers to make things more efficient.
                        Matcher nameMatcher = regex.matcher(user.getName());
                        Matcher ipMatcher = regex.matcher(event.getRealAddress().getHostAddress());
                        Matcher hostMatcher = regex.matcher(rDNS);

                        // If any of them match, we must query the database for the record
                        // then disconnect them for matching something.
                        if (nameMatcher.find() || ipMatcher.find() || hostMatcher.find()) {
                            // FIXME: AND (Expiry IS NULL OR Expiry >= NOW()) -- how do we handle expired
                            // regex bans?
                            PreparedStatement ps = Database.connection.prepareStatement(
                                    "SELECT * FROM lolbans_regexbans WHERE id = ? AND appealed = FALSE OR (expires_at IS NOT NULL >= NOW() OR expires_at IS NULL)");
                            ps.setInt(1, (Integer) pair.getKey());
                            ResultSet result = ps.executeQuery();

                            System.out.println((Integer) pair.getKey());
                            // Something's fucked? lets make note.
                            if (!result.next()) {
                                // throw new SQLException("No such regex " + regex.pattern());
                                LolBans.getLogger().info("No such regex: " + regex.pattern()); // Log the issue
                                LolBans.REGEX.remove(pair.getKey()); // The ban doesn't exist in the database, don't
                                                                     // keep
                                                                     // it around
                                continue; // Skip this iteration, go to the next
                            }

                            Timestamp expiresAt = result.getTimestamp("expires_at");
                            Boolean appealed = result.getBoolean("appealed");
                            Map<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {

                                    put("punish_id", punishmentRecord.getString("punish_id"));
                                    put("ipaddress", event.getRealAddress().getHostAddress());
                                    put("target", user.getName());
                                    put("reason", punishmentRecord.getString("reason"));
                                    put("punisher", punishmentRecord.getString("punisher_name"));
                                    put("rdns", rDNS);
                                    put("regex", regex.pattern());
                                    put("expiry", expiresAt != null ? expiresAt.toString() : "");
                                }
                            };

                            if (!appealed) {
                                user.disconnect(Messages.translate(
                                        expiresAt != null ? "regex-ban.temp-ban-message" : "regex-ban.perm-ban-message",
                                        vars));
                            }
                        }
                    }

                    // Check to make sure they're not an ALT account
                    Future<UUID> AltRecords = IPUtil.checkAlts(event.getRealAddress());
                    UUID altaccount = AltRecords.get();
                    if (altaccount != null) {
                        // OfflinePlayer p = Bukkit.getOfflinePlayer(altaccount);
                        User user = LolBans.getUser(altaccount);
                        // Send a message to all ops with broadcast perms.
                        String message = Messages.translate("ip-ban.ip-alt-notification",
                                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                    {
                                        put("player", player.getName());
                                        put("bannedplayer", user.getName());
                                        put("IPADDRESS", event.getAddress().getHostAddress());
                                    }
                                });

                        LolBans.notifyStaff(message);
                        self.getLogger().warning(message);

                        if (self.getConfig().getBoolean("ip-ban-settings.kick-alt-accounts", false)) {
                            event.disallow(Result.KICK_BANNED, Messages.translate("ip-ban.ip-alt-ban-message",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                        {
                                            // TODO: This!
                                            put("PLAYERNAME", user.getName());
                                            put("ALTACCOUNT", user.getUniqueId().toString());

                                            put("arbiter", "");
                                            put("REASON", "");
                                            put("EXPIRYDURATION", "");
                                            put("PUNISHID", "");
                                        }
                                    }));
                        }

                        // TODO: Send to discord
                        // They're not banned and have no pending warnings, allow them to connect or
                        // other plugins to perform their actions.
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // Kick if there was a server error.
                    if (self.getConfig().getBoolean("general.kick-connection-error"))
                        event.disallow(Result.KICK_FULL, Messages.serverError);
                    return false;
                }
                return true;
            }
        });

        LolBans.pool.execute(t);

        try {
            if (!t.get()) {
                if (self.getConfig().getBoolean("general.kick-connection-error"))
                    event.disallow(Result.KICK_FULL, Messages.serverError);
            }
        } catch (InterruptedException | ExecutionException e) {
            if (self.getConfig().getBoolean("general.kick-connection-error"))
                event.disallow(Result.KICK_FULL, Messages.serverError);
            e.printStackTrace();
        }
        LolBans.registerUser(user);
        debug.print("PlayerLoginEvent completed");
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        LolBans.removeUser(player);
        try {
            if (!(Database.updateUser(player.getUniqueId().toString(), player.getName(),
                    player.getAddress().getAddress().getHostAddress(), new Timestamp(System.currentTimeMillis()))
                    .get()))
                LolBans.getLogger().severe(Messages.serverError);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        LolBans.removeUser(player);
        try {
            if (!(Database.updateUser(player.getUniqueId().toString(), player.getName(),
                    player.getAddress().getAddress().getHostAddress(), new Timestamp(System.currentTimeMillis()))
                    .get()))
                LolBans.getLogger().severe(Messages.serverError);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}