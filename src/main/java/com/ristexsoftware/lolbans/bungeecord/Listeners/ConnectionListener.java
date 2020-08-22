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

package com.ristexsoftware.lolbans.bungeecord.Listeners;

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

import com.ristexsoftware.knappy.util.Debugger;
import com.ristexsoftware.knappy.util.TimeUtil;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.MaintenanceLevel;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.utils.IPUtil;

import inet.ipaddr.IPAddressString;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ConnectionListener implements Listener {

    private static LolBans self = LolBans.getPlugin();
    
    @EventHandler
    public void onLogin(PostLoginEvent event) {
        // Since PostLoginEvent is not asynchronous, we need to run this on a new
        // thread, we don't want to make the main thread wait for this to complete
        Debugger debug = new Debugger(ConnectionListener.class);
        debug.print(String.format("User %s (%s) is joining the server", event.getPlayer().getName(), event.getPlayer().getUniqueId().toString()));
        ProxiedPlayer player = event.getPlayer();
        User user = new User(player.getName(), player.getUniqueId());
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Timestamp login = TimeUtil.now();
                    if (LolBans.getPlugin().getMaintenanceModeEnabled() && !player.hasPermission("lolbans.maintenance."+MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()).toLowerCase())) {
                        player.disconnect(LolBans.getPlugin().getLocaleProvider().translate("maintenance.kick-message", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
                            put("maintenancelevel", MaintenanceLevel.displayName(LolBans.getPlugin().getMaintenanceLevel()));
                        }}));
                    }

                    // Before we do anything, make sure they're abiding the ratelimit
                    if (self.getConfig().getBoolean("connection.rate-limiting.enabled")
                            && !event.getPlayer().hasPermission("lolbans.ratelimit.bypass")) {
                        debug.print("Rate limiting is enabled, " + event.getPlayer().getName()
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
                                    put("ipaddress", player.getAddress().getAddress().getHostAddress().toString());
                                    put("player", player.getName());
                                    put("rate", String.valueOf(
                                            ihatejava - ((limitStamp.getTime() - lastLogin.getTime()) / 1000)));
                                }
                            };
                            user.disconnect(LolBans.getPlugin().getLocaleProvider().translate("rate-limit.limit-reached", variables));
                            debug.print("PlayerLoginEvent complete");
                            return true; // Lets return so we don't update the login time!
                        }
                    }

                    // lets do this too
                    // InsertUser has a check to see if they've joined before, so it's safe to use
                    // as the update event aswell.
                    Database.insertUser(player.getUniqueId().toString(), player.getName(),
                            player.getAddress().getAddress().getHostAddress().toString(), login, login);
                    // To save time, we do a few things here:
                    // We make a query to the database asking for every punishment
                    // First, we check if the player is IP Banned,
                    // Then, we check if they are normally banned,
                    // And finally, we check if they are warned

                    boolean foundIP = false;
                    boolean foundBan = false;
                    boolean foundRegex = false;
                    boolean foundWarn = false;

                    debug.print("Checking cache for outstanding punishments...");

                    for (Punishment punish : LolBans.getPlugin().getPunishmentCache().getAll()) {
                        if (punish.getExpiresAt() != null && punish.getExpiresAt().getTime() <= System.currentTimeMillis()) {
                            punish.expire();
                        }

                        if (punish.getType() == PunishmentType.REGEX) {
                            debug.print(punish.getRegex() + " " + user.getName());
                        }

                        if (punish.getAppealed()) {
                            continue;
                        }

                        Map<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                            {
                                put("punishid", punish.getPunishID());
                                put("ipaddress", player.getAddress().getAddress().getHostAddress().toString());
                                put("player", user.getName());
                                put("reason", punish.getReason());
                                put("arbiter", punish.getPunisher().getName());
                                Timestamp expiresAt = punish.getExpiresAt();
                                put("expiry", expiresAt != null ? expiresAt.toString() : "Never");
                                put("regex", punish.getRegex() == null ? "" : punish.getRegex());
                            }
                        };
                    
                        PunishmentType type = punish.getType();
                        if (!foundIP && type == PunishmentType.IP && punish.getIpAddress().toString().equals(player.getAddress().getAddress().getHostAddress().toString())) {
                            foundIP = true;
                            
                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate(
                                    punish.getExpiresAt() != null ? "ip-ban.temp-ip-ban-message"
                                            : "ip-ban.perm-ip-ban-message",
                                    vars);
                            
                            debug.print("Found a valid and matching IP ban - disconnecting...");
                            user.disconnect(disconnectReason);

                            continue;
                        }

       
                        if (!foundRegex
                                && type == PunishmentType.REGEX
                                && Pattern.matches(punish.getRegex(), user.getName())) {
                            
                            foundRegex = true;
                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate(punish.getExpiresAt() != null ? "regex-ban.temp-ban-message" : "regex-ban.perm-ban-message", vars);
                            
                            debug.print("Found a valid and matching regex ban - disconnecting...");
                            user.disconnect(disconnectReason);
                        }

                        if (punish.getTarget() == null
                                || !punish.getTarget().getUniqueId().equals(user.getUniqueId())) {
                            continue;
                        }

                        if (!foundBan
                                && type == PunishmentType.BAN) {

                            foundBan = true;

                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate(
                                    punish.getExpiresAt() != null ? "ban.temp-ban-message"
                                            : "ban.perm-ban-message",
                                    vars);

                             debug.print("Found a valid ban - disconnecting...");
                             user.disconnect(disconnectReason);

                            continue;
                        }

                        if (!foundWarn && type == PunishmentType.WARN && !punish.getWarningAck()) {
                            foundWarn = true;

                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate("warn.warn-kick-message", vars);

                            debug.print("Found a valid and unacknowledged warn - disconnecting...");
                            user.disconnect(disconnectReason);

                            debug.print("Marking outstanding warning as acknowledged");
                            punish.setWarningAck(true);
                            punish.update();

                            continue;
                        }

                    }

                    if (foundBan || foundIP || foundRegex || foundWarn)
                        return false;

                    final PreparedStatement punishmentQuery = Database.connection.prepareStatement(
                            "SELECT * FROM lolbans_punishments WHERE appealed = FALSE AND (expires_at IS NOT NULL >= NOW() OR expires_at IS NULL)");

                    ResultSet punishmentRecord = Database.executeLater(punishmentQuery).get().get();

                    debug.print("Checking database for outstanding punishments...");

                    // So basically, we need to go in this order for punishments
                    // IP Ban check > Normal ban check > Warning check
                    // If the first result in the resultset isn't an IP Ban, we want to ignore it
                    // until we've found an IP Ban
                    while (punishmentRecord.next()) {

                        if (punishmentRecord.getBoolean("appealed")) {
                            continue;
                        }

                        PunishmentType type = PunishmentType.fromOrdinal(punishmentRecord.getInt("type"));

                        if (type == PunishmentType.REGEX) {
                            debug.print(punishmentRecord.getString("regex") + " " + user.getName());
                        }

                        Map<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                            {
                                put("punishid", punishmentRecord.getString("punish_id"));
                                put("ipaddress", player.getAddress().getAddress().getHostAddress().toString());
                                put("player", user.getName());
                                put("reason", punishmentRecord.getString("reason"));
                                put("arbiter", punishmentRecord.getString("punished_by_name"));
                                Timestamp expiresAt = punishmentRecord.getTimestamp("expires_at");
                                put("expiry", expiresAt != null ? expiresAt.toString() : "Never");
                                put("regex", punishmentRecord.getString("regex"));
                            }
                        };

                        // We want to check for ip bans first.
                        if (!foundIP && type == PunishmentType.IP
                                && punishmentRecord.getString("target_ip_address")
                                        .equals(player.getAddress().getAddress().getHostAddress().toString())) {
                        debug.print("Found a valid IP ban - disconnecting...");
                            foundIP = true;

                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate(
                                    punishmentRecord.getTimestamp("expires_at") != null ? "ip-ban.temp-ip-ban-message"
                                            : "ip-ban.perm-ip-ban-message",
                                    vars);
                                    user.disconnect(disconnectReason);

                            punishmentRecord.beforeFirst();
                            continue;
                        }

                        if (!foundRegex
                                && type == PunishmentType.REGEX
                                && Pattern.matches(punishmentRecord.getString("regex") == null ? ""
                                        : punishmentRecord.getString("regex"), user.getName())) {
                            debug.print("Found a valid and matching regex ban - disconnecting...");
                                    
                            foundRegex = true;
                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate(punishmentRecord.getTimestamp("expires_at") != null ? "regex-ban.temp-ban-message" : "regex-ban.perm-ban-message", vars);
                            user.disconnect(disconnectReason);
                        }

                        if (punishmentRecord.getString("target_uuid") == null    
                            || !punishmentRecord.getString("target_uuid").equals(user.getUniqueId().toString())) {
                            continue;
                        }

                        if (!foundBan
                                && type == PunishmentType.BAN) {
                            debug.print("Found a valid ban - disconnecting...");
                            foundBan = true;

                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate(
                                    punishmentRecord.getTimestamp("expires_at") != null ? "ban.temp-ban-message"
                                            : "ban.perm-ban-message",
                                    vars);
                                    user.disconnect(disconnectReason);
                            
                            punishmentRecord.beforeFirst();

                            continue;
                        }
                        
                        if (!foundWarn
                                && type == PunishmentType.WARN
                                && !punishmentRecord.getBoolean("warning_ack")) {
                            debug.print("Found a valid and unacknowledged warn - disconnecting...");
                            foundWarn = true;

                            String disconnectReason = LolBans.getPlugin().getLocaleProvider().translate("warn.warn-kick-message", vars);
                            user.disconnect(disconnectReason);

                            punishmentRecord.beforeFirst();
                            continue;
                        }

                        debug.print("Caching user punishments...");

                        // Let's cache these!
                        switch (type) {
                            case IP:
                                LolBans.getPlugin().getPunishmentCache()
                                        .put(
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
                                LolBans.getPlugin().getPunishmentCache().put(new Punishment(type,
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
                                
                                warn.setWarningAck(punishmentRecord.getBoolean("warning_ack"));
                                    
                                if (foundWarn) {
                                    debug.print("Marking outstanding warning as acknowledged");
                                    warn.setWarningAck(true);
                                    warn.update();
                                } 
                                else 
                                    LolBans.getPlugin().getPunishmentCache().update(warn);
                                break;
                            default:
                                break;
                        }
                    }
                    
                    // bork bork nom nom
                    if (foundBan || foundIP || foundRegex || foundWarn)
                        return false;

                    String rDNS = IPUtil.rDNSQUery(player.getAddress().getAddress().getHostAddress().toString());
                    // Do Regex matches since they're pre-compiled
                    Iterator<?> it = LolBans.getPlugin().REGEX.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<?, ?> pair = (Map.Entry<?, ?>) it.next();
                        Pattern regex = (Pattern) pair.getValue();
                        // Matchers to make things more efficient.
                        Matcher nameMatcher = regex.matcher(user.getName());
                        Matcher ipMatcher = regex.matcher(player.getAddress().getAddress().getHostAddress().toString());
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
                                LolBans.getPlugin().REGEX.remove(pair.getKey()); // The ban doesn't exist in the database, don't
                                                                     // keep
                                                                     // it around
                                continue; // Skip this iteration, go to the next
                            }

                            Timestamp expiresAt = result.getTimestamp("expires_at");
                            Boolean appealed = result.getBoolean("appealed");
                            Map<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {

                                    put("punish_id", punishmentRecord.getString("punish_id"));
                                    put("ipaddress", player.getAddress().getAddress().getHostAddress().toString());
                                    put("target", user.getName());
                                    put("reason", punishmentRecord.getString("reason"));
                                    put("punisher", punishmentRecord.getString("punisher_name"));
                                    put("rdns", rDNS);
                                    put("regex", regex.pattern());
                                    put("expiry", expiresAt != null ? expiresAt.toString() : "");
                                }
                            };

                            if (!appealed) {
                                user.disconnect(LolBans.getPlugin().getLocaleProvider().translate(
                                        expiresAt != null ? "regex-ban.temp-ban-message" : "regex-ban.perm-ban-message",
                                        vars));
                            }
                        }
                    }

                    // Check to make sure they're not an ALT account
                    Future<UUID> AltRecords = IPUtil.checkAlts(player.getAddress().getAddress());
                    UUID altaccount = AltRecords.get();
                    if (altaccount != null) {
                        // OfflinePlayer p = Bukkit.getOfflinePlayer(altaccount);
                        User user = LolBans.getPlugin().getUser(altaccount);
                        // Send a message to all ops with broadcast perms.
                        String message = LolBans.getPlugin().getLocaleProvider().translate("ip-ban.ip-alt-notification",
                                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                    {
                                        put("player", player.getName());
                                        put("bannedplayer", user.getName());
                                        put("IPADDRESS", player.getAddress().getAddress().getHostAddress().toString());
                                    }
                                });

                        LolBans.getPlugin().notifyStaff(message);
                        self.getLogger().warning(message);

                        if (self.getConfig().getBoolean("ip-ban-settings.kick-alt-accounts", false)) {
                            user.disconnect(LolBans.getPlugin().getLocaleProvider().translate("ip-ban.ip-alt-ban-message",
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
                    user.disconnect(LolBans.getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
                }
                return true;
            }
        });

        LolBans.getPlugin().getPool().execute(t);

        try {
            if (!t.get())
                return;
        } catch (InterruptedException | ExecutionException e) {
            if (self.getConfig().getBoolean("general.kick-connection-error"))
                user.disconnect(LolBans.getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
            e.printStackTrace();
        }
        LolBans.getPlugin().registerUser(user);
        debug.print("PostLoginEvent complete");
    }

    @EventHandler
    public void onKick(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        LolBans.getPlugin().removeUser(player);
        try {
            if (!(Database.updateUser(player.getUniqueId().toString(), player.getName(), player.getAddress().getAddress().getHostAddress(), new Timestamp(System.currentTimeMillis())).get()))
                LolBans.getLogger().severe(LolBans.getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}