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

package com.ristexsoftware.lolbans.bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.utils.CommandUtil;
import com.ristexsoftware.lolbans.api.utils.ServerType;
import com.ristexsoftware.lolbans.bukkit.Listeners.ConnectionListener;
import com.ristexsoftware.lolbans.commands.Ban;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

public class Main extends JavaPlugin {
    @Getter
    private static Main plugin;
    public static boolean isEnabled = false;
    List<Command> CommandList = new ArrayList<Command>();

    @Override
    public void onEnable() {
        plugin = this;
        isEnabled = true;
        try {
            new LolBans(getDataFolder(), getFile(), Class.forName("com.destroystokyo.paper.VersionHistoryManager$VersionData") != null ? ServerType.PAPER : ServerType.BUKKIT);
        } catch (ClassNotFoundException e) { 
            e.printStackTrace();
            new LolBans(getDataFolder(), getFile(), ServerType.BUKKIT);
        }

        // This is dumb, plugman somehow breaks lolbans
        // but whatever, you shouldn't be using plugman anyway...
        if (getServer().getPluginManager().getPlugin("PlugMan") != null)
            getLogger().warning(
                    "PlugMan detected! This WILL cause issues with LolBans, please consider restarting the server to update plugins!");

        // Creating config folder, and adding config to it.
        // if (!this.getDataFolder().exists()) {
        //     getLogger().info("Error: No folder for lolbans was found! Creating...");
        //     this.getDataFolder().mkdirs();
        //     this.saveDefaultConfig();
        //     getLogger().severe("Please configure lolbans and restart the server! :)");
        //     // They're not gonna have their database setup, just exit. It stops us from
        //     // having errors.
        //     return;
        // }

        // if (!(new File(this.getDataFolder(), "config.yml").exists())) {
        //     this.saveDefaultConfig();
        //     getLogger().severe("Please configure lolbans and restart the server! :)");
        //     // They're not gonna have their database setup, just exit. It stops us from
        //     // having errors.
        //     return;
        // }
        // Make sure our messages file exists
        Messages.getMessages();
        
        if (!Database.initDatabase())
            return;
        
        // Incase an admin does /reload
        for (Player player : Bukkit.getOnlinePlayers())
            LolBans.registerUser(player);
        
        CommandUtil.Bukkit.registerBukkitCommand(new Ban.BanCommand(LolBans.getPlugin()));
        getServer().getPluginManager().registerEvents(new ConnectionListener(), this);
    }

    @Override
    public void onDisable() {
        isEnabled = false;
        saveConfig();
    }
} 
