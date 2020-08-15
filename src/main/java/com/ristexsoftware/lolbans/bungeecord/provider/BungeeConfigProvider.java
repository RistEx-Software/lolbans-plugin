package com.ristexsoftware.lolbans.bungeecord.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.base.Charsets;
import com.ristexsoftware.knappy.configuration.InvalidConfigurationException;
import com.ristexsoftware.knappy.configuration.file.FileConfiguration;
import com.ristexsoftware.knappy.configuration.file.YamlConfiguration;
import com.ristexsoftware.lolbans.api.provider.ConfigProvider;
import com.ristexsoftware.lolbans.bungeecord.Main;

import net.md_5.bungee.config.ConfigurationProvider;

public class BungeeConfigProvider implements ConfigProvider {
    private File configFile = new File(getDataFolder(), "config.yml");
    private FileConfiguration config = new YamlConfiguration();
    private FileConfiguration newConfig = null;

    public BungeeConfigProvider() {
        try {
            config.load(getConfigFile());
        } catch(IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public File getDataFolder() {
        // System.out.println(main.getDataFolder());
        return Main.getPlugin().getDataFolder();
    }

    @Override
    public FileConfiguration getConfig() {
        FileConfiguration fc = new YamlConfiguration();
        try {
            fc.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return null;
        }

        return fc;
    }

    @Override
    public void saveConfig() {
        try {
            final net.md_5.bungee.config.Configuration configuration = ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).load(configFile);
            ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).save(configuration, new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reloadConfig() {
        newConfig = YamlConfiguration.loadConfiguration(configFile);

        final InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream == null) {
            return;
        }

        newConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
    } 
}