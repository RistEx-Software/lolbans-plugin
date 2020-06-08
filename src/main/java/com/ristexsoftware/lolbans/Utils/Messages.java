package com.ristexsoftware.lolbans.Utils; // Zachery's package owo

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
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
    public static String Website;
    public static String ServerError;
    public static String InvalidSyntax;
    public static boolean Discord;

    // Initialized by our GetMessages() function.
    protected Messages()
    {
        String TranslationFile = self.getConfig().getString("General.TranslationFile", "messages.en_us.yml");
        CustomConfigFile = new File(self.getDataFolder(), TranslationFile);
        if (!CustomConfigFile.exists()) 
        {
            CustomConfigFile.getParentFile().mkdirs();
            self.saveResource(TranslationFile, false);
        }

        this.Reload();
    }

    /**
     * Get the messages object associated with messages.yml
     * @return A Messages object referencing the messages.yml file.
     */
    public static Messages GetMessages()
    {
        if (Messages.localself == null)
            Messages.localself = new Messages();
        return Messages.localself;
    }

    /**
     * Reload the messages.yml file and update the internal configuration values.
     */
    public void Reload()
    {
        FileConfiguration fc = new YamlConfiguration();
        try 
        {
            fc.load(CustomConfigFile);
            this.CustomConfig = fc;

            // Messages
            Messages.Prefix = this.CustomConfig.getString("Prefix", "[lolbans] ").replace("&", "\u00A7");
            Messages.NetworkName = Messages._Translate(this.CustomConfig.getString("NetworkName", "My Network"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.Website = Messages._Translate(this.CustomConfig.getString("Website", "YourWebsiteHere.com"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.ServerError = Messages._Translate(this.CustomConfig.getString("ServerError", "The server encountered an error!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.InvalidSyntax = Messages._Translate(this.CustomConfig.getString("InvalidSyntax", "&cInvalid Syntax!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.Discord = self.getConfig().getBoolean("Discord.Enabled", false);
        } 
        catch (IOException | InvalidConfigurationException e) 
        {
            e.printStackTrace();
        }
    }

    /**
     * Get the configuration object for messages.yml
     * @return {@link org.bukkit.configuration.file.FileConfiguration}
     */
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
        Variables.put("website", Messages.Website);

        return TranslationUtil.Translate(ConfigMessage, "&", Variables);
    }
    
    private static String _TranslateNoColor(String ConfigMessage, Map<String, String> Variables)
    {
        if (ConfigMessage == null)
            return null;
        
        Variables.put("prefix", Messages.Prefix);
        Variables.put("networkname", Messages.NetworkName);
        Variables.put("website", Messages.Website);

        return TranslationUtil.TranslateVariables(ConfigMessage, Variables);
    }

    /**
     * Lookup the string from messages.yml, replace the placeholders and convert the color codes into colors
     * then return the resulting string for sending to a minecraft client.
     * @param ConfigNode The config node for the message in messages.yml
     * @param Variables Variables to use for placeholders
     * @return A string to send to the minecraft client with all colors and placeholders converted.
     * @throws InvalidConfigurationException if the message node does not exist in messages.yml
     */
    public static String Translate(String ConfigNode, Map<String, String> Variables) throws InvalidConfigurationException
    {
        String ConfigMessage = GetMessages().CustomConfig.getString(ConfigNode);
        if (ConfigMessage == null)
            throw new InvalidConfigurationException("Configuration Node is invalid or does not exist: " + ConfigNode);

        return _Translate(ConfigMessage, Variables);
    }

    /**
     * Lookup a string from the messages.yml file and replace the placeholders with the variables but do not 
     * convert the sequences which may be interpreted as color characters.
     * @param ConfigNode the node from messages.yml to lookup for the string
     * @param Variables Placeholders to replace in the string looked up from messages.yml
     * @return A string with all the placeholders converted
     * @throws InvalidConfigurationException If the messages.yml node does not exist
     */
    public static String TranslateNC(String ConfigNode, Map<String, String> Variables) throws InvalidConfigurationException
    {
        String ConfigMessage = GetMessages().CustomConfig.getString(ConfigNode);
        if (ConfigMessage == null)
            throw new InvalidConfigurationException("Configuration Node is invalid or does not exist: " + ConfigNode);

        return _TranslateNoColor(ConfigMessage, Variables);
    }

    /**
     * Check if many strings equal a single comparison string
     * @param haystack the string to compare to
     * @param needles things that may match the comparison string
     * @return Whether something matches.
     */
    public static boolean CompareMany(String haystack, String[] needles)
    {
        for (String needle : needles)
        {
            if (haystack.equalsIgnoreCase(needle))
                return true;
        }
        
        return false;
    }
}