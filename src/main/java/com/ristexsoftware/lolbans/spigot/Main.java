/* 
 *  LolBans - The advanced banning system for Minecraft
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

package com.ristexsoftware.lolbans.spigot;

import java.io.File;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.database.Database;
import com.ristexsoftware.lolbans.spigot.listeners.Connection;
import com.ristexsoftware.lolbans.spigot.utils.Configuration;
import com.ristexsoftware.lolbans.spigot.utils.Messages;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        new Configuration(this.getConfig());
                // This is dumb, plugman somehow breaks lolbans
        // but whatever, you shouldn't be using plugman anyway...
        if (getServer().getPluginManager().getPlugin("PlugMan") != null)
            getLogger().warning(
                    "PlugMan detected! This WILL cause issues with LolBans, please consider restarting the server to update plugins!");

        // Creating config folder, and adding config to it.
        if (!this.getDataFolder().exists()) {
            // uwubans*
            getLogger().info("Error: No folder for lolbans was found! Creating...");
            this.getDataFolder().mkdirs();
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            return;
        }

        if (!(new File(this.getDataFolder(), "config.yml").exists())) {
            this.saveDefaultConfig();
            getLogger().severe("Please configure lolbans and restart the server! :)");
            // They're not gonna have their database setup, just exit. It stops us from
            // having errors.
            return;
        }

        // Initialize our database connections.
        if (!Database.InitializeDatabase(Configuration.dbhost, Configuration.dbusername, Configuration.dbpassword, Configuration.dbname, Configuration.dbport, Configuration.MaxReconnects, Configuration.QueryUpdateLong))
            return;

        // Make sure our messages file exists
        Messages.GetMessages();

        getServer().getPluginManager().registerEvents(new Connection(), this);
        
        getLogger().info("hello");
        LolBans.getLogger().info("hello 2!");
    }

    @Override
    public void onDisable() {
        // Save our config values
        reloadConfig();
        // Close out or database.
        Database.Terminate();
    }
}