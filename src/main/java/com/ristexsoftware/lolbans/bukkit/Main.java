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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.knappy.util.Version;
import com.ristexsoftware.lolbans.bukkit.Listeners.ConnectionListener;
import com.ristexsoftware.lolbans.bukkit.Listeners.PlayerEventListener;
import com.ristexsoftware.lolbans.bukkit.provider.BukkitConfigProvider;
import com.ristexsoftware.lolbans.bukkit.provider.BukkitUserProvider;
import com.ristexsoftware.lolbans.common.utils.CommandUtil;

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
            new LolBans(new BukkitConfigProvider(), new BukkitUserProvider(), Version.getServerType());
        } catch (FileNotFoundException e) {
            return;
        }


        // This is dumb, plugman somehow breaks lolbans
        // but whatever, you shouldn't be using plugman anyway...
        if (getServer().getPluginManager().getPlugin("PlugMan") != null)
            getLogger().warning(
                    "PlugMan detected! This WILL cause issues with LolBans, please consider restarting the server to update plugins!");

        for (Player player : Bukkit.getOnlinePlayers())
            LolBans.getPlugin().registerUser(player);

        if (!Database.initDatabase())
            return;

        CommandUtil.registerAllCommands();

        getServer().getPluginManager().registerEvents(new ConnectionListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);
    }

    @Override
    public void onDisable() {
        LolBans.getPlugin().destroy();
        isEnabled = false;
        reloadConfig();
    }
}