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
        Messages.InvalidSyntax = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("InvalidSyntax"));
        Messages.ServerError = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("ServerError"));
    }

    public FileConfiguration GetConfig()
    {
        return this.CustomConfig;
    }

    public String Translate(String ConfigNode, Map<String, String> Variables) throws InvalidConfigurationException
    {
        String ConfigMessage = this.CustomConfig.getString(ConfigNode);
        if (ConfigMessage == null)
            throw new InvalidConfigurationException("Configuration Node is invalid or does not exist: " + ConfigNode);

        return TranslationUtil.Translate(ConfigMessage, "&", Variables);
    }
}