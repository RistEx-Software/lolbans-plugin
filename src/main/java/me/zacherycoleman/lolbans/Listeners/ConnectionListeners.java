package me.zacherycoleman.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.User;

public class ConnectionListeners implements Listener 
{
    private static Main self = Main.getPlugin(Main.class);

    @EventHandler
    public void OnPlayerConnect(PlayerJoinEvent event) 
    {
        Main.USERS.put(event.getPlayer().getUniqueId(), new User(event.getPlayer()));

        try 
        {
            PreparedStatement pst = self.connection.prepareStatement(
                    "SELECT * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NULL OR Expiry >= NOW())");
            pst.setString(1, event.getPlayer().getUniqueId().toString());

            ResultSet result = pst.executeQuery();

            if (result.next()) 
            {
                Timestamp BanTime = result.getTimestamp("Expiry");
                User.KickPlayer(result.getString("Executioner"), event.getPlayer(), result.getString("BanID"), result.getString("Reason"), BanTime);
            }

            PreparedStatement pst2 = self.connection.prepareStatement(
                "SELECT * FROM Warnings WHERE UUID = ? AND Accepted = ?");
            pst2.setString(1, event.getPlayer().getUniqueId().toString());
            pst2.setBoolean(2, false);

            ResultSet result2 = pst2.executeQuery();

            if (result2.next()) 
            {
                //System.out.println(result2.getString("Reason"));
                //System.out.println(event.getPlayer());
                PreparedStatement pst3 = self.connection.prepareStatement("UPDATE Warnings SET Accepted = true WHERE UUID = ?");
                pst3.setString(1, event.getPlayer().getUniqueId().toString());
                pst3.executeUpdate();
                event.getPlayer().kickPlayer("You were warned!\nReason: " + result2.getString("Reason") + "\nRejoin to acknowledge that you've been warned");

                //User.KickPlayer(result.getString("Executioner"), event.getPlayer(), result.getString("BanID"), result.getString("Reason"), BanTime);
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
        Main.USERS.get(event.getPlayer().getUniqueId()).SetWarned(false);

        Main.USERS.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void OnPlayerKick(PlayerKickEvent event)
    {
        Main.USERS.get(event.getPlayer().getUniqueId()).SetWarned(false);

        Main.USERS.remove(event.getPlayer().getUniqueId());
    }
}