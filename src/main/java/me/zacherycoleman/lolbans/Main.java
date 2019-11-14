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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import me.zacherycoleman.lolbans.Commands.BanCommand;
import me.zacherycoleman.lolbans.Commands.HistoryCommand;
import me.zacherycoleman.lolbans.Commands.UnbanCommand;
import me.zacherycoleman.lolbans.Listeners.ConnectionListeners;
import me.zacherycoleman.lolbans.Runnables.QueryRunnable;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Utils.TimeUtil;

import java.sql.*;
import java.util.UUID;

public final class Main extends JavaPlugin
{
    private String dbhost = "";
    private String dbname = "";
    private String dbusername = "";
    private String dbpassword = "";
    private Integer dbport = 3306;
    private QueryRunnable CheckThread;

    public String DiscordWebhook = "";
    public String NetworkName = "";
    public Connection connection;

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

    public boolean IsPlayerBanned(OfflinePlayer user)
    {
        try 
        {
            PreparedStatement ps = this.connection.prepareStatement("SELECT 1 FROM BannedPlayers WHERE UUID = ? LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public OfflinePlayer FindPlayerByBanID(String BanID)
    {
        // Try stupid first. If the BanID is just a nickname, then avoid DB queries.
        OfflinePlayer op = Bukkit.getOfflinePlayer(BanID);
        if (op != null)
            return op;
        
        try 
        {
            PreparedStatement ps = this.connection.prepareStatement("SELECT UUID FROM BannedPlayers WHERE BanID = ? LIMIT 1");
            ps.setString(1, BanID);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next())
            {
                UUID uuid = UUID.fromString(rs.getString("UUID"));
                op = Bukkit.getOfflinePlayer(uuid);

                // Try and query from history
                if (op == null)
                {
                    ps = this.connection.prepareStatement("SELECT UUID FROM BannedHistory WHERE BanID = ? LIMIT 1");
                    ps.setString(1, BanID);
                    rs = ps.executeQuery();

                    if (rs.next())
                    {
                        uuid = UUID.fromString(rs.getString("UUID"));
                        op = Bukkit.getOfflinePlayer(uuid);
                        return op;
                    }
                }
                else
                    return op;
            }
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        
        return null;
    }

    public void KickPlayer(String sender, Player target, String BanID, String reason, Timestamp BanTime)
    {
        StringBuilder builder = new StringBuilder();


        // if user is perma banned (this needs an actal check...)
        // Username Section of the message.
        builder.append(ChatColor.RED);
        builder.append("The Account ");
        builder.append(ChatColor.GRAY);
        builder.append(target.getName());
        builder.append(ChatColor.RED);
        if (BanTime == null)
            builder.append(" is INDEFINITELY suspended from ");
        else
            builder.append(" is temporarily suspended from ");
        builder.append(ChatColor.GRAY);
        builder.append(this.NetworkName + "\n\n");

        // Who banned player.
        builder.append(ChatColor.GRAY);
        builder.append("You were banned by: ");
        builder.append(ChatColor.WHITE);
        builder.append(sender + "\n");
        
        // Reason for the ban
        builder.append(ChatColor.GRAY);
        builder.append("Reason: ");
        builder.append(ChatColor.WHITE);
        builder.append(reason);
        if (BanTime != null)
        {
            builder.append("\n");
            builder.append(ChatColor.GRAY);
            builder.append("Expiry: ");
            builder.append(ChatColor.WHITE);
            builder.append(TimeUtil.TimeString(BanTime));
        }
        
    
        builder.append("\n\n");

        // Ban ID section
        builder.append(ChatColor.GRAY);
        builder.append("Ban ID: ");
        builder.append(ChatColor.WHITE);
        builder.append("#" + BanID + "\n");

        // Sharing the banid is bad, mm'kay?
        builder.append(ChatColor.GRAY);
        builder.append("Sharing your Ban ID may affect the result of your ban appeal.");

        // and kick the player
        target.kickPlayer(builder.toString());
    }

    @Override
    public void onEnable()
    {    
        // Plugin startup logic

        // Creating config folder, and adding config to it.
        if (!this.getDataFolder().exists())
        {
            getLogger().info("Error: No folder for uwubans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().info("the folder for @w@0w0OwODwDQwQ~w~bans was created successfully!");
        }

        // Registering config strings
        // Admin strings
        this.dbhost = getConfig().getString("dbhost");
        this.dbport = getConfig().getInt("dbport");
        this.dbname = getConfig().getString("dbname");
        this.dbusername = getConfig().getString("dbusername");
        this.dbpassword = getConfig().getString("dbpassword");
        DiscordUtil.Webhook = getConfig().getString("DiscordWebhook");

        // Messages
        this.NetworkName = getConfig().getString("NetworkName");
        
        try 
        {
            this.openConnection();

            // Ensure Our tables are created.
            PreparedStatement ps2 = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedHistory (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, UnbanReason TEXT, UnbanExecutioner varchar(17), TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)");
            PreparedStatement ps = this.connection.prepareStatement("CREATE TABLE IF NOT EXISTS BannedPlayers (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, UUID varchar(36) NOT NULL, PlayerName varchar(17) NOT NULL, Reason TEXT NULL, Executioner varchar(17) NOT NULL, BanID varchar(20) NOT NULL, TimeBanned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, Expiry TIMESTAMP NULL)");
            ps.execute();
            ps2.execute();
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

        // Schedule a repeating task to delete expired bans.
        // TODO: Make config option.
        this.CheckThread = new QueryRunnable();
        this.CheckThread.runTaskTimerAsynchronously(this, 0L, 1200L);
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
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
}
