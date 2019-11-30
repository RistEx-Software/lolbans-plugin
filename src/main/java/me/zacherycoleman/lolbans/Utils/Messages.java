package me.zacherycoleman.lolbans.Utils; // Zachery's package owo

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import me.zacherycoleman.lolbans.Main;

public class Messages
{
    private Main self = Main.getPlugin(Main.class);
    private File CustomConfigFile;
    private FileConfiguration CustomConfig;
    private static Messages localself = null;

    public static String Prefix;
    public static String ServerError;
    public static String BanwaveStart;
    public static String TempBanMessage;
    public static String PermBanMessage;
    public static String CannotBanSelf;
    public static String BanAnnouncment;
    public static String UnbanAnnouncment;
    public static String SilentUnbanAnnouncment;
    public static String InvalidSyntax;
    public static String CannotAddSelf;
    public static String PlayerDoesntExist;
    public static String PlayerIsBanned;
    public static String PlayerIsInBanWave;
    public static String BannedPlayersInBanWave;
    public static String SilentWarnAnnouncment;
    public static String WarnAnnouncment;
    public static String WarnedMessage;
    public static String WarnKickMessage;

    // Initialized by our GetMessages() function.
    protected Messages()
    {
        CustomConfigFile = new File(self.getDataFolder(), "messages.yml");
        if (!CustomConfigFile.exists()) 
        {
            CustomConfigFile.getParentFile().mkdirs();
            self.saveResource("messages.yml", false);
        }

        this.Reload();
    }

    public static Messages GetMessages()
    {
        if (Messages.localself == null)
            Messages.localself = new Messages();
        return Messages.localself;
    }

    public void Reload()
    {
        FileConfiguration fc = new YamlConfiguration();
        try 
        {
            fc.load(CustomConfigFile);
            this.CustomConfig = fc;
        } 
        catch (IOException | InvalidConfigurationException e) 
        {
            e.printStackTrace();
        }

        // Messages
        Messages.Prefix = this.CustomConfig.getString("Prefix").replace("&", "\u00A7");
        //Messages.TempBanMessage = this.CustomConfig.getString("Ban.TempBanMessage");
        //Messages.PermBanMessage = this.CustomConfig.getString("Ban.PermBanMessage");
        //Messages.CannotBanSelf = this.CustomConfig.getString("Ban.CannotBanSelf");
        //Messages.BanAnnouncment = this.CustomConfig.getString("Ban.BanAnnouncment");
        //Messages.UnbanAnnouncment = this.CustomConfig.getString("Ban.UnbanAnnouncment");
        //Messages.SilentUnbanAnnouncment = this.CustomConfig.getString("Ban.SilentUnbanAnnouncment");
        Messages.InvalidSyntax = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("InvalidSyntax"));
        Messages.ServerError = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("ServerError"));
        Messages.BanwaveStart = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("BanwaveStart"));
        //Messages.CannotAddSelf = this.CustomConfig.getString("Banwave.CannotAddSelf");
        //Messages.PlayerDoesntExist = this.CustomConfig.getString("PlayerDoesntExist");
        //Messages.PlayerIsBanned = this.CustomConfig.getString("Ban.PlayerIsBanned"); 
        //Messages.PlayerIsInBanWave = this.CustomConfig.getString("Banwave.PlayerIsInBanWave"); 
        //Messages.BannedPlayersInBanWave = this.CustomConfig.getString("Banwave.BannedPlayersInBanWave");
        //Messages.SilentWarnAnnouncment = this.CustomConfig.getString("Warn.SilentWarnAnnouncment");
        //Messages.WarnAnnouncment = this.CustomConfig.getString("Warn.WarnAnnouncment");
        //Messages.WarnedMessage = this.CustomConfig.getString("Warn.WarnedMessage");
        //Messages.WarnKickMessage = this.CustomConfig.getString("Warn.WarnKickMessage");
    }

    public FileConfiguration GetConfig()
    {
        return this.CustomConfig;
    }

    public String Translate(String ConfigNode, Map<String, String> Variables)
    {
        return TranslationUtil.Translate(this.CustomConfig.getString(ConfigNode), "&", Variables);
    }
}