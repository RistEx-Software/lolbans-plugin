package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Arrays;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import com.ristexsoftware.lolbans.Main;

public class Messages
{
    private Main self = Main.getPlugin(Main.class);
    private File CustomConfigFile;
    private FileConfiguration CustomConfig;
    private static Messages localself = null;

    // Everything else
    public static String Prefix;
    public static String NetworkName;
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

    // Discord simplified messages
    public static String SimpMessageBan;
    public static String SimpMessageUnban;
    public static String SimpMessageSilentBan;
    public static String SimpMessageSilentUnban;
    public static String SimpMessageMute;
    public static String SimpMessageUnmute;
    public static String SimpMessageSilentMute;
    public static String SimpMessageSilentUnmute;
    public static String SimpMessageKick;
    public static String SimpMessageSilentKick;
    public static String SimpMessageWarn;
    public static String SimpMessageSilentWarn;

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

            // TODO: ALL theses need default values!

            // Messages
            Messages.Prefix = this.CustomConfig.getString("Prefix", "[lolbans] ").replace("&", "\u00A7");
            Messages.NetworkName = Messages._Translate(this.CustomConfig.getString("NetworkName", "My Network"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.InvalidSyntax = Messages._Translate(this.CustomConfig.getString("InvalidSyntax", "&cInvalid Syntax!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            
            // Discord simplified message
            DiscordUtil.SimpMessageBan = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageBan"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageUnban = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageUnban"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageSilentBan = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageSilentBan"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageSilentUnban = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageSilentUnban"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageMute = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageMute"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageUnmute = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageUnmute"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageSilentMute = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageSilentMute"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageSilentUnmute = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageSilentUnmute"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageKick = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageKick"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageSilentKick = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageSilentKick"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageWarn = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageWarn"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            DiscordUtil.SimpMessageSilentWarn = Messages._Translate(this.CustomConfig.getString("Discord.SimpMessageSilentWarn"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));

            // Ban messages
            Messages.SilentBanAnnouncement = Messages._Translate(this.CustomConfig.getString("Ban.SilentBanAnnouncement"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.BanAnnouncement = Messages._Translate(this.CustomConfig.getString("Ban.BanAnnouncement"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.UnbanAnnouncment = Messages._Translate(this.CustomConfig.getString("Ban.UnbanAnnouncment"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.SilentUnbanAnnouncment = Messages._Translate(this.CustomConfig.getString("Ban.SilentUnbanAnnouncment"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));

            // Mute messages
            Messages.SilentUnmuteAnnouncment = Messages._Translate(this.CustomConfig.getString("Mute.SilentUnmuteAnnouncment"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.UnmuteAnnouncment = Messages._Translate(this.CustomConfig.getString("Mute.UnmuteAnnouncment"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
        } 
        catch (IOException | InvalidConfigurationException e) 
        {
            e.printStackTrace();
        }
    }

    public FileConfiguration GetConfig()
    {
        return this.CustomConfig;
    }

    private static String _Translate(String ConfigMessage, Map<String, String> Variables)
    {
        if (ConfigMessage == null)
            return null;
        
        Variables.put("prefix", Messages.Prefix);
        Variables.put("networkname", Messages.NetworkName);

        return TranslationUtil.Translate(ConfigMessage, "&", Variables);
    }

    public static String Translate(String ConfigNode, Map<String, String> Variables) throws InvalidConfigurationException
    {
        String ConfigMessage = GetMessages().CustomConfig.getString(ConfigNode);
        if (ConfigMessage == null)
            throw new InvalidConfigurationException("Configuration Node is invalid or does not exist: " + ConfigNode);

        return _Translate(ConfigMessage, Variables);
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