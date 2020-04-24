package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import org.bukkit.configuration.file.FileConfiguration;

import com.ristexsoftware.lolbans.Main;

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
        DiscordUtil du = new DiscordUtil(config.getString("Discord.Webhook"), config.getString("Discord.ReportWebhook"));
        du.UseSimplifiedMessage = config.getBoolean("Discord.UseSimplifiedMessage");
        du.WebhookProfilePicture = config.getString("Discord.WebhookProfilePicture");
    }
}