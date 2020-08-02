/* 
 *  LolBans - An advanced punishment management system made for Minecraft
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

package com.ristexsoftware.lolbans.api;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.configuration.file.FileConfiguration;
import com.ristexsoftware.lolbans.bukkit.Main;

// import com.ristexsoftware.lolbans.api.punishment.runnables.Query;

public class Database {
    public static Connection connection;
    private static String host = null;
    private static String username = null;
    private static String password = null;
    private static String database = null;
    private static Integer port = 3306;
    private static Integer maxReconnects = 5;

    // private static Query CheckThread;

    public static boolean initDatabase() {
        FileConfiguration config = LolBans.getPlugin().getConfig();
        host = config.getString("database.host");
        port = config.getInt("database.port");
        database = config.getString("database.name");
        username = config.getString("database.username");
        password = config.getString("database.password");
        maxReconnects = config.getInt("database.MaxReconnects");
        // queryUpdateLong = config.getLong("database.QueryUpdate");

        try {
            openConnection(host, username, password, database, port, maxReconnects);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        // Ensure Our tables are created.
        try {
            // TODO: Support table prefixes?
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_punishments ("
                                            +"id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            // Player info stuffs
                                            +"target_uuid VARCHAR(36) NULL,"
                                            +"target_name VARCHAR(17) NULL,"
                                            +"target_ip_address VARCHAR(48) DEFAULT '#',"
                                            // (General punish info)
                                            +"reason TEXT NULL,"
                                            +"punish_id VARCHAR(20) NOT NULL,"
                                            +"type INT NOT NULL,"
                                            // 0 = Ban, 1 = Mute, 2 = Kick, 3 = Warn
                                            +"time_punished TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"expires_at TIMESTAMP NULL,"
                                            +"commit_punishment_by TIMESTAMP NULL,"
                                            // Who banned (punshied) them
                                            +"punished_by_name VARCHAR(17) NOT NULL,"
                                            +"punished_by_uuid VARCHAR(36) NOT NULL,"
                                            // Who un-punished (appealed) them
                                            +"appealed_by_name VARCHAR(17) NULL,"
                                            +"appealed_by_uuid VARCHAR(36) NULL," // Who has reviewed and approved/denied the appeal.           
                                
                                            // categorize this nonsense
                                            +"appealed BOOLEAN DEFAULT FALSE," // this will just make checking if they're banned or not easier...
                                            +"appeal_reason TEXT NULL,"
                                            +"appealed_at TIMESTAMP NULL,"
                                            +"silent BOOLEAN DEFAULT FALSE," 
                                            +"warning_ack BOOLEAN DEFAULT FALSE,"  // Used only when type == 3 for warnings.
                                            +"ip_ban BOOLEAN DEFAULT FALSE," // for IP bans
                                            +"regex_ban BOOLEAN DEFAULT FALSE,"  // for regex bans
                                            +"regex TEXT NULL,"
                                            +"banwave BOOLEAN DEFAULT FALSE"
                                            +")").execute();
                                            
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_users "
                                            +"(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            +"player_uuid VARCHAR(36) NOT NULL,"
                                            +"player_name VARCHAR(17),"
                                            +"ip_address VARCHAR(48) NOT NULL,"
                                            +"first_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"last_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            +"times_connected INT NULL"
                                            +")").execute();
                                            
            //connection.prepareStatement("CREATE TABLE IF NOT EXISTS LinkConfirmations (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, Executioner varchar(17) NOT NULL, LinkID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NOT NULL)").execute();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS lolbans_reports (" 
                                            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                            + "reported_by_uuid varchar(36) NOT NULL,"
                                            + "reported_by_name varchar(17) NOT NULL,"
                                            + "reported_uuid varchar(36) NOT NULL,"
                                            + "reported_name varchar(17) NOT NULL,"
                                            + "reason TEXT NOT NULL,"
                                            + "claimed_by_name varchar(17) NULL,"
                                            + "claimed_by_uuid varchar(36) NULL,"
                                            + "type varchar(32) NOT NULL,"
                                            + "close_reason TEXT NULL,"
                                            + "closed boolean DEFAULT FALSE NOT NULL,"
                                            + "time_added TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                            + "punish_id varchar(20) NOT NULL" 
                                            + ")").execute();
            // NOTE: This table compares both minecraft names AND client hostnames against
            // this, not sure yet if this is a good idea or not...
        } catch (SQLException e) {
            e.printStackTrace();
            LolBans.getLogger()
                    .severe("Cannot create database tables, please ensure your SQL user has the correct permissions.");
            return false;
        }

        // Schedule a repeating task to delete expired bans.
        // Database.CheckThread = new Query();
        // Database.CheckThread.runTaskTimerAsynchronously(self, 20L, QueryUpdateLong *
        // 20L);
        return true;
    }

    /**
     * Actually open the conenction to the database, this should not be used outside
     * this class.
     * 
     * @throws SQLException SQL exception if the connection fails
     */
    private static void openConnection(String host, String username, String password, String database, Integer port,
            Integer maxReconnects) throws SQLException {
        if (connection != null && !connection.isClosed())
            return;
            
        synchronized (LolBans.getPlugin()) {
            if (connection != null && !connection.isClosed())
                return;

            connection = DriverManager.getConnection(
                    String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&failOverReadOnly=false&maxReconnects=%d", host,
                            port, database, maxReconnects),
                    username, password);
        }
    }

    /**
     * Terminate the connection to the database.
     */
    public static void Terminate() {
        // Terminate our thread.
        // if (CheckThread != null)
        // CheckThread.cancel();

        // Close the database connection (if open)
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Execute a database query asynchronously and return the result later.
     * 
     * @param statement A {@link java.sql.PreparedStatement} to execute later.
     * @return A future optional ResultSet of the results from the database query
     */
    public static Future<Optional<ResultSet>> ExecuteLater(PreparedStatement statement) {
        FutureTask<Optional<ResultSet>> t = new FutureTask<>(new Callable<Optional<ResultSet>>() {
            @Override
            public Optional<ResultSet> call() {
                // This is where you should do your database interaction
                try {
                    return Optional.ofNullable(statement.executeQuery());
                } catch (Throwable e) {
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
     * 
     * @param statement the {@link java.sql.PreparedStatement} to execute later.
     * @return An integer of the number of rows updated by the statement.
     */
    public static Future<Integer> ExecuteUpdate(PreparedStatement statement) {
        FutureTask<Integer> t = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() {
                // This is where you should do your database interaction
                try {
                    return statement.executeUpdate();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return -1;
            }
        });

        LolBans.pool.execute(t);
        return t;
    }

    /*
     * PUNISHMENT UTILS
     */

    /**
     * Get the latest ID from the database table
     * 
     * @param table Table to get the latest id from
     * @return The latest id
     * @throws SQLException An exception if the database query fails.
     */
    public static int genID(String table) throws SQLException {
        // Get the latest ID of the banned players to generate a PunishID form it.
        ResultSet ids = connection.createStatement().executeQuery("SELECT MAX(id) FROM " + table);
        int id = 1;
        if (ids.next() && !ids.wasNull())
            id = ids.getInt(1);
        return id;
    }

    public static Future<Boolean> isUserBanned(UUID uuid) {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    PreparedStatement ps = Database.connection.prepareStatement(
                            "SELECT 1 FROM lolbans_punishments WHERE target_uuid = ? AND type = 0 AND appealed = FALSE LIMIT 1");
                    ps.setString(1, uuid.toString());

                    return ps.executeQuery().next();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });

        LolBans.pool.execute(t);

        return (Future<Boolean>) t;
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
                            .prepareStatement("SELECT ip_address FROM lolbans_users WHERE player_uuid = ? LIMIT 1");
                    ps.setString(1, uuid);
                    ResultSet results = ps.executeQuery();
                    if (results.next()) {
                        if (results.getString("ip_address").contains(",")) {
                            String[] iplist = results.getString("ip_address").split(",");
                            return iplist[iplist.length - 1];
                        }
                        return results.getString("ip_address");
                    }

                    PreparedStatement ps1 = connection.prepareStatement(
                            "SELECT target_ip_address FROM lolbans_punishments WHERE target_uuid = ? LIMIT 1");
                    ps.setString(1, uuid);
                    ResultSet results1 = ps1.executeQuery();
                    if (results1.next()) {
                        if (results1.getString("target_ip_address").contains(",")) {
                            String[] iplist = results1.getString("target_ip_address").split(",");
                            return iplist[iplist.length - 1];
                        }
                        return results1.getString("target_ip_address");
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
