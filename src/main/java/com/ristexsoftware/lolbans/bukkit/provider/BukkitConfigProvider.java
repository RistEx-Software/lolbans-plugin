package com.ristexsoftware.lolbans.bukkit.provider;

import java.io.File;
import java.io.IOException;

import com.ristexsoftware.lolbans.bukkit.Main;
import com.ristexsoftware.knappy.configuration.InvalidConfigurationException;
import com.ristexsoftware.knappy.configuration.file.FileConfiguration;
import com.ristexsoftware.knappy.configuration.file.YamlConfiguration;
import com.ristexsoftware.lolbans.api.provider.ConfigProvider;

public class BukkitConfigProvider implements ConfigProvider {

    private FileConfiguration config = new YamlConfiguration();

    public BukkitConfigProvider() {
        try {
            config.load(getConfigFile());
        } catch(IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public File getDataFolder() {
        return Main.getPlugin().getDataFolder();
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public void saveConfig() {
        Main.getPlugin(Main.class).saveConfig();
    }

    @Override
    public void reloadConfig() {
        Main.getPlugin(Main.class).reloadConfig();
    }   
}