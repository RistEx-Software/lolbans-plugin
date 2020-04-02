package com.ristexsoftware.lolbans.Utils;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Runnables.QueryRunnable;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;



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
            // TODO: Support table prefixes?
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Punishments"
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            // Player info stuffs
                                            +"UUID VARCHAR(36) NOT NULL,"
                                            +"PlayerName VARCHAR(17) NOT NULL,"
                                            +"IPAddress VARCHAR(48) NOT NULL,"
                                            // (General punish info)
                                            +"Reason TEXT NULL,"
                                            +"PunishID VARCHAR(20) NOT NULL,"
                                            // 0 = Ban, 1 = Mute, 2 = Kick, 3 = Warn
                                            +"Type INT NOT NULL,"
                                            +"TimePunished TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"Expiry TIMESTAMP NULL,"
                                            // Who banned (punshied) them
                                            +"ExecutionerName VARCHAR(17) NOT NULL,"
                                            +"ExecutionerUUID VARCHAR(36) NOT NULL,"
                                            // Who un-punished (appealed their punishment) them
                                            +"AppealReason TEXT NULL,"
                                            +"AppealStaff VARCHAR(17) NULL,"
                                            +"AppealUUID VARCHAR(36) NULL,"
                                            +"AppealTime TIMESTAMP NULL,"
                                            // this will just make checking if they're banned or not easier...
                                            +"Appealed BOOLEAN DEFAULT FALSE,"
                                            // Used only when type == 3 for warnings.
                                            +"WarningAck BOOLEAN DEFAULT FALSE"
                                            +")").execute();
                                            
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Users "
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"UUID VARCHAR(36) NOT NULL,"
                                            +"PlayerName VARCHAR(17),"
                                            +"IPAddress VARCHAR(48) NOT NULL,"
                                            +"FirstLogin TIMESTAMP NOT NULL,"
                                            +"LastLogin TIMESTAMP NOT NULL,"
                                            +"Punishments INT NULL,"
                                            +"LastPunished TIMESTAMP NULL"
                                            +")").execute();

            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BanWave"
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"UUID varchar(36) NOT NULL,"
                                            +"PlayerName varchar(17) NOT NULL,"
                                            +"IPAddress varchar(48) NOT NULL,"
                                            +"Reason TEXT NULL,"
                                            +"ExecutionerName varchar(17) NOT NULL,"
                                            +"ExecutionerUUID varchar(36) NOT NULL,"
                                            +"PunishID varchar(20) NOT NULL,"
                                            +"TimePunished TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"Expiry TIMESTAMP NULL"
                                            +")").execute();

            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS IPBans (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, IPAddress varchar(49) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, PunishID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            // FIXME: How is this gonna work for our new website-based model?
            // (Links will still be a thing to link their UUID to the website, ig reports will only be local, or global so other
            // users are aware of why a player is being reported.)
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS LinkConfirmations (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, Executioner varchar(17) NOT NULL, LinkID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NOT NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Reports (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, PlaintiffUUID varchar(36) NOT NULL, PlaintiffName varchar(17) NOT NULL, DefendantUUID varchar(36) NOT NULL, DefendantName varchar(17) NOT NULL, Reason TEXT NOT NULL, JudgeUUID varchar(36) NULL, JudgeName varchar(17) NULL, Type varchar(32) NOT NULL, CloseReason TEXT NULL, Closed boolean DEFAULT FALSE NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PunishID varchar(20) NOT NULL)").execute();
            // NOTE: This table compares both minecraft names AND client hostnames against this, not sure yet if this is a good idea or not...
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS RegexBans (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, Regex VARCHAR(255) NOT NULL, Reason TEXT NOT NULL, Executioner varchar(17) NOT NULL, PunishID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
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
                    return Optional.empty();
                }
            }
        });

        Main.pool.execute(t);

        return t;
    }

    public static Future<Integer> ExecuteUpdate(PreparedStatement statement)
    {
        FutureTask<Integer> t = new FutureTask<>(new Callable<Integer>()
        {
            @Override
            public Integer call()
            {
                //This is where you should do your database interaction
                try 
                {
                    return statement.executeUpdate();
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                }
                return -1;
            }
        });

        Main.pool.execute(t);
        return t;
    }

    /*
    PUNISHMENT UTILS
    */

    public static int GenID(String table) throws SQLException
    {
        // Get the latest ID of the banned players to generate a PunishID form it.
        ResultSet ids = self.connection.createStatement().executeQuery("SELECT MAX(id) FROM " + table);
        int id = 1;
        if (ids.next())
        {
            if (!ids.wasNull())
                id = ids.getInt(1);
        }
        return id;
    }


    /*
    USER UTILS
    */
    public static Future<Boolean> InsertUser(String UUID, String PlayerName, String IPAddress, Timestamp FirstLogin, Timestamp LastLogin) throws SQLException
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
                    PreparedStatement InsertUser = self.connection
                    .prepareStatement(String.format("INSERT INTO Users (UUID, PlayerName, IPAddress, FirstLogin, LastLogin) VALUES (?, ?, ?, ?, ?)"));
                    InsertUser.setString(i++, UUID);
                    InsertUser.setString(i++, PlayerName);
                    InsertUser.setString(i++, IPAddress);
                    InsertUser.setTimestamp(i++, FirstLogin);
                    InsertUser.setTimestamp(i++, LastLogin);
                    InsertUser.executeUpdate();
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        Main.pool.execute(t);

        return (Future<Boolean>)t;
    }

    public static Future<Boolean> UpdateUser(Timestamp LastLogin, String PlayerName, String IPAddress, String UUID) throws SQLException
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
                    PreparedStatement UpdateUser = self.connection
                    .prepareStatement(String.format("UPDATE Users SET LastLogin = ?, PlayerName = ?, IPAddress = ? WHERE UUID = ?"));
                    UpdateUser.setTimestamp(i++, LastLogin);
                    UpdateUser.setString(i++, PlayerName);
                    UpdateUser.setString(i++, IPAddress);
                    UpdateUser.setString(i++, UUID);
                    UpdateUser.executeUpdate();
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        Main.pool.execute(t);

        return (Future<Boolean>)t;
    }
}