package com.ristexsoftware.lolbans.Hacks;

import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.IPBanUtil;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.User;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
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

    /************************************************************************************
     * Convenience Functions
     */

    /************************************************************************************
     * Our event listeners.
     */

    // Adding players to a hashmap and account linking
    public static void OnPlayerConnect(PlayerJoinEvent event) 
    {
        if (LinkMessages == null)
            LinkMessages = new HashMap<UUID, String>();

        Player player = event.getPlayer();
        String puuid = player.getUniqueId().toString();
        String ipaddr = player.getAddress().getAddress().getHostAddress();

        Main.USERS.put(player.getUniqueId(), new User(player));

        try 
        {
            if (!player.hasPlayedBefore())
            {
                Timestamp firstjoin = TimeUtil.TimestampNow();
                DatabaseUtil.InsertUser(puuid, player.getName(), ipaddr, firstjoin, firstjoin);
            }
            else
            {
                Timestamp lastjoin = TimeUtil.TimestampNow();
                DatabaseUtil.UpdateUser(lastjoin, player.getName(), ipaddr, puuid);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        // Link accounts via the website
        String JoinMessage = LinkMessages.get(player.getUniqueId());
        if (JoinMessage != null)
            player.sendMessage(JoinMessage);
    }

    // We need to make this async so the database stuff doesn't run on the main
    // thread.
    // This event is already async, no need.
    public static void OnPlayerConnectAsync(AsyncPlayerPreLoginEvent event) 
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

            // Ask the database for any ban records
            PreparedStatement BanStatement = self.connection.prepareStatement("SELECT * FROM Punishments WHERE UUID = ? AND Type = ? AND Appealed = FALSE OR (Expiry IS NOT NULL AND Expiry >= NOW())");
            BanStatement.setString(1, event.getUniqueId().toString());
            BanStatement.setInt(2, PunishmentType.PUNISH_BAN.ordinal());

            // Also ask for any warning records
            PreparedStatement WarnStatement = self.connection.prepareStatement("SELECT * FROM Punishments WHERE UUID = ? AND Type = ? AND WarningAck = ?");
            WarnStatement.setString(1, event.getUniqueId().toString());
            WarnStatement.setInt(2, PunishmentType.PUNISH_WARN.ordinal());
            WarnStatement.setBoolean(3, false);

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
                        put("arbiter", result.getString("ArbiterName"));
                        put("expiry", Expiry != null ? Expiry.toString() : "Never");
                        put("punishid", result.getString("PunishID"));
                    }};

                    IPBanMessage = Messages.Translate(Expiry != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage", Variables);
                }
            }

            // Do Regex matches since they're pre-compiled
            Iterator<?> it = Main.REGEX.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<?, ?> pair = (Map.Entry<?, ?>)it.next();
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
                        put("arbiter", result.getString("ArbiterName"));
                        put("expiry", Expiry != null ? Expiry.toString() : "Never");
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
                        put("arbiter", result.getString("ArbiterName"));
                        put("expiry", BanTime != null ? BanTime.toString() : "Never");
                        put("punishid", result.getString("PunishID"));
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
                            put("arbiter", result.getString("ArbiterName"));
                            put("punishid", result.getString("PunishID"));
                        }}
                    );
                    
                    // 👋
                    event.disallow(Result.KICK_OTHER, WarnKickMessage);

                    // Now accept the warning
                    PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Punishments SET WarningAck = true WHERE UUID = ?");
                    pst3.setString(1, event.getUniqueId().toString());
                    pst3.executeUpdate();
                }
            }

            // Check to make sure they're not an ALT account
            UUID altaccount = AltRecords.get();
            if (altaccount != null)
            {
                OfflinePlayer p = Bukkit.getOfflinePlayer(altaccount);
                // Send a message to all ops with broadcast perms.
                String Message = Messages.Translate("IPBan.IPAltNotification", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) 
                    {{
                        put("player", event.getName());
                        put("bannedplayer", p.getName());
                        put("IPADDRESS", event.getAddress().getHostAddress());
                    }}
                );

                BroadcastUtil.BroadcastOps(Message);
                self.getLogger().warning(Message);

                if (self.getConfig().getBoolean("IPBanSettings.KickAltAccounts", false))
                {
                    event.disallow(Result.KICK_BANNED, Messages.Translate("IPBan.IPAltBanMessage",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            // TODO: This!
                            put("PLAYERNAME", event.getName());
                            put("ALTACCOUNT", p.getUniqueId().toString());

                            put("arbiter", "");
                            put("REASON", "");
                            put("EXPIRYDURATION", "");
                            put("PUNISHID", "");
                        }}
                    ));
                }

                // TODO: Send to discord?
                if (DiscordUtil.UseSimplifiedMessage)
                {
                    DiscordUtil.SendFormatted(Messages.Translate("Discord.KickedAltAccount", 
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("", "");
                        }}
                    ));
                }
                else
                {

                }
            }

            if (LinkMessages == null)
                LinkMessages = new HashMap<UUID, String>();

            LinkMessages.put(event.getUniqueId(), LinkedAccountMessage);

            // They're not banned and have no pending warnings, allow them to connect or other plugins to perform their actions.
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            // Kick if there was a server error.
            if (self.getConfig().getBoolean("General.KickConnectionError"))
                event.disallow(Result.KICK_FULL, Messages.ServerError);
        }
    }

    public static void OnPlayerDisconnect(PlayerQuitEvent event) 
    {
        UUID PlayerUUID = event.getPlayer().getUniqueId();
        Main.USERS.remove(PlayerUUID);
    }

    public static void OnPlayerKick(PlayerKickEvent event) 
    {
        Main.USERS.remove(event.getPlayer().getUniqueId());
    }
}