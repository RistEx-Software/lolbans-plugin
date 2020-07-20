/* 
 *  LolBans - The advanced banning system for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.api.database;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.punishment.runnables.Query;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.spigot.Main;

// This class is honestly a mess... It scares me, but it works...
// TODO: Clean this class
public class Database
{
    // private static Main self = Main.getPlugin(Main.class);
    private static LolBans self;
    private static Query CheckThread;
    public static Connection connection;
    // Configuration.dbhost, Configuration.dbport, Configuration.dbname, Configuration.maxReconnects), Configuration.dbusername, Configuration.dbpassword

    /**
     * Initialize the database tables and connection. This also starts the synchronization thread for the database.
     * @param host The database host
     * @param username The database username
     * @param password The database password
     * @param database The database name
     * @param port The port to connect with
     * @param maxReconnects The amount of retries
     * @param QueryUpdateLong The amount of time between the punishment queries
     * @return True if the tables were created successfully and the connection completed successfully.
     */
    public static boolean InitializeDatabase(String host, String username, String password, String database, Integer port, Integer MaxReconnects, Long QueryUpdateLong)
    {
        try 
        {
            Database.OpenConnection(host, username, password, database, port, MaxReconnects);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            LolBans.getLogger().severe("Cannot connect to database, ensure your database is setup correctly and restart the server.");
            // Just exit and let the user figure it out.
            return false;
        }

        // Ensure Our tables are created.
        try
        {
            // TODO: Support table prefixes?
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_punishments"
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
                                            
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_users "
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"UUID VARCHAR(36) NOT NULL,"
                                            +"PlayerName VARCHAR(17),"
                                            +"IPAddress VARCHAR(48) NOT NULL,"
                                            +"FirstLogin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"LastLogin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"TimesConnected INT NULL"
                                            +")").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_banwave"
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

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_ipbans"
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
            //connection.prepareStatement("CREATE TABLE IF NOT EXISTS LinkConfirmations (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, Executioner varchar(17) NOT NULL, LinkID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NOT NULL)").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_reports (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, PlaintiffUUID varchar(36) NOT NULL, PlaintiffName varchar(17) NOT NULL, DefendantUUID varchar(36) NOT NULL, DefendantName varchar(17) NOT NULL, Reason TEXT NOT NULL, JudgeUUID varchar(36) NULL, JudgeName varchar(17) NULL, Type varchar(32) NOT NULL, CloseReason TEXT NULL, Closed boolean DEFAULT FALSE NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PunishID varchar(20) NOT NULL)").execute();
            // NOTE: This table compares both minecraft names AND client hostnames against this, not sure yet if this is a good idea or not...
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_regexbans"
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
            LolBans.getLogger().severe("Cannot create database tables, please ensure your SQL user has the correct permissions.");
            return false; 
        }

        // Schedule a repeating task to delete expired bans.
        // Database.CheckThread = new Query();
        // Database.CheckThread.runTaskTimerAsynchronously(self, 20L, QueryUpdateLong * 20L);
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
        if (connection != null)
        {
            try 
            {
                connection.close();
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
    private static void OpenConnection(String host, String username, String password, String database, Integer port, Integer maxReconnects) throws SQLException
    {
        if (connection != null && !connection.isClosed())
            return;

        synchronized (Main.getPlugin(Main.class).isEnabled() ? Main.getPlugin(Main.class) : null)
        {
            if (connection != null && !connection.isClosed())
                return;

            connection =
                    DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&failOverReadOnly=false&maxReconnects=%d", 
                                   host, port, database, maxReconnects), username, password);
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

        LolBans.pool.execute(t);

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

        LolBans.pool.execute(t);
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
        ResultSet ids = connection.createStatement().executeQuery("SELECT MAX(id) FROM " + table);
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
                    // Make sure we're not duping data, if they already exist go ahead and update them
                    // This happens because we insert every time they join for the first time, but if the playerdata is removed on the world
                    // or the spigot plugin is setup in  multiple servers using the same database, it would add them a second time
                    // lets not do that....
                    int j = 1;
                    PreparedStatement CheckUser = connection.
                    prepareStatement("SELECT id FROM lolbans_users WHERE UUID = ?");
                    CheckUser.setString(j++, UUID);
                    ResultSet results = CheckUser.executeQuery();
                    if (results.next() && !results.wasNull())
                    {
                        UpdateUser(UUID, PlayerName, IPAddress, LastLogin);
                        return true;
                    }
                    LolBans.getLogger().warning(CheckUser.toString());

                    // Preapre a statement
                    int i = 1;
                    PreparedStatement InsertUser = connection
                    .prepareStatement(String.format("INSERT INTO lolbans_users (UUID, PlayerName, IPAddress, FirstLogin, LastLogin, TimesConnected) VALUES (?, ?, ?, ?, ?, ?)"));
                    InsertUser.setString(i++, UUID);
                    InsertUser.setString(i++, PlayerName);
                    InsertUser.setString(i++, IPAddress);
                    InsertUser.setTimestamp(i++, FirstLogin);
                    InsertUser.setTimestamp(i++, LastLogin);
                    InsertUser.setInt(i++, 1);
                    InsertUser.executeUpdate();
                    LolBans.getLogger().warning(InsertUser.toString());
                } 
                catch (Throwable e) 
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        LolBans.pool.execute(t);

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

                    int j = 1;
                    // This is a fail-safe just incase the table was dropped or the player joined the server BEFORE the plugin was added...
                    // This will ensure they get added to the database no matter what.
                    PreparedStatement CheckUser = connection.
                    prepareStatement(String.format("SELECT id FROM lolbans_users WHERE UUID = ?"));
                    CheckUser.setString(j++, UUID);
                    ResultSet results = CheckUser.executeQuery();
                    if (!results.next())
                    {
                        Timestamp FirstLogin = TimeUtil.TimestampNow();
                        return InsertUser(UUID, PlayerName, IPAddress, FirstLogin, LastLogin).get();
                    }
                    LolBans.getLogger().warning(CheckUser.toString());

                    PreparedStatement gtc = connection.prepareStatement(String.format("SELECT TimesConnected FROM lolbans_users WHERE UUID = ?"));
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
                    PreparedStatement UpdateUser = connection
                    .prepareStatement(String.format("UPDATE lolbans_users SET LastLogin = ?, PlayerName = ?, IPAddress = ?, TimesConnected = ? WHERE UUID = ?"));
                    UpdateUser.setTimestamp(i++, LastLogin);
                    UpdateUser.setString(i++, PlayerName);
                    UpdateUser.setString(i++, IPAddress);
                    UpdateUser.setInt(i++, ++tc);
                    UpdateUser.setString(i++, UUID);
                    UpdateUser.executeUpdate();
                    LolBans.getLogger().warning(UpdateUser.toString());
                } 
                catch (Throwable e) 
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        LolBans.pool.execute(t);

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
                        PreparedStatement bans = connection.prepareStatement("SELECT COUNT(*) FROM lolbans_punishments WHERE Type = ?");
                        bans.setInt(1, type.ordinal());
                        ResultSet results = bans.executeQuery();
                        if (results.next() &&  !results.wasNull()) {
                            return results.getInt("COUNT(*)");
                        }
                    } else if (type == PunishmentType.PUNISH_REGEX) {
                        PreparedStatement bans = connection.prepareStatement("SELECT COUNT(*) FROM lolbans_regexbans");
                        ResultSet results = bans.executeQuery();
                        if (results.next() &&  !results.wasNull()) {
                            return results.getInt("COUNT(*)");
                        }
                    } else if (type == PunishmentType.PUNISH_IP) {
                        PreparedStatement bans = connection.prepareStatement("SELECT COUNT(*) FROM lolbans_ipbans");
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

        LolBans.pool.execute(t);

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
                    PreparedStatement users = connection.prepareStatement("SELECT COUNT(*) FROM lolbans_users");
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

        LolBans.pool.execute(t);

        return (Future<Integer>)t;
    }

    public static Future<Boolean> isUserBanned(UUID uuid)
    {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                try {
                    PreparedStatement ps = Database.connection.prepareStatement(
                            "SELECT 1 FROM lolbans_punishments WHERE UUID = ? AND Type = 0 AND Appealed = FALSE LIMIT 1");
                    ps.setString(1, uuid.toString());
        
                    return ps.executeQuery().next();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });

        LolBans.pool.execute(t);

        return (Future<Boolean>)t;
    }

    /**
     * Get the last ip of a user
     * 
     * @param uuid UUID of player to check
     * @return The last IP of the specified user
     */
    private static Future<String> getLastIP(String uuid) {
        FutureTask<String> t = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() {
                // This is where you should do your database interaction
                try {
                    PreparedStatement ps = connection
                            .prepareStatement("SELECT ipaddress FROM lolbans_users WHERE UUID = ? LIMIT 1");
                    ps.setString(1, uuid);
                    ResultSet results = ps.executeQuery();
                    if (results.next()) {
                        if (results.getString("ipaddress").contains(",")) {
                            String[] iplist = results.getString("ipaddress").split(",");
                            return iplist[iplist.length - 1];
                        }
                        return results.getString("ipaddress");
                    }
                    return null;
                } catch (Throwable e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        LolBans.pool.execute(t);
        return (Future<String>) t;
    }

    /**
     * Get the last ip of a user
     * 
     * @param uuid UUID of player to check
     * @return The last IP of the specified user
     */
    public static String getLastAddress(String uuid) {
        try {
            return getLastIP(uuid).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
