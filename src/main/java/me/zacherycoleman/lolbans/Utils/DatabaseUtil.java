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
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS MutedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, TimeMuted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, UnbanReason TEXT, UnbanExecutioner varchar(17), UnbanExecutionerUUID varchar(36), TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS MutedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, UnmuteReason TEXT, UnmuteExecutioner varchar(17), UnmuteExecutionerUUID varchar(36), TimeMuted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BanWave (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Warnings (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, Accepted boolean, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Kicks (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, IPAddress varchar(48) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, ExecutionerUUID varchar(36) NOT NULL, PunishID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS IPBans (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, IPAddress varchar(49) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, PunishID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS LinkConfirmations (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, Executioner varchar(17) NOT NULL, LinkID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NOT NULL)").execute();
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

    public static Future<Boolean> InsertBan(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String ExecutionerUUID, String BanID, Timestamp BanTime) throws SQLException
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
                    .prepareStatement(String.format("INSERT INTO BannedPlayers (UUID, PlayerName, IPAddress, Reason, Executioner, ExecutionerUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"));
                    InsertBan.setString(i++, UUID);
                    InsertBan.setString(i++, PlayerName);
                    InsertBan.setString(i++, IPAddress);
                    InsertBan.setString(i++, Reason);
                    InsertBan.setString(i++, Executioner.getName().toString());
                    InsertBan.setString(i++, ExecutionerUUID);
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

    public static Future<Boolean> InsertWarn(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String ExecutionerUUID, String WarnID) throws SQLException
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
                    PreparedStatement pst = self.connection.prepareStatement("INSERT INTO Warnings (UUID, PlayerName, IPAddress, Reason, Executioner, ExecutionerUUID, PunishID, Accepted) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                    pst.setString(i++, UUID);
                    pst.setString(i++, PlayerName);
                    pst.setString(i++, IPAddress);
                    pst.setString(i++, Reason);
                    pst.setString(i++, Executioner.getName().toString());
                    pst.setString(i++, ExecutionerUUID);
                    pst.setString(i++, WarnID);
                    pst.setBoolean(i++, false);
                    pst.executeUpdate();
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

    public static Future<Boolean> UnBan(String UUID, String PlayerName, String Reason, CommandSender Executioner, String ExecutionerUUID) throws SQLException
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
                    PreparedStatement pst2 = self.connection.prepareStatement("UPDATE BannedHistory INNER JOIN (SELECT PunishID AS LatestBanID, UUID as bUUID FROM BannedPlayers WHERE UUID = ?) tm SET UnbanReason = ?, UnbanExecutioner = ?, UnbanExecutionerUUID = ? WHERE UUID = tm.bUUID AND PunishID = tm.LatestBanID");
                    pst2.setString(i++, UUID);
                    pst2.setString(i++, Reason);
                    pst2.setString(i++, Executioner.getName().toString());
                    pst2.setString(i++, ExecutionerUUID);
                    pst2.executeUpdate();

                    int j = 1;
                    // Preapre a statement
                    PreparedStatement pst = self.connection.prepareStatement("DELETE FROM BannedPlayers WHERE UUID = ?");
                    pst.setString(j++, UUID);
                    pst.executeUpdate();
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

    /*
    PreparedStatement pst2 = self.connection.prepareStatement("UPDATE BannedHistory INNER JOIN (SELECT BanID AS LatestBanID, UUID as bUUID FROM BannedPlayers WHERE UUID = ?) tm SET UnbanReason = ?, UnbanExecutioner = ? WHERE UUID = tm.bUUID AND BanID = tm.LatestBanID");
    pst2.setString(1, target.getUniqueId().toString());
    pst2.setString(2, reason);
    pst2.setString(3, sender.getName());
    pst2.executeUpdate();

    // Preapre a statement
    PreparedStatement pst = self.connection.prepareStatement("DELETE FROM BannedPlayers WHERE UUID = ?");
    pst.setString(1, target.getUniqueId().toString());
    pst.executeUpdate();
    */

    public static Future<Boolean> UnMute(String UUID, String PlayerName, String Reason, CommandSender Executioner, String ExecutionerUUID) throws SQLException
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
                    // We need to get the latest banid first.
                    PreparedStatement pst2 = self.connection.prepareStatement("UPDATE MutedHistory INNER JOIN (SELECT PunishID AS LatestMuteID, UUID as bUUID FROM MutedPlayers WHERE UUID = ?) tm SET UnmuteReason = ?, UnmuteExecutioner = ?, UnmuteExecutionerUUID = ? WHERE UUID = tm.bUUID AND PunishID = tm.LatestMuteID");
                    pst2.setString(1, UUID);
                    pst2.setString(2, Reason);
                    pst2.setString(3, Executioner.getName().toString());
                    pst2.setString(4, ExecutionerUUID);
                    pst2.executeUpdate();

                    // Preapre a statement
                    PreparedStatement pst = self.connection.prepareStatement("DELETE FROM MutedPlayers WHERE UUID = ?");
                    pst.setString(1, UUID);
                    pst.executeUpdate();
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

    public static Future<Boolean> InsertMute(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String ExecutionerUUID, String MuteID, Timestamp MuteTime) throws SQLException
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
                    .prepareStatement(String.format("INSERT INTO MutedPlayers (UUID, PlayerName, IPAddress, Reason, Executioner, ExecutionerUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"));
                    InsertBan.setString(i++, UUID);
                    InsertBan.setString(i++, PlayerName);
                    InsertBan.setString(i++, IPAddress);
                    InsertBan.setString(i++, Reason);
                    InsertBan.setString(i++, Executioner.getName().toString());
                    InsertBan.setString(i++, ExecutionerUUID);
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

    public static Future<Boolean> InsertHistory(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String ExecutionerUUID, String BanID, Timestamp BanTime) throws SQLException
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
                    .prepareStatement(String.format("INSERT INTO BannedHistory (UUID, PlayerName, IPAddress, Reason, Executioner, ExecutionerUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"));
                    pst.setString(i++, UUID);
                    pst.setString(i++, PlayerName);
                    pst.setString(i++, IPAddress);
                    pst.setString(i++, Reason);
                    pst.setString(i++, Executioner.getName().toString());
                    pst.setString(i++, ExecutionerUUID);
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

    public static Future<Boolean> InsertMuteHistory(String UUID, String PlayerName, String IPAddress, String Reason, CommandSender Executioner, String ExecutionerUUID, String MuteID, Timestamp MuteTime) throws SQLException
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
                    .prepareStatement(String.format("INSERT INTO MutedHistory (UUID, PlayerName, IPAddress, Reason, Executioner, ExecutionerUUID, PunishID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"));
                    pst.setString(i++, UUID);
                    pst.setString(i++, PlayerName);
                    pst.setString(i++, IPAddress);
                    pst.setString(i++, Reason);
                    pst.setString(i++, Executioner.getName().toString());
                    pst.setString(i++, ExecutionerUUID);
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