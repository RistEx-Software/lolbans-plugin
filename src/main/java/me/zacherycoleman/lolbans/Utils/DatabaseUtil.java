package me.zacherycoleman.lolbans.Utils;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Runnables.QueryRunnable;
import me.zacherycoleman.lolbans.Utils.Configuration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;



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
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS MutedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, MuteID varchar(20) NOT NULL, TimeMuted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, UnbanReason TEXT, UnbanExecutioner varchar(17), TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS MutedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, MuteID varchar(20) NOT NULL, UnmuteReason TEXT, UnmuteExecutioner varchar(17), TimeMuted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BanWave (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Warnings (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, WarnID varchar(20) NOT NULL, Accepted boolean, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS IPBans (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
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

    public static Future<Optional<ResultSet>> ExecuteLater(PreparedStatement statement)
    {
        FutureTask<Optional<ResultSet>> t = new FutureTask<>(new Callable<Optional<ResultSet>>()
        {
            @Override
            public Optional<ResultSet> call()
            {
                //This is where you should do your database interaction
                try 
                {
                    return Optional.ofNullable(statement.executeQuery());
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    return Optional.ofNullable(null);
                }
            }
        });

        self.pool.execute(t);

        return t;
    }

    public static Future<Boolean> InsertBan(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String BanID, Timestamp BanTime) throws SQLException
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
                    int i = 1;
                    PreparedStatement InsertBan = self.connection
                    .prepareStatement(String.format("INSERT INTO BannedPlayers (UUID, PlayerName, IPAddress, Reason, Executioner, BanID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?)"));
                    InsertBan.setString(i++, UUID);
                    InsertBan.setString(i++, PlayerName);
                    InsertBan.setString(i++, IPAddress);
                    InsertBan.setString(i++, Reason);
                    InsertBan.setString(i++, Executioner.getName().toString());
                    InsertBan.setString(i++, BanID);
                    InsertBan.setTimestamp(i++, BanTime);
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

    public static Future<Boolean> InsertMute(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String MuteID, Timestamp MuteTime) throws SQLException
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
                    int i = 1;
                    PreparedStatement InsertBan = self.connection
                    .prepareStatement(String.format("INSERT INTO MutedPlayers (UUID, PlayerName, IPAddress, Reason, Executioner, MuteID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?)"));
                    InsertBan.setString(i++, UUID);
                    InsertBan.setString(i++, PlayerName);
                    InsertBan.setString(i++, IPAddress);
                    InsertBan.setString(i++, Reason);
                    InsertBan.setString(i++, Executioner.getName().toString());
                    InsertBan.setString(i++, MuteID);
                    InsertBan.setTimestamp(i++, MuteTime);
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

    public static Future<Boolean> InsertHistory(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String BanID, Timestamp BanTime) throws SQLException
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
                    int i = 1;
                    PreparedStatement pst = self.connection
                    .prepareStatement(String.format("INSERT INTO BannedHistory (UUID, PlayerName, IPAddress, Reason, Executioner, BanID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?)"));
                    pst.setString(i++, UUID);
                    pst.setString(i++, PlayerName);
                    pst.setString(i++, IPAddress);
                    pst.setString(i++, Reason);
                    pst.setString(i++, Executioner.getName().toString());
                    pst.setString(i++, BanID);
                    pst.setTimestamp(i++, BanTime);
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

    public static Future<Boolean> InsertMuteHistory(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String MuteID, Timestamp MuteTime) throws SQLException
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
                    int i = 1;
                    PreparedStatement pst = self.connection
                    .prepareStatement(String.format("INSERT INTO MutedHistory (UUID, PlayerName, IPAddress, Reason, Executioner, MuteID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?)"));
                    pst.setString(i++, UUID);
                    pst.setString(i++, PlayerName);
                    pst.setString(i++, IPAddress);
                    pst.setString(i++, Reason);
                    pst.setString(i++, Executioner.getName().toString());
                    pst.setString(i++, MuteID);
                    pst.setTimestamp(i++, MuteTime);
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