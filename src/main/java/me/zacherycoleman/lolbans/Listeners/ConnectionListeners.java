package me.zacherycoleman.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.User;

public class ConnectionListeners implements Listener {
    private static Main self = Main.getPlugin(Main.class);

    @EventHandler
    public void OnPlayerConnect(PlayerJoinEvent event) {
        try {
            PreparedStatement pst = self.connection.prepareStatement(
                    "SELECT * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NULL OR Expiry >= NOW())");
            pst.setString(1, event.getPlayer().getUniqueId().toString());

            ResultSet result = pst.executeQuery();

            if (result.next()) {
                Timestamp BanTime = result.getTimestamp("Expiry");
                User.KickPlayer(result.getString("Executioner"), event.getPlayer(), result.getString("BanID"), result.getString("Reason"), BanTime);
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
        try 
        {
            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NULL OR Expiry >= NOW())");
            pst.setString(1, event.getPlayer().getUniqueId().toString());
            
            ResultSet result = pst.executeQuery();

            if (result.next())
            {
                event.setQuitMessage("");
            }
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void OnPlayerKick(PlayerKickEvent event)
    {

    }
}