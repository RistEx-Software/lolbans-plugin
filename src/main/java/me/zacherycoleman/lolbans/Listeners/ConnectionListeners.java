package me.zacherycoleman.lolbans.Listeners;

import java.sql.*;

import java.util.TreeMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Utils.Messages;
import me.zacherycoleman.lolbans.Utils.CIDRBan;
import me.zacherycoleman.lolbans.Utils.TimeUtil;
import me.zacherycoleman.lolbans.Utils.TranslationUtil;

public class ConnectionListeners implements Listener 
{
    private static Main self = Main.getPlugin(Main.class);

    @EventHandler
    public void OnPlayerConnect(PlayerJoinEvent event) 
    {
        Main.USERS.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));
    }

    // We need to make this async so the database stuff doesn't run on the main thread.
    @EventHandler
    public void OnPlayerConnectAsync(AsyncPlayerPreLoginEvent event) 
    {
        try 
        {
            // First, check if they're IP banned, this is the easiest check as we've already made
            // queries to ensure this list is up to date.
            for (CIDRBan cb : Main.BannedCIDRs)
            {
                // They're a banned cidr, query for the reason and kick them.
                if (cb.CheckRange(event.getAddress()))
                {
                    PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM IPBans WHERE IPAddress = ? AND CIDR = ?");
                    pst.setBlob(1, cb.GetBlob());
                    pst.setInt(2, cb.GetMask());

                    ResultSet res = pst.executeQuery();

                    Timestamp Expiry = res.getTimestamp("Expiry");
                    Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("IPADDRESS", event.getAddress().toString());
                        put("player", event.getName());
                        put("reason", res.getString("Reason"));
                        put("banner", res.getString("Executioner"));
                        put("fullexpiry", Expiry != null ? String.format("%s (%s)", TimeUtil.TimeString(Expiry), TimeUtil.Expires(Expiry)) : "Never");
                        put("expiryduration", Expiry != null ? TimeUtil.Expires(Expiry) : "Never");
                        put("dateexpiry", Expiry != null ? TimeUtil.TimeString(Expiry) : "Never");
                        put("banid", res.getString("BanID"));
                    }};

                    event.disallow(Result.KICK_BANNED, Messages.GetMessages().Translate(Expiry != null ? "IPBan.IPBanTempMessage" : "IPBan.IPBanPermMessage", Variables));
                    return;
                }
            }

            PreparedStatement pst = self.connection.prepareStatement(
                    "SELECT * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NULL OR Expiry >= NOW())");
            pst.setString(1, event.getUniqueId().toString());
            ResultSet result = pst.executeQuery();

            if (result.next())
            {
                Timestamp BanTime = result.getTimestamp("Expiry");
                String reason = result.getString("Reason");
                String sender = result.getString("Executioner");
                String BanID = result.getString("BanID");

                Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", event.getName());
                    put("reason", reason);
                    put("banner", sender);
                    put("fullexpiry", BanTime != null ? String.format("%s (%s)", TimeUtil.TimeString(BanTime), TimeUtil.Expires(BanTime)) : "Never");
                    put("expiryduration", BanTime != null ? TimeUtil.Expires(BanTime) : "Never");
                    put("dateexpiry", BanTime != null ? TimeUtil.TimeString(BanTime) : "Never");
                    put("banid", BanID);
                }};

                event.disallow(Result.KICK_BANNED, Messages.GetMessages().Translate(BanTime != null ? "Ban.TempBanMessage" : "Ban.PermBanMessage", Variables);
                // Just return and don't execute queries, they're being kicked anyway.
                return;
            }

            // Check to make sure they don't have any pending and unacknowledged warnings.
            PreparedStatement pst2 = self.connection.prepareStatement("SELECT * FROM Warnings WHERE UUID = ? AND Accepted = ?");
            pst2.setString(1, event.getUniqueId().toString());
            pst2.setBoolean(2, false);

            ResultSet result2 = pst2.executeQuery();
            if (result2.next()) 
            {
                // Set that their warning was acknowledged 
                PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Warnings SET Accepted = true WHERE UUID = ?");
                pst3.setString(1, event.getUniqueId().toString());
                pst3.executeUpdate();

                String WarnKickMessage = TranslationUtil.Translate(self.getConfig().getString("PermBanMessage"), "&",
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("player", result2.getString("PlayerName"));
                        put("reason", result2.getString("Reason"));
                        put("issuer", result2.getString("Executioner"));
                        put("warnid", result2.getString("WarnID"));
                    }}
                );
                
                // 👋
                event.disallow(Result.KICK_OTHER, WarnKickMessage);
            }
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
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