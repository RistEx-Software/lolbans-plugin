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
        return config;
    }

    @Override
    public void saveConfig() {
        try {
            config.save(configFile);
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