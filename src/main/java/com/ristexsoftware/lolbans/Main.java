package com.ristexsoftware.lolbans;

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

import com.ristexsoftware.lolbans.Commands.Warn.AcceptCommand;
import com.ristexsoftware.lolbans.Commands.Warn.WarnCommand;
import com.ristexsoftware.lolbans.Commands.Ban.BanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.UnbanCommand;
import com.ristexsoftware.lolbans.Commands.Ban.BanWaveCommand;
import com.ristexsoftware.lolbans.Commands.Ban.IPBanCommand;
import com.ristexsoftware.lolbans.Commands.History.HistoryCommand;
import com.ristexsoftware.lolbans.Commands.Misc.BroadcastCommand;
import com.ristexsoftware.lolbans.Commands.Misc.KickCommand;
import com.ristexsoftware.lolbans.Commands.Misc.ReportCommand;
import com.ristexsoftware.lolbans.Commands.Mute.UnmuteCommand;
import com.ristexsoftware.lolbans.Commands.Mute.MuteCommand;
import com.ristexsoftware.lolbans.Listeners.AsyncChatListener;
import com.ristexsoftware.lolbans.Listeners.ConnectionListeners;
import com.ristexsoftware.lolbans.Listeners.PlayerEventListener;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Hacks.Hacks;

import inet.ipaddr.IPAddressString;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.UUID;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// welcome.
public final class Main extends JavaPlugin
{
    public static HashMap<UUID, User> USERS = new HashMap<UUID, User>();
    public static List<IPAddressString> BannedAddresses = new Vector<IPAddressString>();
    // For some reason using Futures with the Bukkit Async scheduler doesn't work.
    // Instead of relying on dumb bukkit APIs to get tasks done, we use a thread pool of
    // our own control to get whatever we want done.
    public static ExecutorService pool = Executors.newFixedThreadPool(3);

    // Whether or not the chat has been muted globally.
    public boolean ChatMuted = false;
    
    // Our database connection, here for legacy reasons (it should be in DatabaseUtil)
    public Connection connection;
    
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
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from having errors.
            return;
        }

        if (!(new File(this.getDataFolder(), "config.yml").exists()))
        {
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from having errors.
            return;
        }

        // Initialize our database connections.
        if (!DatabaseUtil.InitializeDatabase())
            return;

        // Make sure our messages file exists
        Messages.GetMessages();

        Bukkit.getPluginManager().registerEvents(new ConnectionListeners(), this);
        Bukkit.getPluginManager().registerEvents(new AsyncChatListener(), this);

        // Register commands
        this.getCommand("ban").setExecutor(new BanCommand());
        this.getCommand("ipban").setExecutor(new IPBanCommand());
        this.getCommand("unban").setExecutor(new UnbanCommand());
        this.getCommand("history").setExecutor(new HistoryCommand());
        this.getCommand("clearhistory").setExecutor(new HistoryCommand());
        this.getCommand("banwave").setExecutor(new BanWaveCommand());
        this.getCommand("warn").setExecutor(new WarnCommand());
        this.getCommand("accept").setExecutor(new AcceptCommand());
        this.getCommand("mute").setExecutor(new MuteCommand());
        this.getCommand("unmute").setExecutor(new UnmuteCommand());
        this.getCommand("kick").setExecutor(new KickCommand());
        this.getCommand("broadcast").setExecutor(new BroadcastCommand());
        this.getCommand("report").setExecutor(new ReportCommand());
        
        // Used if the admin does /reload confirm
        for (Player p : Bukkit.getOnlinePlayers())
            Main.USERS.put(p.getUniqueId(), new User(p));

        // Run our hacks
        Hacks.HackIn(this);
    }

    @Override
    public void onDisable()
    {
        // Unregister our hacks.
        Hacks.GetCaught();
        // Save our config values
        reloadConfig();
        // Close out or database.
        DatabaseUtil.Terminate();
    }
}