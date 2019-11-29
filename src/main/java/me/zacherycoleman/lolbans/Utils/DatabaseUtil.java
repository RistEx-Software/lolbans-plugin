package me.zacherycoleman.lolbans.Utils;

import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.zacherycoleman.lolbans.Runnables.QueryRunnable;
import me.zacherycoleman.lolbans.Main;

public class DatabaseUtil
{
    private static Main self = Main.getPlugin(Main.class);
    private static QueryRunnable CheckThread;

    public static boolean InitializeDatabase()
    {
        try 
        {
            DatabaseUtil.OpenConnection();
        }
        catch (SQLException e)
        {
            //e.printStackTrace();
            self.getLogger().severe("Cannot connect to database, ensure your database is setup correctly and restart the server.");
            // Just exit and let the user figure it out.
            return false;
        }

        // Ensure Our tables are created.
        try
        {
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, UnbanReason TEXT, UnbanExecutioner varchar(17), TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BanWave (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Warnings (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, WarnID varchar(20) NOT NULL, Accepted boolean, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            self.getLogger().severe("Cannot create database tables, please ensure your SQL user has the correct permissions.");
            return false;
        }

        // Schedule a repeating task to delete expired bans.
        DatabaseUtil.CheckThread = new QueryRunnable();
        DatabaseUtil.CheckThread.runTaskTimerAsynchronously(self, 20L, Configuration.QueryUpdateLong * 20L);
        return true;
    }

    public static void Terminate()
    {
        // Terminate our thread.
        if (CheckThread != null)
            CheckThread.cancel();

        // Close the database connection (if open)
        if (self.connection != null)
        {
            try 
            {
                self.connection.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void OpenConnection() throws SQLException
    {
        if (self.connection != null && !self.connection.isClosed())
            return;

        synchronized (self)
        {
            if (self.connection != null && !self.connection.isClosed())
                return;

            self.connection =
                    DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&failOverReadOnly=false&maxReconnects=%d", 
                                    Configuration.dbhost, Configuration.dbport, Configuration.dbname, Configuration.MaxReconnects), Configuration.dbusername, Configuration.dbpassword);
        }
    }

    public static Future<Boolean> InsertBan(String UUID, String PlayerName, String Reason, CommandSender Executioner, String BanID, Timestamp BanTime) throws SQLException
    {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
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

        self.pool.execute(t);

        return (Future<Boolean>)t;
    }

    public static Future<Boolean> InsertHistory(String UUID, String PlayerName, String Reason, CommandSender Executioner, String BanID, Timestamp BanTime) throws SQLException
    {
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

        self.pool.execute(t);

        return (Future<Boolean>)t;
    }

    public static int GenID() throws SQLException
    {
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
}