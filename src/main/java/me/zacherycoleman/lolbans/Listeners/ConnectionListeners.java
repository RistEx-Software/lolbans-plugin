package me.zacherycoleman.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.zacherycoleman.lolbans.Main;

public class ConnectionListeners implements Listener
{
    private static Main self = Main.getPlugin(Main.class);

    @EventHandler
    public void OnPlayerConnect(PlayerJoinEvent event) 
    {
        try 
        {
            PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NOT NULL AND Expiry >= NOW())");
            pst.setString(1, event.getPlayer().getUniqueId().toString());
            
            ResultSet result = pst.executeQuery();

            // Remove the ban if it has expired (There will also be a thread running for this, but this is so when they join (if the thread hasn't already unbanned them) will unban them)
            PreparedStatement pst2 = self.connection.prepareStatement("DELETE * FROM BannedPlayers WHERE UUID = ? AND (Expiry IS NOT NULL AND Expiry >= NOW())");
            pst2.setString(1, event.getPlayer().getUniqueId().toString());
            pst2.executeUpdate();

            if (result.next())
            {
                self.KickPlayer(result.getString("Executioner"), event.getPlayer(), result.getString("BanID"), result.getString("Reason"), result.getString("Expriry"));
                event.setJoinMessage("");
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

    }

    @EventHandler
    public void OnPlayerKick(PlayerKickEvent event)
    {

    }
}