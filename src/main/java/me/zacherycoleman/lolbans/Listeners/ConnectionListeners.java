package me.zacherycoleman.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

            Timestamp BanTime = result.getTimestamp("Expiry");
            String reason = result.getString("Reason");
            String sender = result.getString("Executioner");
            String BanID = result.getString("BanID");

            if (BanTime != null)
                Configuration.TempBanMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("TempBanMessage").replace("%player%", event.getName()).replace("%reason%", reason).replace("%banner%", sender).replace("%timetoexpire%", BanTime.toString()).replace("%banid%", BanID));
            
            Configuration.PermBanMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("PermBanMessage").replace("%player%", event.getName()).replace("%reason%", reason).replace("%banner%", sender).replace("%banid%", BanID));

            if (result.next()) 
            {
                //User.KickPlayer(result.getString("Executioner"), event.getPlayer(), result.getString("BanID"), result.getString("Reason"), BanTime);
                // We have to use this instead, because PreLogin doesn't return a player, because they havn't loaded into the world yet
                if (BanTime != null)
                    event.disallow(Result.KICK_BANNED, Configuration.TempBanMessage);
                else
                    event.disallow(Result.KICK_BANNED, Configuration.PermBanMessage);
            }

            PreparedStatement pst2 = self.connection.prepareStatement("SELECT * FROM Warnings WHERE UUID = ? AND Accepted = ?");
            pst2.setString(1, event.getUniqueId().toString());
            pst2.setBoolean(2, false);

            ResultSet result2 = pst2.executeQuery();

            if (result2.next()) 
            {
                System.out.println(result2.getString("Reason"));
                System.out.println(event.getName());
                PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Warnings SET Accepted = true WHERE UUID = ?");
                pst3.setString(1, event.getUniqueId().toString());
                pst3.executeUpdate();
                event.disallow(Result.KICK_OTHER, "You were warned!\n Reason " + result2.getString("Reason") + "\nBy rejoning you acknowledge that you've been warned!");
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