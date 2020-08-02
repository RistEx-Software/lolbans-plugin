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

package com.ristexsoftware.lolbans.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.ristexsoftware.lolbans.bukkit.Main;

import org.bukkit.Bukkit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Represents a player. Proxies bungee and bukkit methods.
 */
@SuppressWarnings("deprecation")
public class User {
    public static HashMap<UUID, User> USERS = new HashMap<>();

    private String username;
    private UUID uuid;

    private Boolean isConsole = false;

    public User(String Username, UUID UUID) {
        this.username = Username;
        this.uuid = UUID;

        if (this.uuid.toString() == "00000000-0000-0000-0000-000000000000")
            this.isConsole = true;
    }

    /**
     * Get the name of this user
     * @return The name of this  user
     */
    public String getName() {
        if (this.isConsole) {
            return "CONSOLE";
        }

        return this.username;
    }

    /**
     * Get the unique id of this user
     * @return The unique id of this user
     */
    public UUID getUniqueId() {
        if (this.isConsole) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        return this.uuid;
    }

    /**
     * Check if this user is banned
     * @return True if this user is banned
     */
    public boolean isBanned() {
        try {
            return Database.isUserBanned(this.uuid).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOnline() {
        return USERS.containsKey(this.uuid);
    }

    public boolean isConsole() {
        return isConsole;
    }

    /**
     * Get the address of this user
     * @return The player's address, or null if an address can't be found
     */
    public String getAddress() {
        if (Main.getPlugin(Main.class).isEnabled()) {
            org.bukkit.entity.Player player = Bukkit.getPlayer(this.username);
            if (player != null)
                return player.getAddress().getAddress().getHostAddress();
        } else if (com.ristexsoftware.lolbans.bungeecord.Main.isEnabled) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(this.uuid);
            if (player != null) 
                return player.getAddress().getAddress().getHostAddress();
        }
        return Database.getLastAddress(this.uuid.toString());
    }

    /**
     * Disconnect a user from the server
     * @param message The message to send
     */
    public void disconnect(String message) {
        if (message == null || message.equals(""))
            message = "You have been kicked by an operator!";
        if (Main.getPlugin(Main.class).isEnabled()) {
            org.bukkit.entity.Player player = Bukkit.getPlayer(this.username);
            player.kickPlayer(message);
        } else if (com.ristexsoftware.lolbans.bungeecord.Main.isEnabled) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(this.uuid);
            player.disconnect(message);
        }
    }

    /**
     * Send a message to a user.
     * 
     * @param message The message to send
     * @return If the user is not online
     * @return If the player is not found
     * @throws NullPointerException if <code>message</code> is null
     */
    public void sendMessage(String message) {
        if (!isOnline())
            return;
        if (message == null)
            throw new NullPointerException();
        if (Main.getPlugin(Main.class).isEnabled()) {
            org.bukkit.entity.Player player = Bukkit.getPlayer(this.username);
            if (player == null)
                return;
            player.sendMessage(message);
        } else if (com.ristexsoftware.lolbans.bungeecord.Main.isEnabled) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(this.uuid);
            if (player == null)
                return;
            player.sendMessage(message);
        }
        return;
    }

    public static User resolveUser(String Username) {
        if (!Username.equals("") || Username != null) {
            try {
                // api.mojang.com is slow as fuck, but i'll make this a config option
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + Username);
                JsonElement jsonResponse = new JsonParser().parse(new InputStreamReader(url.openStream()));
                String uuid = jsonResponse.getAsJsonObject().get("uuid").toString().replace("\"", "");
                String username = jsonResponse.getAsJsonObject().get("username").toString().replace("\"", "");

                if (uuid == null)
                    return null;
                return new User(username, UUID.fromString(uuid));

            } catch (IOException e) {
                // e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static User getConsoleUser() {
        return new User("CONSOLE", UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }
}