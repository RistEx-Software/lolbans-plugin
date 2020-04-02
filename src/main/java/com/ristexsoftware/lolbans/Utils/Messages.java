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
    public static String Website;
    public static String ServerError;
    public static String InvalidSyntax;

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

            // Messages
            Messages.Prefix = this.CustomConfig.getString("Prefix", "[lolbans] ").replace("&", "\u00A7");
            Messages.NetworkName = Messages._Translate(this.CustomConfig.getString("NetworkName", "My Network"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.Website = Messages._Translate(this.CustomConfig.getString("Website", "YourWebsiteHere.com"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.ServerError = Messages._Translate(this.CustomConfig.getString("ServerError", "The server encountered an error!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.InvalidSyntax = Messages._Translate(this.CustomConfig.getString("InvalidSyntax", "&cInvalid Syntax!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
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