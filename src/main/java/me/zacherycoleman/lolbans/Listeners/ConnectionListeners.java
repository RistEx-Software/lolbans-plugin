package me.zacherycoleman.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.TreeMap;
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

                if (BanTime != null)
                {
                    String TempBanMessage = TranslationUtil.Translate(self.getConfig().getString("TempBanMessage"), "&",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", event.getName());
                            put("reason", reason);
                            put("banner", sender);
                            put("expires", TimeUtil.Expires(BanTime));
                            put("banid", BanID);
                        }}
                    );

                    // We have to use this instead, because PreLogin doesn't return a player, because they havn't loaded into the world yet
                    event.disallow(Result.KICK_BANNED, TempBanMessage);
                }
                else
                {
                    String PermBanMessage = TranslationUtil.Translate(self.getConfig().getString("PermBanMessage"), "&",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("player", event.getName());
                            put("reason", reason);
                            put("banner", sender);
                            put("banid", BanID);
                        }}
                    );
                    event.disallow(Result.KICK_BANNED, PermBanMessage);
                }
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