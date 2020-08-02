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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.utils.PunishID;
import com.ristexsoftware.lolbans.bukkit.commands.Test;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

public class Main extends JavaPlugin {
    @Getter
    private static Main plugin;
    List<Command> CommandList = new ArrayList<Command>();

    @Override
    public void onEnable() {
        new LolBans(getDataFolder(), getFile());
        plugin = this;
        if (!Database.initDatabase())
            return;
        this.getCommand("test").setExecutor(new Test());
    }

    @Override
    public void onDisable() {

    }
}
