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

package com.ristexsoftware.lolbans.bukkit.Listeners;

import java.sql.Timestamp;
import java.util.concurrent.ExecutionException;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.configuration.Messages;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {
    
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        LolBans.registerUser(player);
        String ip = player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress();
        try {
            if (!(Database.insertUser(player.getUniqueId().toString(), player.getName(), ip == null ? "#" : ip, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis())).get()))
                LolBans.getLogger().severe(Messages.serverError);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        LolBans.removeUser(player);
        try {
            if (!(Database.updateUser(player.getUniqueId().toString(), player.getName(), player.getAddress().getAddress().getHostAddress(), new Timestamp(System.currentTimeMillis())).get()))
                LolBans.getLogger().severe(Messages.serverError);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        LolBans.removeUser(player);
        try {
            if (!(Database.updateUser(player.getUniqueId().toString(), player.getName(), player.getAddress().getAddress().getHostAddress(), new Timestamp(System.currentTimeMillis())).get()))
                LolBans.getLogger().severe(Messages.serverError);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}