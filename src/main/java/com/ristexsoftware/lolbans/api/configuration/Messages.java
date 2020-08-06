/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.api.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.configuration.file.FileConfiguration;
import com.ristexsoftware.lolbans.api.configuration.file.YamlConfiguration;
import com.ristexsoftware.lolbans.api.utils.Translation;

import lombok.Getter;

public class Messages {
    private static LolBans self = LolBans.getPlugin();
    private static File CustomConfigFile;
    private static FileConfiguration CustomConfig;
    private static Messages localself = null;

    // Everything else
    public static String prefix;
    public static String networkName;
    public static String website;
    public static String serverError;
    public static String invalidSyntax;
    public static boolean Discord;

    // Initialized by our GetMessages() function.
    protected Messages() {
        String TranslationFile = self.getConfig().getString("general.translation-file", "messages.en_us.yml");
        CustomConfigFile = new File(self.getDataFolder(), TranslationFile);
        if (!CustomConfigFile.exists()) {
            CustomConfigFile.getParentFile().mkdirs();
            self.saveResource(TranslationFile, false);
        }

        reload();
    }

    /**
     * Get the messages object associated with messages.yml
     * 
     * @return A Messages object referencing the messages.yml file.
     */
    public static Messages getMessages() {
        if (Messages.localself == null)
            Messages.localself = new Messages();
        return Messages.localself;
    }

    /**
     * Reload the messages.yml file and update the internal configuration values.
     */
    public static void reload() {
        FileConfiguration fc = new YamlConfiguration();
        try {
            fc.load(CustomConfigFile);
            CustomConfig = fc;

            // Messages
            Messages.prefix = CustomConfig.getString("prefix", "[lolbans] ").replace("&", "\u00A7");
            Messages.networkName = Messages._translate(CustomConfig.getString("network-name", "My Network"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.website = Messages._translate(CustomConfig.getString("website", "YourWebsiteHere.com"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.serverError = Messages._translate(CustomConfig.getString("server-error", "The server encountered an error!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.invalidSyntax = Messages._translate(CustomConfig.getString("invalid-syntax", "&cInvalid Syntax!"), new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
            Messages.Discord = self.getConfig().getBoolean("discord.enabled", false);
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
    public FileConfiguration getConfig()
    {
        return CustomConfig;
    }

    private static String _translate(String configMessage, Map<String, String> vars)
    {
        if (configMessage == null)
        return null;
        
        vars.put("prefix", Messages.prefix);
        vars.put("networkname", Messages.networkName);
        vars.put("website", Messages.website);

        return Translation.translate(configMessage, "&", vars);
    }
    
    private static String _translateNoColor(String ConfigMessage, Map<String, String> Variables)
    {
        if (ConfigMessage == null)
            return null;
        
        Variables.put("prefix", Messages.prefix);
        Variables.put("networkname", Messages.networkName);
        Variables.put("website", Messages.website);

        return Translation.translateVariables(ConfigMessage, Variables);
    }

    /**
     * Lookup the string from messages.yml, replace the placeholders and convert the color codes into colors
     * then return the resulting string for sending to a minecraft client.
     * @param ConfigNode The config node for the message in messages.yml
     * @param Variables Variables to use for placeholders
     * @return A string to send to the minecraft client with all colors and placeholders converted.
     * @throws InvalidConfigurationException if the message node does not exist in messages.yml
     */
    public static String translate(String ConfigNode, Map<String, String> Variables) throws InvalidConfigurationException
    {
        getMessages();
        String ConfigMessage = Messages.CustomConfig.getString(ConfigNode);
        if (ConfigMessage == null)
        throw new InvalidConfigurationException("Configuration Node is invalid or does not exist: " + ConfigNode);
        
        return _translate(ConfigMessage, Variables);
    }

    /**
     * Lookup a string from the messages.yml file and replace the placeholders with the variables but do not 
     * convert the sequences which may be interpreted as color characters.
     * @param ConfigNode the node from messages.yml to lookup for the string
     * @param Variables Placeholders to replace in the string looked up from messages.yml
     * @return A string with all the placeholders converted
     * @throws InvalidConfigurationException If the messages.yml node does not exist
     */
    public static String translateNc(String ConfigNode, Map<String, String> Variables) throws InvalidConfigurationException
    {
        getMessages();
        String ConfigMessage = Messages.CustomConfig.getString(ConfigNode);
        if (ConfigMessage == null)
            throw new InvalidConfigurationException("Configuration Node is invalid or does not exist: " + ConfigNode);

        return _translateNoColor(ConfigMessage, Variables);
    }

    /**
     * Check if many strings equal a single comparison string
     * @param haystack the string to compare to
     * @param needles things that may match the comparison string
     * @return Whether something matches.
     */
    public static boolean compareMany(String haystack, String[] needles)
    {
        for (String needle : needles)
        {
            if (haystack.equalsIgnoreCase(needle))
                return true;
        }
        
        return false;
    }
}