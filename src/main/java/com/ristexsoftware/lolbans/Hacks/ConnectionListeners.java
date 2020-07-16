/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Hacks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.IPUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class ConnectionListeners implements Listener {
    private static Main self = Main.getPlugin(Main.class);
    // private static HashMap<UUID, String> LinkMessages = null;

    /************************************************************************************
     * Our event listeners.
     */

    // I changed this this PlayerLoginEvent instead of PlayerPreLoginEvent because
    // you're actually suppose to do ban checks and whatnot here
    // And I needed the Player object to do permission checking
    @EventHandler
    public static void OnPlayerConnectAsync(PlayerLoginEvent event) {
        // Since PlayerLogineEvent is not asynchronous, we need to run this on a new
        // thread, we don't want to make the main thread wait for this to complete
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Player player = event.getPlayer();
                    Timestamp login = TimeUtil.TimestampNow();
                    if (User.getLastIP(player.getUniqueId().toString()).get() == null) {
                        DatabaseUtil.InsertUser(player.getUniqueId().toString(), player.getName(),
                                event.getAddress().getHostAddress().toString(), login, login);
                        Thread.sleep(100);
                    }
                    // Before we do anything, make sure they're abiding the ratelimit and are not
                    // coming from a new IP range
                    if (self.getConfig().getBoolean("Connection.RateLimiting.Enabled")
                            && !player.hasPermission("lolbans.ratelimit.bypass")) {
                        Integer limit = self.getConfig().getInt("Connection.RateLimiting.Limit");
                        if (limit <= 0)
                            limit = 6;
                        Timestamp lastLogin = User.getLastLogin(player.getUniqueId().toString()).get();
                        Timestamp limitStamp = new Timestamp(TimeUtil.GetUnixTime() * 1000L + limit);
                        if (lastLogin.getTime() + (limit * 1000) >= limitStamp.getTime()) {
                            final Integer ihatejava = limit;
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("ipaddress", event.getAddress().toString());
                                    put("player", player.getName());
                                    put("rate", String.valueOf(
                                            ihatejava - ((limitStamp.getTime() - lastLogin.getTime()) / 1000)));
                                }
                            };
                            event.disallow(Result.KICK_OTHER, Messages.Translate("RateLimit.LimitReached", Variables));
                            return true; // Lets return so we don't update the login time!
                        }
                    }
                    if (self.getConfig().getBoolean("Connection.IPCheck.Enabled")
                            && !player.hasPermission("lolbans.ipcheck.bypass")) {
                        Integer prefix = self.getConfig().getInt("Connection.IPCheck.Prefix");
                        // Make sure it's a valid CIDR prefix
                        if (prefix < 1 || prefix > 30)
                            prefix = 23;
                        if (!IPUtil.checkRange(User.getLastIP(player.getUniqueId().toString()).get(),
                                event.getAddress().getHostAddress().toString(), String.valueOf(prefix))) {
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("ipaddress", event.getAddress().toString());
                                    put("player", player.getName());
                                }
                            };
                            event.disallow(Result.KICK_OTHER, Messages.Translate("IPCheck.InvalidCIDR", Variables));
                            return true;
                        }
                    }

                    // lets do this too
                    // InsertUser has a check to see if they've joined before, so it's safe to use
                    // as the update event aswell.
                    DatabaseUtil.InsertUser(player.getUniqueId().toString(), player.getName(),
                            event.getAddress().getHostAddress().toString(), login, login);
                    // To save time, we do a few things here:
                    // 1. We execute 3 queries:
                    // 1st query: Get whether the player is banned.
                    // 2nd query: Get whether the player has a pending warning.
                    // 3rd query: Compare the IP to banned IPs
                    // 2. While those two queries are executing, we also check them
                    // against any CIDR-based IP bans we have.
                    // 3. If they don't match any CIDR bans, hopefully our queries
                    // are done and we can check if they have anything.
                    // 4. If they don't match any queries, we let them join.

                    // Ask the database for any ban records
                    PreparedStatement BanStatement = self.connection.prepareStatement(
                            "SELECT * FROM lolbans_punishments WHERE UUID = ? AND Type = ? AND Appealed = FALSE OR (Expiry IS NOT NULL >= NOW())");
                    BanStatement.setString(1, player.getUniqueId().toString());
                    BanStatement.setInt(2, PunishmentType.PUNISH_BAN.ordinal());

                    // Also ask for any warning records
                    PreparedStatement WarnStatement = self.connection.prepareStatement(
                            "SELECT * FROM lolbans_punishments WHERE UUID = ? AND Type = ? AND WarningAck = ?");
                    WarnStatement.setString(1, player.getUniqueId().toString());
                    WarnStatement.setInt(2, PunishmentType.PUNISH_WARN.ordinal());
                    WarnStatement.setBoolean(3, false);

                    // Also ask to see if we have any linked account confirmations waiting.
                    // PreparedStatement LinkedStatement = self.connection.prepareStatement("SELECT
                    // * FROM LinkConfirmations WHERE UUID = ? AND Expiry >= NOW()");
                    // LinkedStatement.setString(1, event.getUniqueId().toString());

                    // Send off to other threads to query the database while we do other things.
                    Future<Optional<ResultSet>> BanRecord = DatabaseUtil.ExecuteLater(BanStatement);
                    Future<Optional<ResultSet>> WarnRecord = DatabaseUtil.ExecuteLater(WarnStatement);
                    // Future<Optional<ResultSet>> LinkedRecord =
                    // DatabaseUtil.ExecuteLater(LinkedStatement);
                    Future<Optional<ResultSet>> IPBanRecord = IPUtil.IsBanned(event.getAddress());
                    Future<UUID> AltRecords = IPUtil.CheckAlts(event.getAddress());

                    // Query for their reverse DNS hostname
                    String rDNS = IPUtil.rDNSQUery(event.getAddress().getHostAddress());

                    // While we run those queries, lets check to see if they match any CIDRs
                    // NOTE: We don't immediately disconnect them from here, instead we make
                    // note that they match a CIDR range and should be kicked in general. We
                    // format the kick message for a CIDR ban but a UUID ban supercedes an IP
                    // ban as those are explicit bans on users and not ranged bans.

                    String IPBanMessage = null;
                    Optional<ResultSet> IPBanResult = IPBanRecord.get();
                    if (IPBanResult.isPresent()) {
                        ResultSet result = IPBanResult.get();
                        if (result.next()) {
                            Timestamp Expiry = result.getTimestamp("Expiry");
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("IPADDRESS", event.getAddress().toString());
                                    put("player", player.getName());
                                    put("reason", result.getString("Reason"));
                                    put("arbiter", result.getString("ArbiterName"));
                                    put("expiry", Expiry != null ? Expiry.toString() : "Never");
                                    put("punishid", result.getString("PunishID"));
                                }
                            };

                            IPBanMessage = Messages.Translate(
                                    Expiry != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage", Variables);
                        }
                    }

                    // Do Regex matches since they're pre-compiled
                    Iterator<?> it = Main.REGEX.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<?, ?> pair = (Map.Entry<?, ?>) it.next();
                        Pattern regex = (Pattern) pair.getValue();
                        // Matchers to make things more efficient.
                        Matcher NameMatch = regex.matcher(player.getName());
                        Matcher IPMatch = regex.matcher(event.getAddress().getHostAddress());
                        Matcher HostMatch = regex.matcher(rDNS);

                        // If any of them match, we must query the database for the record
                        // then disconnect them for matching something.
                        if (NameMatch.find() || IPMatch.find() || HostMatch.find()) {
                            // FIXME: AND (Expiry IS NULL OR Expiry >= NOW()) -- how do we handle expired
                            // regex bans?
                            PreparedStatement ps = self.connection
                                    .prepareStatement("SELECT * FROM lolbans_regexbans WHERE id = ?"); // AND Appealed =
                                                                                                       // FALSE OR
                                                                                                       // (Expiry IS NOT
                                                                                                       // NULL >= NOW())
                            ps.setInt(1, (Integer) pair.getKey());
                            ResultSet result = ps.executeQuery();

                            System.out.println((Integer) pair.getKey());
                            // Something's fucked? lets make note.
                            if (!result.next()) {
                                // throw new SQLException("No such regex " + regex.pattern());
                                self.getLogger().info("No such regex: " + regex.pattern()); // Log the issue
                                Main.REGEX.remove(pair.getKey()); // The ban doesn't exist in the database, don't keep
                                                                  // it around
                                continue; // Skip this iteration, go to the next
                            }

                            Timestamp Expiry = result.getTimestamp("Expiry");
                            Boolean Appealed = result.getBoolean("Appealed");
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("IPADDRESS", event.getAddress().toString());
                                    put("player", player.getName());
                                    put("rdns", rDNS);
                                    put("regex", regex.pattern());
                                    put("reason", result.getString("Reason"));
                                    put("arbiter", result.getString("ArbiterName"));
                                    put("expiry", Expiry != null ? Expiry.toString() : "");
                                    put("punishid", result.getString("PunishID"));
                                }
                            };

                            // We'll commondeer the IPBanMessage variable for regex bans too.
                            if (!Appealed) // re: how to handle expired regex bans: Just leave this null if it's
                                           // expired/appealed, that way they don't get kicked.
                                IPBanMessage = Messages.Translate(
                                        Expiry != null ? "RegexBan.TempBanMessage" : "RegexBan.PermBanMessage",
                                        Variables);
                        }
                    }

                    // NOTICE: We grab their linked account ID before we ban them (IP Bans don't
                    // matter though)
                    // so we can kick with their linked account ID as part of the ban message to
                    // confirm their UUID.
                    // As such we must grab their link id before we process kicks for bans and the
                    // like.
                    // Optional<ResultSet> LinkedResult = LinkedRecord.get();
                    // String TempMsg = null;
                    // if (LinkedResult.isPresent())
                    // {
                    // ResultSet result = LinkedResult.get();
                    // if (result.next())
                    // {
                    // TempMsg = Messages.Translate("Link.LinkedAccountMessage",
                    // new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    // {{
                    // put("LinkID", result.getString("LinkID"));
                    // }});
                    // }
                    // }

                    // final String LinkedAccountMessage = TempMsg;

                    // Now we wait for the ban record to return
                    Optional<ResultSet> BanResult = BanRecord.get();
                    // The query was successful.
                    if (BanResult.isPresent()) {
                        ResultSet result = BanResult.get();
                        // They're banned. Disconnect now.
                        if (result.next()) {
                            Timestamp BanTime = result.getTimestamp("Expiry");
                            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                {
                                    put("player", player.getName());
                                    put("reason", result.getString("Reason"));
                                    put("arbiter", result.getString("ArbiterName"));
                                    put("TimePunished", result.getTime("TimePunished").toString());
                                    if (BanTime != null)
                                        put("expiry", BanTime.toString());
                                    put("punishid", result.getString("PunishID"));
                                    // put("LinkMessage", LinkedAccountMessage);
                                }
                            };
                            // (String message, String ColorChars, Map<String, String> Variables)
                            event.disallow(Result.KICK_BANNED, Messages.Translate(
                                    BanTime != null ? "Ban.TempBanMessage" : "Ban.PermBanMessage", Variables));
                            return true;
                        }
                    }

                    // At this point if we have an IP ban, the IP ban supercedes warnings but UUID
                    // bans supercede IP bans
                    // If there is an IP ban, disconnect them for that and avoid running the warning
                    // code below.
                    if (IPBanMessage != null) {
                        event.disallow(Result.KICK_BANNED, IPBanMessage);
                        return true;
                    }

                    // Check to make sure they don't have any pending and unacknowledged warnings.
                    Optional<ResultSet> WarnResult = WarnRecord.get();
                    if (WarnResult.isPresent()) {
                        ResultSet result = WarnResult.get();
                        if (result.next()) {
                            String WarnKickMessage = Messages.Translate("Warn.WarnKickMessage",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                        {
                                            put("player", result.getString("PlayerName"));
                                            put("reason", result.getString("Reason"));
                                            put("arbiter", result.getString("ArbiterName"));
                                            put("punishid", result.getString("PunishID"));
                                        }
                                    });

                            // 👋
                            event.disallow(Result.KICK_OTHER, WarnKickMessage);

                            // Now accept the warning
                            PreparedStatement pst3 = self.connection.prepareStatement(
                                    "UPDATE lolbans_punishments SET WarningAck = true WHERE UUID = ?");
                            pst3.setString(1, player.getUniqueId().toString());
                            pst3.executeUpdate();
                        }
                    }

                    // Check to make sure they're not an ALT account
                    UUID altaccount = AltRecords.get();
                    if (altaccount != null) {
                        OfflinePlayer p = Bukkit.getOfflinePlayer(altaccount);
                        // Send a message to all ops with broadcast perms.
                        String Message = Messages.Translate("IPBan.IPAltNotification",
                                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                    {
                                        put("player", player.getName());
                                        put("bannedplayer", p.getName());
                                        put("IPADDRESS", event.getAddress().getHostAddress());
                                    }
                                });

                        BroadcastUtil.BroadcastOps(Message);
                        self.getLogger().warning(Message);

                        if (self.getConfig().getBoolean("IPBanSettings.KickAltAccounts", false)) {
                            event.disallow(Result.KICK_BANNED, Messages.Translate("IPBan.IPAltBanMessage",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                                        {
                                            // TODO: This!
                                            put("PLAYERNAME", player.getName());
                                            put("ALTACCOUNT", p.getUniqueId().toString());

                                            put("arbiter", "");
                                            put("REASON", "");
                                            put("EXPIRYDURATION", "");
                                            put("PUNISHID", "");
                                        }
                                    }));
                        }

                        // TODO: Send to discord?
                    }

                    // if (LinkMessages == null)
                    // LinkMessages = new HashMap<UUID, String>();

                    // LinkMessages.put(event.getUniqueId(), LinkedAccountMessage);

                    // They're not banned and have no pending warnings, allow them to connect or
                    // other plugins to perform their actions.
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // Kick if there was a server error.
                    if (self.getConfig().getBoolean("General.KickConnectionError"))
                        event.disallow(Result.KICK_FULL, Messages.ServerError);
                    return false;
                }
                return true;
            }
        });
        Main.pool.execute(t);
        try {
            if (!t.get()) {
                if (self.getConfig().getBoolean("General.KickConnectionError"))
                    event.disallow(Result.KICK_FULL, Messages.ServerError);
            }
        } catch (InterruptedException | ExecutionException e) {
            if (self.getConfig().getBoolean("General.KickConnectionError"))
                event.disallow(Result.KICK_FULL, Messages.ServerError);
            e.printStackTrace();
        }
    }

    @EventHandler
    public static void OnPlayerDisconnect(PlayerQuitEvent event) {
        DatabaseUtil.UpdateUser(event.getPlayer().getUniqueId().toString(), event.getPlayer().getName(),
                event.getPlayer().getAddress().getAddress().getHostAddress(), TimeUtil.TimestampNow());
        UUID PlayerUUID = event.getPlayer().getUniqueId();
        Main.USERS.remove(PlayerUUID);
    }

    @EventHandler
    public static void OnPlayerKick(PlayerKickEvent event) {
        DatabaseUtil.UpdateUser(event.getPlayer().getUniqueId().toString(), event.getPlayer().getName(),
                event.getPlayer().getAddress().getAddress().getHostAddress(), TimeUtil.TimestampNow());
        Main.USERS.remove(event.getPlayer().getUniqueId());
    }
}