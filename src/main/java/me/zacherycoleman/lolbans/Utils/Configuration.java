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
    public static Integer MaxReconnects = 5;

    public static Long QueryUpdateLong;

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
        // Database
        Configuration.dbhost = config.getString("database.host");
        Configuration.dbport = config.getInt("database.port");
        Configuration.dbname = config.getString("database.name");
        Configuration.dbusername = config.getString("database.username");
        Configuration.dbpassword = config.getString("database.password");
        Configuration.MaxReconnects = config.getInt("database.MaxReconnects");
        Configuration.QueryUpdateLong = config.getLong("database.QueryUpdate");

        // Discord
        DiscordUtil.Webhook = config.getString("Discord.Webhook");
        DiscordUtil.UseSimplifiedMessage = config.getBoolean("Discord.UseSimplifiedMessage");
/*         DiscordUtil.SimplifiedMessage = config.getString("Discord.SimplifiedMessage");
        DiscordUtil.SimplifiedMessageUnban = config.getString("Discord.SimplifiedMessageUnban");
        DiscordUtil.SimplifiedMessageSilent = config.getString("Discord.SimplifiedMessageSilent");
        DiscordUtil.SimplifiedMessageSilentUnban = config.getString("Discord.SimplifiedMessageUnbanSilent"); */
        DiscordUtil.WebhookProfilePicture = config.getString("Discord.WebhookProfilePicture");
        DiscordUtil.ReportWebhook = config.getString("Discord.ReportWebhook");
    }
}