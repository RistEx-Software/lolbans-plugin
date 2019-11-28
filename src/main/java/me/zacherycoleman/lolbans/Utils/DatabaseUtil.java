package me.zacherycoleman.lolbans.Utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.zacherycoleman.lolbans.Main;

public class DatabaseUtil
{

    public static Future<Boolean> InsertBan(String UUID, String PlayerName, String Reason, CommandSender Executioner, String BanID, Timestamp BanTime) throws SQLException
    {
        Main self = Main.getPlugin(Main.class);

        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                System.out.println("Inside INSERT BAN!");
                //This is where you should do your database interaction
                try 
                {
                    // Preapre a statement
                    PreparedStatement InsertBan = self.connection
                    .prepareStatement(String.format("INSERT INTO BannedPlayers (UUID, PlayerName, Reason, Executioner, BanID, Expiry) VALUES (?, ?, ?, ?, ?, ?)"));
                    InsertBan.setString(1, UUID);
                    InsertBan.setString(2, PlayerName);
                    InsertBan.setString(3, Reason);
                    InsertBan.setString(4, Executioner.getName().toString());
                    InsertBan.setString(5, BanID);
                    InsertBan.setTimestamp(6, BanTime);
                    InsertBan.executeUpdate();
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    Executioner.sendMessage(ChatColor.RED + "The server encountered an error, please try again later.1");
                    return false;
                }
                return true;
            }
        });

        System.out.println("Inserting into bukkit scheduler!");
        
        Bukkit.getScheduler().runTaskAsynchronously(self, t);
        return (Future<Boolean>)t;
    }

    public static Future<Boolean> InsertHistory(String UUID, String PlayerName, String Reason, CommandSender Executioner, String BanID, Timestamp BanTime) throws SQLException
    {
        Main self = Main.getPlugin(Main.class);
        
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                //This is where you should do your database interaction
                try 
                {
                    // Preapre a statement
                    PreparedStatement pst = self.connection
                    .prepareStatement(String.format("INSERT INTO BannedHistory (UUID, PlayerName, Reason, Executioner, BanID, Expiry) VALUES (?, ?, ?, ?, ?, ?)"));
                    pst.setString(1, UUID);
                    pst.setString(2, PlayerName);
                    pst.setString(3, Reason);
                    pst.setString(4, Executioner.getName().toString());
                    pst.setString(5, BanID);
                    pst.setTimestamp(6, BanTime);
                    pst.executeUpdate();
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    Executioner.sendMessage(ChatColor.RED + "The server encountered an error, please try again later.2");
                    return false;
                }
                return true;
            }
        });

        Bukkit.getScheduler().runTaskAsynchronously(self, t);

        return (Future<Boolean>)t;
    }

    public static int GenID() throws SQLException
    {
        Main self = Main.getPlugin(Main.class);

        // Get the latest ID of the banned players to generate a BanID form it.
        ResultSet ids = self.connection.createStatement().executeQuery("SELECT MAX(id) FROM BannedPlayers");
        int id = 1;
        if (ids.next())
        {
            if (!ids.wasNull())
                id = ids.getInt(1);
        }
        return id;
    }
    
    /* 
    // Add everything to the history DB
    PreparedStatement pst2 = self.connection.prepareStatement("INSERT INTO BannedHistory (UUID, PlayerName, Reason, Executioner, BanID, Expiry) VALUES (?, ?, ?, ?, ?, ?)");
    pst2.setString(1, target.getUniqueId().toString());
    pst2.setString(2, target.getName());
    pst2.setString(3, reason);
    pst2.setString(4, sender.getName());
    pst2.setString(5, banid);
    pst2.setTimestamp(6, bantime);

    // Commit to the database.
    pst2.executeUpdate();
        */
}