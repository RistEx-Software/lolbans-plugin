package com.ristexsoftware.lolbans.Utils;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Runnables.QueryRunnable;

// This class is honestly a mess... It scares me, but it works...
// TODO: Clean this class
public class DatabaseUtil
{
    private static Main self = Main.getPlugin(Main.class);
    private static QueryRunnable CheckThread;

    /**
     * Initialize the database tables and connection. This also starts the synchronization thread for the database.
     * @return True if the tables were created successfully and the connection completed successfully.
     */
    public static boolean InitializeDatabase()
    {
        try 
        {
            DatabaseUtil.OpenConnection();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            self.getLogger().severe("Cannot connect to database, ensure your database is setup correctly and restart the server.");
            // Just exit and let the user figure it out.
            return false;
        }

        // Ensure Our tables are created.
        try
        {
            // TODO: Support table prefixes?
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_punishments"
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
                                            +"ArbiterName VARCHAR(17) NOT NULL,"
                                            +"ArbiterUUID VARCHAR(36) NOT NULL,"
                                            // Who un-punished (appealed their punishment) them
                                            +"AppealReason TEXT NULL,"
                                            // Who has reviewed and approved/denied the appeal.
                                            +"AppelleeName VARCHAR(17) NULL,"
                                            +"AppelleeUUID VARCHAR(36) NULL,"
                                            +"AppealTime TIMESTAMP NULL,"
                                            // this will just make checking if they're banned or not easier...
                                            +"Appealed BOOLEAN DEFAULT FALSE,"
                                            +"Silent BOOLEAN DEFAULT FALSE,"
                                            // Used only when type == 3 for warnings.
                                            +"WarningAck BOOLEAN DEFAULT FALSE"
                                            +")").execute();
                                            
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_users "
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"UUID VARCHAR(36) NOT NULL,"
                                            +"PlayerName VARCHAR(17),"
                                            +"IPAddress VARCHAR(48) NOT NULL,"
                                            +"Country VARCHAR(64) NOT NULL,"
                                            +"CountryCode VARCHAR(3) NOT NULL,"
                                            +"FirstLogin TIMESTAMP NOT NULL,"
                                            +"LastLogin TIMESTAMP NOT NULL,"
                                            +"Punishments INT NULL,"
                                            +"LastPunished TIMESTAMP NULL,"
                                            +"TimesConnected INT NULL"
                                            +")").execute();

            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_banwave"
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"UUID varchar(36) NOT NULL,"
                                            +"PlayerName varchar(17) NOT NULL,"
                                            +"IPAddress varchar(48) NOT NULL,"
                                            +"Reason TEXT NULL,"
                                            +"ArbiterName varchar(17) NOT NULL,"
                                            +"ArbiterUUID varchar(36) NOT NULL,"
                                            +"PunishID varchar(20) NOT NULL,"
                                            +"TimePunished TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"Expiry TIMESTAMP NULL"
                                            +")").execute();

            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_ipbans"
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"IPAddress varchar(49) NOT NULL,"
                                            +"Reason TEXT NULL,"
                                            +"ArbiterName varchar(17) NOT NULL,"
                                            +"ArbiterUUID VARCHAR(36) NOT NULL,"
                                            // Who un-punished (appealed their punishment) them
                                            +"AppealReason TEXT NULL,"
                                            // Who has reviewed and approved/denied the appeal.
                                            +"AppelleeName VARCHAR(17) NULL,"
                                            +"AppelleeUUID VARCHAR(36) NULL,"
                                            +"AppealTime TIMESTAMP NULL,"
                                            // this will just make checking if they're banned or not easier...
                                            +"Appealed BOOLEAN DEFAULT FALSE,"
                                            +"PunishID varchar(20) NOT NULL,"
                                            +"TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"Expiry TIMESTAMP NULL"
                                            +")").execute();
            //self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS LinkConfirmations (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, Executioner varchar(17) NOT NULL, LinkID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NOT NULL)").execute();
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_reports (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, PlaintiffUUID varchar(36) NOT NULL, PlaintiffName varchar(17) NOT NULL, DefendantUUID varchar(36) NOT NULL, DefendantName varchar(17) NOT NULL, Reason TEXT NOT NULL, JudgeUUID varchar(36) NULL, JudgeName varchar(17) NULL, Type varchar(32) NOT NULL, CloseReason TEXT NULL, Closed boolean DEFAULT FALSE NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PunishID varchar(20) NOT NULL)").execute();
            // NOTE: This table compares both minecraft names AND client hostnames against this, not sure yet if this is a good idea or not...
            self.connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_regexbans"
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"Regex VARCHAR(255) NOT NULL,"
                                            +"Reason TEXT NOT NULL,"
                                            +"ArbiterName varchar(17) NOT NULL,"
                                            +"ArbiterUUID VARCHAR(36) NOT NULL,"
                                            // Who un-punished (appealed their punishment) them
                                            +"AppealReason TEXT NULL,"
                                            // Who has reviewed and approved/denied the appeal.
                                            +"AppelleeName VARCHAR(17) NULL,"
                                            +"AppelleeUUID VARCHAR(36) NULL,"
                                            +"AppealTime TIMESTAMP NULL,"
                                            // this will just make checking if they're banned or not easier...
                                            +"Appealed BOOLEAN DEFAULT FALSE,"
                                            +"PunishID varchar(20) NOT NULL,"
                                            +"TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"Expiry TIMESTAMP NULL"
                                            +")").execute();
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

    /**
     * Terminate the connection to the database.
     */
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

    /**
     * Actually open the conenction to the database, this should not be used outside this class.
     * @throws SQLException SQL exception if the connection fails
     */
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

    /**
     * Execute a database query asynchronously and return the result later.
     * @param statement A {@link java.sql.PreparedStatement} to execute later.
     * @return A future optional ResultSet of the results from the database query
     */
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
                catch (Throwable e) 
                {
                    e.printStackTrace();
                    return Optional.empty();
                }
            }
        });

        Main.pool.execute(t);

        return t;
    }

    /**
     * Asynchronously execute an update query for the database.
     * @param statement the {@link java.sql.PreparedStatement} to execute later.
     * @return An integer of the number of rows updated by the statement.
     */
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
                catch (Throwable e) 
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

    /**
     * Get the latest ID from the database table
     * @param table Table to get the latest id from
     * @return The latest id
     * @throws SQLException An exception if the database query fails.
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

    /**
     * Insert a user into the database.
     * @param UUID UUID of the minecraft user
     * @param PlayerName Name of the minecraft player
     * @param IPAddress IP address of the minecraft player
     * @param FirstLogin The first time they logged in (as a timestamp)
     * @param LastLogin The last time  they logged in (as a timestamp)
     * @return True if the user was created successfully
     */
    public static Future<Boolean> InsertUser(String UUID, String PlayerName, String IPAddress, Timestamp FirstLogin, Timestamp LastLogin)
    {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                //This is where you should do your database interaction
                try 
                {
                    String[] geodata = GeoLocation.GetIPLocation(IPAddress);

                    // Make sure we're not duping data, if they already exist go ahead and update them
                    // This happens because we insert every time they join for the first time, but if the playerdata is removed on the world
                    // or the spigot plugin is setup in  multiple servers using the same database, it would add them a second time
                    // lets not do that....
                    int j = 1;
                    PreparedStatement CheckUser = self.connection.
                    prepareStatement("SELECT id FROM lolbans_users WHERE UUID = ?");
                    CheckUser.setString(j++, UUID);
                    ResultSet results = CheckUser.executeQuery();
                    if (results.next() && !results.wasNull())
                    {
                        UpdateUser(UUID, PlayerName, IPAddress, LastLogin);
                        return true;
                    }

                    // Preapre a statement
                    int i = 1;
                    PreparedStatement InsertUser = self.connection
                    .prepareStatement(String.format("INSERT INTO lolbans_users (UUID, PlayerName, IPAddress, FirstLogin, LastLogin, TimesConnected, Country, CountryCode) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"));
                    InsertUser.setString(i++, UUID);
                    InsertUser.setString(i++, PlayerName);
                    InsertUser.setString(i++, IPAddress);
                    InsertUser.setTimestamp(i++, FirstLogin);
                    InsertUser.setTimestamp(i++, LastLogin);
                    InsertUser.setInt(i++, 1);
                    InsertUser.setString(i++, geodata[1]);
                    InsertUser.setString(i++, geodata[0]);
                    InsertUser.executeUpdate();
                } 
                catch (Throwable e) 
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

    /**
     * Update a user record
     * @param UUID lolbans_users current UUID
     * @param PlayerName lolbans_users current player name
     * @param IPAddress lolbans_users current IP address
     * @param LastLogin The timestamp of the last time a user logged in
     * @return True if the update was successful.
     */
    public static Future<Boolean> UpdateUser(String UUID, String PlayerName, String IPAddress, Timestamp LastLogin)
    //(Timestamp LastLogin, String PlayerName, String IPAddress, String UUID)
    {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                //This is where you should do your database interaction
                try
                {
                    String[] geodata = GeoLocation.GetIPLocation(IPAddress);

                    int j = 1;
                    // This is a fail-safe just incase the table was dropped or the player joined the server BEFORE the plugin was added...
                    // This will ensure they get added to the database no matter what.
                    PreparedStatement CheckUser = self.connection.
                    prepareStatement(String.format("SELECT id FROM lolbans_users WHERE UUID = ?"));
                    CheckUser.setString(j++, UUID);
                    ResultSet results = CheckUser.executeQuery();
                    if (!results.next())
                    {
                        Timestamp FirstLogin = TimeUtil.TimestampNow();
                        InsertUser(UUID, PlayerName, IPAddress, FirstLogin, LastLogin);
                        return true;
                    }

                    PreparedStatement gtc = self.connection.prepareStatement(String.format("SELECT TimesConnected FROM lolbans_users WHERE UUID = ?"));
                    gtc.setString(1, UUID);

                    ResultSet gtc2 = gtc.executeQuery();
                    int tc = 1;
                    if (gtc2.next())
                    {
                        if (!gtc2.wasNull()){
                            tc = gtc2.getInt("TimesConnected");
                        }
                        else
                        {
                            tc = 0;
                        }
                    }

                    // Preapre a statement
                    int i = 1;
                    PreparedStatement UpdateUser = self.connection
                    .prepareStatement(String.format("UPDATE lolbans_users SET LastLogin = ?, PlayerName = ?, IPAddress = ?, TimesConnected = ?, Country = ?, CountryCode = ? WHERE UUID = ?"));
                    UpdateUser.setTimestamp(i++, LastLogin);
                    UpdateUser.setString(i++, PlayerName);
                    UpdateUser.setString(i++, IPAddress);
                    UpdateUser.setInt(i++, ++tc);
                    UpdateUser.setString(i++, geodata[1]);
                    UpdateUser.setString(i++, geodata[0]);
                    UpdateUser.setString(i++, UUID);
                    UpdateUser.executeUpdate();
                } 
                catch (Throwable e) 
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

    public static Future<Integer> getPunishmentCount(PunishmentType type)
    {
        FutureTask<Integer> t = new FutureTask<>(new Callable<Integer>()
        {
            @Override
            public Integer call()
            {
                //This is where you should do your database interaction
                try 
                {   if (!(type == PunishmentType.PUNISH_REGEX || type == PunishmentType.PUNISH_IP)) {
                        PreparedStatement bans = self.connection.prepareStatement("SELECT COUNT(*) FROM lolbans_punishments WHERE Type = ?");
                        bans.setInt(1, type.ordinal());
                        ResultSet results = bans.executeQuery();
                        if (results.next() &&  !results.wasNull()) {
                            return results.getInt("COUNT(*)");
                        }
                    } else if (type == PunishmentType.PUNISH_REGEX) {
                        PreparedStatement bans = self.connection.prepareStatement("SELECT COUNT(*) FROM lolbans_regexbans");
                        ResultSet results = bans.executeQuery();
                        if (results.next() &&  !results.wasNull()) {
                            return results.getInt("COUNT(*)");
                        }
                    } else if (type == PunishmentType.PUNISH_IP) {
                        PreparedStatement bans = self.connection.prepareStatement("SELECT COUNT(*) FROM lolbans_ipbans");
                        ResultSet results = bans.executeQuery();
                        if (results.next() &&  !results.wasNull()) {
                            return results.getInt("COUNT(*)");
                        }
                    }
                } 
                catch (Throwable e) 
                {
                    e.printStackTrace();
                    return 0;
                }
                return 0;
            }
        });

        Main.pool.execute(t);

        return (Future<Integer>)t;
    }

    public static Future<Integer> getUsersCount()
    {
        FutureTask<Integer> t = new FutureTask<>(new Callable<Integer>()
        {
            @Override
            public Integer call()
            {
                //This is where you should do your database interaction
                try {
                    PreparedStatement users = self.connection.prepareStatement("SELECT COUNT(*) FROM lolbans_users");
                    ResultSet results = users.executeQuery();
                    if (results.next() &&  !results.wasNull()) {
                        return results.getInt("COUNT(*)");
                    }
                } 
                catch (Throwable e) 
                {
                    e.printStackTrace();
                    return 0;
                }
                return 0;
            }
        });

        Main.pool.execute(t);

        return (Future<Integer>)t;
    }
}
