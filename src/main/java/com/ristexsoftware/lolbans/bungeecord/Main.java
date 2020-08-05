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

package com.ristexsoftware.lolbans.bungeecord;

import java.io.FileNotFoundException;
import java.util.UUID;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.common.utils.CommandUtil;
import com.ristexsoftware.lolbans.common.utils.MemoryUtil;
import com.ristexsoftware.lolbans.api.utils.ServerType;
import com.ristexsoftware.lolbans.bungeecord.Listeners.ConnectionListener;
import com.ristexsoftware.lolbans.common.commands.Ban;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class Main extends Plugin {
    public static boolean isEnabled;
    @Getter
    public static Main plugin;

    @Override
    public void onEnable() {
        try {
            new LolBans(getDataFolder(), getFile(), ServerType.BUNGEECORD);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        isEnabled = true;
        // Make sure our messages file exists
        Messages.getMessages();

        if (!Database.initDatabase())
            return;

        CommandUtil.BungeeCord.registerBungeeCommand(new Ban.BanCommand(LolBans.getPlugin()));
        getProxy().getPluginManager().registerListener(this, new ConnectionListener());
    }

    @Override
    public void onDisable() {
        isEnabled = false;
    }

    public static ProxiedPlayer getPlayer(String name) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }
    public static ProxiedPlayer getPlayer(UUID uuid) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getUniqueId() == uuid) return player;
        }
        return null;
    }
}
