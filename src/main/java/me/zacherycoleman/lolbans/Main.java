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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Commands.BanCommand;
import me.zacherycoleman.lolbans.Commands.BanWaveCommand;
import me.zacherycoleman.lolbans.Commands.HistoryCommand;
import me.zacherycoleman.lolbans.Commands.UnbanCommand;
import me.zacherycoleman.lolbans.Listeners.ConnectionListeners;
import me.zacherycoleman.lolbans.Runnables.QueryRunnable;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;

import java.io.File;
import java.sql.*;
// welcome.
public final class Main extends JavaPlugin
{
    private String dbhost = "";
    private String dbname = "";
    private String dbusername = "";
    private String dbpassword = "";
    private Integer dbport = 3306;
    private QueryRunnable CheckThread;

    public String DiscordWebhook;
    public String Prefix;
    public String TempBanMessage;
    public String PermBanMessage;
    public Long QueryUpdateLong;
    public String BanAnnouncment;
    public String SilentBanAnnouncment;
    public String UnbanAnnouncment;
    public String SilentUnbanAnnouncment;
    public String CannotBanSelf;

    public Connection connection;
    public static YamlConfiguration LANG;
    public static File LANG_FILE;

    public void openConnection() throws SQLException
    {
        if (connection != null && !connection.isClosed())
            return;

        synchronized (this)
        {
            if (connection != null && !connection.isClosed())
                return;

            connection =
                    DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&failOverReadOnly=false&maxReconnects=10", 
                                    this.dbhost, this.dbport, this.dbname), this.dbusername, this.dbpassword);
        }
    }

    @Override
    public void onEnable()
    {    
        // Plugin startup logic

        

        // Creating config folder, and adding config to it.
        if (!this.getDataFolder().exists())
        {
            // uwubans*
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().info("the folder for lolbans was created successfully!");
        }


        // Registering config strings
        // Admin strings
        this.dbhost = getConfig().getString("dbhost");
        this.dbport = getConfig().getInt("dbport");
        this.dbname = getConfig().getString("dbname");
        this.dbusername = getConfig().getString("dbusername");
        this.dbpassword = getConfig().getString("dbpassword");
        DiscordUtil.Webhook = getConfig().getString("DiscordWebhook");
        this.TempBanMessage = getConfig().getString("TempBanMessage");
        this.PermBanMessage = getConfig().getString("PermMessage");
        this.QueryUpdateLong = getConfig().getLong("QueryUpdateLong");

        // Messages

        this.Prefix = getConfig().getString("Prefix").replace("&", "§");
        this.CannotBanSelf = getConfig().getString("CannotBanSelf");
        this.BanAnnouncment = getConfig().getString("BanAnnouncment");
        this.SilentUnbanAnnouncment = getConfig().getString("BanAnnouncment");
        this.UnbanAnnouncment = getConfig().getString("UnbanAnnouncment");
        this.SilentUnbanAnnouncment = getConfig().getString("SilentUnbanAnnouncment");
        
        try 
        {
            this.openConnection();

            // Ensure Our tables are created.
            PreparedStatement ps = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)");
            PreparedStatement ps2 = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, UnbanReason TEXT, UnbanExecutioner varchar(17), TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)");
            PreparedStatement ps3 = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BanWave (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeAdded TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)");
            ps.execute();
            ps2.execute();
            ps3.execute();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        Bukkit.getPluginManager().registerEvents(new ConnectionListeners(), this);
        this.getCommand("ban").setExecutor(new BanCommand());
        this.getCommand("unban").setExecutor(new UnbanCommand());
        this.getCommand("history").setExecutor(new HistoryCommand());
        this.getCommand("h").setExecutor(new HistoryCommand());
        this.getCommand("clearhistory").setExecutor(new HistoryCommand());
        this.getCommand("ch").setExecutor(new HistoryCommand());
        this.getCommand("banwave").setExecutor(new BanWaveCommand());

        // Schedule a repeating task to delete expired bans.
        this.CheckThread = new QueryRunnable();
        this.CheckThread.runTaskTimerAsynchronously(this, 20L, QueryUpdateLong * 20L);
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
        reloadConfig();
        CheckThread.cancel();
        try 
        {
            this.connection.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void KickPlayer(String string, Player player, String string2, String string3, Timestamp timestamp) 
    {
    }
}
