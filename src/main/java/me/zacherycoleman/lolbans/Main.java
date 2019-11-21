package me.zacherycoleman.lolbans;

/*
                    The Wicked Dick Witch of the West!
                            -    .|||||.
                                |||||||||
                        -      ||||||  .
                            -  ||||||   >
                                ||||||| -/
                        --   ||||||'(
                        -       .'      \
                            .-'    | | |
                            /        \ \ \
            --        -  |      `---:.`.\
            ____________._>           \\_\\____ ,--.__
--    ,--""           /    `-   .     |)_)    '\     '\
    /  "             |      .-'     /          \      '\
    ,/                  \           .'            '\     |
    | "   "   "          \         /                '\,  /
    |           " , =_____`-.   .-'_________________,--""
- |  "    "    /"/'      /\>-' ( <
    \  "      ",/ /    -  ( <    |\_)
    \   ",",_/,-'        |\_)
-- -'-;.__:-'  Watch out before she steals your side-hoe!
*/

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Commands.AcceptCommand;
import me.zacherycoleman.lolbans.Commands.BanCommand;
import me.zacherycoleman.lolbans.Commands.BanWaveCommand;
import me.zacherycoleman.lolbans.Commands.HistoryCommand;
import me.zacherycoleman.lolbans.Commands.UnbanCommand;
import me.zacherycoleman.lolbans.Commands.WarnCommand;
import me.zacherycoleman.lolbans.Commands.BoxCommand;
import me.zacherycoleman.lolbans.Listeners.ConnectionListeners;
import me.zacherycoleman.lolbans.Listeners.MovementListener;
import me.zacherycoleman.lolbans.Listeners.PlayerEventListener;
import me.zacherycoleman.lolbans.Runnables.QueryRunnable;
import me.zacherycoleman.lolbans.Utils.Configuration;
import me.zacherycoleman.lolbans.Utils.User;
import me.zacherycoleman.lolbans.Hacks.Hacks;

import java.sql.*;
import java.util.HashMap;
import java.util.UUID;
// welcome.
public final class Main extends JavaPlugin
{
    public static HashMap<UUID, User> USERS = new HashMap<UUID, User>();
    
    public Connection connection;
    private QueryRunnable CheckThread;

    public void openConnection() throws SQLException
    {
        if (connection != null && !connection.isClosed())
            return;

        synchronized (this)
        {
            if (connection != null && !connection.isClosed())
                return;

            connection =
                    DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&failOverReadOnly=false&maxReconnects=%d", 
                                    Configuration.dbhost, Configuration.dbport, Configuration.dbname, Configuration.MaxReconnects), Configuration.dbusername, Configuration.dbpassword);
        }
    }

    @Override
    public void onEnable()
    {    
        // Plugin startup logic
        new Configuration(this.getConfig());
        
        // Creating config folder, and adding config to it.
        if (!this.getDataFolder().exists())
        {
            // uwubans*
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().info("the folder for lolbans was created successfully!");
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from having errors.
            return;
        }

        try 
        {
            this.openConnection();
        }
        catch (SQLException e)
        {
            //e.printStackTrace();
            getLogger().severe("Cannot connect to database, ensure your database is setup correctly and restart the server.");
            // Just exit and let the user figure it out.
            return;
        }

        // Ensure Our tables are created.
        try
        {
            this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, UnbanReason TEXT, UnbanExecutioner varchar(17), TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BanWave (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)").execute();
            this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS Warnings (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, WarnID varchar(20) NOT NULL, Accepted boolean, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            getLogger().severe("Cannot create database tables, please ensure your SQL user has the correct permissions.");
            return;
        }

        Bukkit.getPluginManager().registerEvents(new ConnectionListeners(), this);
        Bukkit.getPluginManager().registerEvents(new MovementListener(), this);
        //Bukkit.getPluginManager().registerEvents(new PlayerEventListener(), this);

        // Register commands
        this.getCommand("ban").setExecutor(new BanCommand());
        this.getCommand("unban").setExecutor(new UnbanCommand());
        this.getCommand("history").setExecutor(new HistoryCommand());
        this.getCommand("h").setExecutor(new HistoryCommand());
        this.getCommand("clearhistory").setExecutor(new HistoryCommand());
        this.getCommand("ch").setExecutor(new HistoryCommand());
        this.getCommand("banwave").setExecutor(new BanWaveCommand());
        this.getCommand("warn").setExecutor(new WarnCommand());
        this.getCommand("accept").setExecutor(new AcceptCommand());

        // DEBUG
        this.getCommand("box").setExecutor(new BoxCommand());

        // Used if the admin does /reload confirm
        for (Player p : Bukkit.getOnlinePlayers())
            Main.USERS.put(p.getUniqueId(), new User(p));

        // Schedule a repeating task to delete expired bans.
        this.CheckThread = new QueryRunnable();
        this.CheckThread.runTaskTimerAsynchronously(this, 20L, Configuration.QueryUpdateLong * 20L);

        // Run our hacks
        Hacks.HackIn();
    }

    @Override
    public void onDisable()
    {
        // Unregister our hacks.
        Hacks.GetCaught();
        // Save our config values
        reloadConfig();
        // Terminate our thread.
        if (CheckThread != null)
            CheckThread.cancel();
        
        // Close the database connection (if open)
        if (this.connection != null)
        {
            try 
            {
                this.connection.close();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
}
