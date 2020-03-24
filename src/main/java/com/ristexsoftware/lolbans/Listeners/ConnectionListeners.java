package com.ristexsoftware.lolbans.Listeners;

import inet.ipaddr.IPAddressString;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ristexsoftware.lolbans.IPBanning.IPBanUtil;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.TranslationUtil;
import com.ristexsoftware.lolbans.Utils.User;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class ConnectionListeners implements Listener 
{
    private static Main self = Main.getPlugin(Main.class);

    private static HashMap<UUID, String> LinkMessages = null;

    public ConnectionListeners()
    {
        LinkMessages = new HashMap<UUID, String>();
    }

    /************************************************************************************
     * Convenience Functions
     */

    /************************************************************************************
     * Our event listeners.
     */

     // What are you doing here? I'm confused by it, I wont touch it.
    @EventHandler
    public void OnPlayerConnect(PlayerJoinEvent event) 
    {
        Main.USERS.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));

        // Link accounts via the website
        String JoinMessage = LinkMessages.get(event.getPlayer().getUniqueId());
        if (JoinMessage != null)
            event.getPlayer().sendMessage(JoinMessage);
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        String puuid = player.getUniqueId().toString();
        String ipaddr = player.getAddress().getAddress().getHostAddress();
        if (player.hasPlayedBefore())
        {
            Timestamp firstjoin = TimeUtil.TimestampNow();
            try 
            {
                int i = 1;
                PreparedStatement ps = self.connection.prepareStatement("INSERT INTO Users (UUID, PlayerName, IPAddress, FirstLogin, LastLogin) VALUES (?, ?, ?, ?, ?)");
                ps.setString(i++, puuid);
                ps.setString(i++, player.getName());
                ps.setString(i++, ipaddr);
                ps.setTimestamp(i++, firstjoin);
                ps.setTimestamp(i++, firstjoin);

                DatabaseUtil.ExecuteLater(ps);
            }
            catch (SQLException ex)
            {
                ex.printStackTrace();
            }
        }
        else
        {
            Timestamp lastjoin = TimeUtil.TimestampNow();
            try 
            {
                int i = 1;
                PreparedStatement ps = self.connection.prepareStatement("UPDATE Users SET LastLogin = ? WHERE UUID = ?");
                ps.setString(i++, player.getName());
                ps.setString(i++, ipaddr);
                ps.setTimestamp(i++, lastjoin);
                ps.setString(i++, puuid);

                DatabaseUtil.ExecuteLater(ps);
            }
            catch (SQLException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    // We need to make this async so the database stuff doesn't run on the main
    // thread.
    @EventHandler
    public void OnPlayerConnectAsync(AsyncPlayerPreLoginEvent event) 
    {
        try 
        {
            // To save time, we do a few things here:
            // 1. We execute 3 queries:
            //      1st query: Get whether the player is banned.
            //      2nd query: Get whether the player has a pending warning.
            //      3rd query: Compare the IP to banned IPs
            // 2. While those two queries are executing, we also check them
            //    against any CIDR-based IP bans we have.
            // 3. If they don't match any CIDR bans, hopefully our queries
            //    are done and we can check if they have anything.
            // 4. If they don't match any queries, we let them join.

            // Here's the nonsense for adding the user info into the database, move this where ever ig...
            String player = event.getName();
            String addr = event.getAddress().getHostAddress();

            // Ask the database for any ban records
            PreparedStatement BanStatement = self.connection.prepareStatement("SELECT * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NULL OR Expiry >= NOW())");
            BanStatement.setString(1, event.getUniqueId().toString());

            // Also ask for any warning records
            PreparedStatement WarnStatement = self.connection.prepareStatement("SELECT * FROM Warnings WHERE UUID = ? AND Accepted = ?");
            WarnStatement.setString(1, event.getUniqueId().toString());
            WarnStatement.setBoolean(2, false);

            // Also ask to see if we have any linked account confirmations waiting.
            PreparedStatement LinkedStatement = self.connection.prepareStatement("SELECT * FROM LinkConfirmations WHERE UUID = ? AND Expiry >= NOW()");
            LinkedStatement.setString(1, event.getUniqueId().toString());

            // Send off to other threads to query the database while we do other things.
            Future<Optional<ResultSet>> BanRecord = DatabaseUtil.ExecuteLater(BanStatement);
            Future<Optional<ResultSet>> WarnRecord = DatabaseUtil.ExecuteLater(WarnStatement);
            Future<Optional<ResultSet>> LinkedRecord = DatabaseUtil.ExecuteLater(LinkedStatement);
            Future<Optional<ResultSet>> IPBanRecord = IPBanUtil.IsBanned(event.getAddress());
            Future<UUID> AltRecords  = IPBanUtil.CheckAlts(event.getAddress());

            // Query for their reverse DNS hostname
            String rDNS = IPBanUtil.rDNSQUery(event.getAddress().getHostAddress());

            // While we run those queries, lets check to see if they match any CIDRs
            // NOTE: We don't immediately disconnect them from here, instead we make
            // note that they match a CIDR range and should be kicked in general. We
            // format the kick message for a CIDR ban but a UUID ban supercedes an IP
            // ban as those are explicit bans on users and not ranged bans.

            String IPBanMessage = null;
            Optional<ResultSet> IPBanResult = IPBanRecord.get();
            if (IPBanResult.isPresent())
            {
                ResultSet result = IPBanResult.get();
                if (result.next())
                {
                    Timestamp Expiry = result.getTimestamp("Expiry");
                    Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("IPADDRESS", event.getAddress().toString());
                        put("player", event.getName());
                        put("reason", result.getString("Reason"));
                        put("banner", result.getString("Executioner"));
                        put("fullexpiry", Expiry != null ? String.format("%s (%s)", TimeUtil.TimeString(Expiry), TimeUtil.Expires(Expiry)) : "Never");
                        put("expiryduration", Expiry != null ? TimeUtil.Expires(Expiry) : "Never");
                        put("dateexpiry", Expiry != null ? TimeUtil.TimeString(Expiry) : "Never");
                        put("banid", result.getString("PunishID"));
                    }};

                    IPBanMessage = Messages.Translate(Expiry != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage", Variables);
                }
            }

            // Do Regex matches since they're pre-compiled
            Iterator it = self.REGEX.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry)it.next();
                Pattern regex = (Pattern)pair.getValue();
                // Matchers to make things more efficient.
                Matcher NameMatch = regex.matcher(event.getName());
                Matcher IPMatch = regex.matcher(event.getAddress().getHostAddress());
                Matcher HostMatch = regex.matcher(rDNS);

                // If any of them match, we must query the database for the record
                // then disconnect them for matching something.
                if (NameMatch.matches() || IPMatch.matches() || HostMatch.matches())
                {
                    // FIXME: AND (Expiry IS NULL OR Expiry >= NOW()) -- how do we handle expired regex bans?
                    PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM RegexBans WHERE id = ?");
                    ps.setInt(1, (Integer)pair.getKey());
                    ResultSet result = ps.executeQuery();

                    // Something's fucked? lets make note.
                    if (!result.next())
                        throw new SQLException("No such regex " + regex.pattern());

                    Timestamp Expiry = result.getTimestamp("Expiry");
                    Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("IPADDRESS", event.getAddress().toString());
                        put("player", event.getName());
                        put("rdns", rDNS);
                        put("regex", regex.pattern());
                        put("reason", result.getString("Reason"));
                        put("banner", result.getString("Executioner"));
                        put("fullexpiry", Expiry != null ? String.format("%s (%s)", TimeUtil.TimeString(Expiry), TimeUtil.Expires(Expiry)) : "Never");
                        put("expiryduration", Expiry != null ? TimeUtil.Expires(Expiry) : "Never");
                        put("dateexpiry", Expiry != null ? TimeUtil.TimeString(Expiry) : "Never");
                        put("punishid", result.getString("PunishID"));
                    }};

                    // We'll commondeer the IPBanMessage variable for regex bans too.
                    IPBanMessage = Messages.Translate(Expiry != null ? "RegexBan.TempBanMessage" : "RegexBan.PermBanMessage", Variables);
                }
            }

            // NOTICE: We grab their linked account ID before we ban them (IP Bans don't matter though)
            // so we can kick with their linked account ID as part of the ban message to confirm their UUID.
            // As such we must grab their link id before we process kicks for bans and the like.
            Optional<ResultSet> LinkedResult = LinkedRecord.get();
            String TempMsg = null;
            if (LinkedResult.isPresent())
            {
                ResultSet result = LinkedResult.get();
                if (result.next())
                {
                    TempMsg = Messages.Translate("Link.LinkedAccountMessage",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("LinkID", result.getString("LinkID"));
                    }});
                }
            }

            final String LinkedAccountMessage = TempMsg;

            // Now we wait for the ban record to return
            Optional<ResultSet> BanResult = BanRecord.get();
            // The query was successful.
            if (BanResult.isPresent())
            {
                ResultSet result = BanResult.get();
                // They're banned. Disconnect now.
                if (result.next())
                {
                    Timestamp BanTime = result.getTimestamp("Expiry");
                    Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", event.getName());
                        put("reason", result.getString("Reason"));
                        put("banner", result.getString("Executioner"));
                        put("fullexpiry", BanTime != null ? String.format("%s (%s)", TimeUtil.TimeString(BanTime), TimeUtil.Expires(BanTime)) : "Never");
                        put("expiryduration", BanTime != null ? TimeUtil.Expires(BanTime) : "Never");
                        put("dateexpiry", BanTime != null ? TimeUtil.TimeString(BanTime) : "Never");
                        put("banid", result.getString("PunishID"));
                        put("LinkMessage", LinkedAccountMessage);
                    }};
                    //(String message, String ColorChars, Map<String, String> Variables)
                    event.disallow(Result.KICK_BANNED, Messages.Translate(BanTime != null ? "Ban.TempBanMessage" : "Ban.PermBanMessage", Variables));
                    return;
                }
            }


            // At this point if we have an IP ban, the IP ban supercedes warnings but UUID bans supercede IP bans
            // If there is an IP ban, disconnect them for that and avoid running the warning code below.
            if (IPBanMessage != null)
            {
                event.disallow(Result.KICK_BANNED, IPBanMessage);
                return;
            }


            // Check to make sure they don't have any pending and unacknowledged warnings.
            Optional<ResultSet> WarnResult = WarnRecord.get();
            if (WarnResult.isPresent())
            {
                ResultSet result = WarnResult.get();
                if (result.next())
                {
                    String WarnKickMessage = Messages.Translate("Warn.WarnKickMessage",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", result.getString("PlayerName"));
                            put("reason", result.getString("Reason"));
                            put("issuer", result.getString("Executioner"));
                            put("warnid", result.getString("PunishID"));
                        }}
                    );
                    
                    // 👋
                    event.disallow(Result.KICK_OTHER, WarnKickMessage);

                    // Now accept the warning
                    PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Warnings SET Accepted = true WHERE UUID = ?");
                    pst3.setString(1, event.getUniqueId().toString());
                    pst3.executeUpdate();
                }
            }

            // Check to make sure they're not an ALT account
            // TODO: Add config option to kick/ban alts
            UUID altaccount = AltRecords.get();
            if (altaccount != null)
            {
                OfflinePlayer p = Bukkit.getOfflinePlayer(altaccount);
                // Send a message to all ops with broadcast perms.
                String Message = Messages.Translate("IPBans.IPAltNotification", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) 
                    {{
                        put("player", event.getName());
                        put("bannedplayer", p.getName());
                    }}
                );

                BroadcastUtil.BroadcastOps(Message);
                self.getLogger().warning(Message);
                // TODO: Send to discord?
            }

            LinkMessages.put(event.getUniqueId(), LinkedAccountMessage);

            // They're not banned and have no pending warnings, allow them to connect or other plugins to perform their actions.
        }
        catch (SQLException | InterruptedException | ExecutionException | InvalidConfigurationException ex)
        {
            ex.printStackTrace();
            // Kick if there was a server error.
            if (self.getConfig().getBoolean("General.KickConnectionError"))
                event.disallow(Result.KICK_FULL, Messages.ServerError);
        }
    }

    @EventHandler
    public void OnPlayerDisconnect(PlayerQuitEvent event) 
    {
        UUID PlayerUUID = event.getPlayer().getUniqueId();
        Main.USERS.remove(PlayerUUID);
    }

    @EventHandler
    public void OnPlayerKick(PlayerKickEvent event) 
    {
        Main.USERS.remove(event.getPlayer().getUniqueId());
    }
}