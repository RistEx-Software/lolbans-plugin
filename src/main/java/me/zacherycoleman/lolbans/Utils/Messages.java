package me.zacherycoleman.lolbans.Utils; // Zachery's package owo

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Arrays;
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
    public static String SilentBanAnnouncement;
    public static String BanAnnouncement;
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
    public static String SimplifiedMessageSilent;
    public static String SimplifiedMessage;
    public static String SilentUnmuteAnnouncment;
    public static String UnmuteAnnouncment;
    public static String TempIPBanMessage;
    public static String PermIPBanMessage;
    public static String IPBanAnnouncement;
    public static String SilentIPBanAnnouncement;
    public static String UnIPbanAnnouncement;
    public static String SilentUnIPbanAnnouncement;
    public static String CannotIPBanSelf;
    public static String IPIsBanned;
    public static String IPIsNotBanned;
    public static String Insanity;

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
        Messages.SimplifiedMessageSilent = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Discord.SimplifiedMessageSilent"));
        Messages.SimplifiedMessage = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Discord.SimplifiedMessage"));
        Messages.SilentBanAnnouncement = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Ban.SilentBanAnnouncement"));
        Messages.BanAnnouncement = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Ban.BanAnnouncement"));
        Messages.UnbanAnnouncment = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Ban.UnbanAnnouncment"));
        Messages.SilentUnbanAnnouncment = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Ban.SilentUnbanAnnouncment"));
        Messages.SilentUnmuteAnnouncment = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Mute.SilentUnmuteAnnouncment"));
        Messages.UnmuteAnnouncment = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("Mute.UnmuteAnnouncment"));
        //Messages.TempIPBanMessage = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.TempIPBanMessage"));
        //Messages.PermIPBanMessage = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.PermIPBanMessage"));
        //Messages.IPBanAnnouncement = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.IPBanAnnouncement"));
        //Messages.SilentIPBanAnnouncement = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.SilentIPBanAnnouncement"));
        //Messages.UnIPbanAnnouncement = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.UnIPbanAnnouncement"));
        //Messages.SilentUnIPbanAnnouncement = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.SilentUnIPbanAnnouncement"));
        //Messages.IPIsBanned = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.IPIsBanned"));
        //Messages.Insanity = TranslationUtil.TranslateColors("&", this.CustomConfig.getString("IPBan.Insanity"));
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

    public static String ConcatenateRest(String[] args, int offset, String delim)
    {
        return args.length > 1 ? String.join(delim, Arrays.copyOfRange(args, offset, args.length)) : args[1];
    }

    public static String ConcatenateRest(String[] args, int offset)
    {
        return Messages.ConcatenateRest(args, offset, " ");
    }
}