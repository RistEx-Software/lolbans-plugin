package me.zacherycoleman.lolbans.Utils; // Zachery's package owo

import me.zacherycoleman.lolbans.Runnables.QueryRunnable;

import java.io.File;
import java.sql.Connection;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import me.zacherycoleman.lolbans.Main;

public class Configuration
{
    static Main self = Main.getPlugin(Main.class);
    private static Configuration me;

    public static String dbhost = "";
    public static String dbname = "";
    public static String dbusername = "";
    public static String dbpassword = "";
    public static Integer dbport = 3306;

    public static String DiscordWebhook;
    public static String Prefix;
    public static String TempBanMessage;
    public static String PermBanMessage;
    public static Long QueryUpdateLong;
    public static String BanAnnouncment;
    public static String SilentBanAnnouncment;
    public static String UnbanAnnouncment;
    public static String SilentUnbanAnnouncment;
    public static String CannotBanSelf;
    public static String InvalidSyntax;
    public static String CannotAddSelf;
    public static String PlayerDoesntExist;
    public static String PlayerIsBanned;
    public static String PlayerIsInBanWave;
    public static String BannedPlayersInBanWave;

    public static Connection connection;
    public static YamlConfiguration LANG;
    public static File LANG_FILE;

    public Configuration(FileConfiguration config)
    {
        this.Reload(config);
        Configuration.me = this;
    }

    public static Configuration GetConfig()
    {
        return me;
    }

    public void Reload(FileConfiguration config)
    {
        Configuration.dbhost = config.getString("dbhost");
        Configuration.dbport = config.getInt("dbport");
        Configuration.dbname = config.getString("dbname");
        Configuration.dbusername = config.getString("dbusername");
        Configuration.dbpassword = config.getString("dbpassword");
        DiscordUtil.Webhook = config.getString("DiscordWebhook");
        Configuration.TempBanMessage = config.getString("TempBanMessage");
        Configuration.PermBanMessage = config.getString("PermMessage");
        Configuration.QueryUpdateLong = config.getLong("QueryUpdateLong");

        // Messages
        Configuration.Prefix = config.getString("Prefix").replace("&", "§");
        Configuration.CannotBanSelf = config.getString("CannotBanSelf");
        Configuration.BanAnnouncment = config.getString("BanAnnouncment");
        Configuration.SilentUnbanAnnouncment = config.getString("BanAnnouncment");
        Configuration.UnbanAnnouncment = config.getString("UnbanAnnouncment");
        Configuration.SilentUnbanAnnouncment = config.getString("SilentUnbanAnnouncment");
        Configuration.InvalidSyntax = config.getString("InvalidSyntax");
        Configuration.CannotAddSelf = config.getString("CannotAddSelf");
        Configuration.PlayerDoesntExist = config.getString("PlayerDoesntExist");
        Configuration.PlayerIsBanned = config.getString("PlayerIsBanned"); 
        Configuration.PlayerIsInBanWave = config.getString("PlayerIsInBanWave"); 
        Configuration.BannedPlayersInBanWave = config.getString("BannedPlayersInBanWave");
    }
}