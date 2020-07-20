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

package com.ristexsoftware.lolbans.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.ristexsoftware.lolbans.api.database.Database;

public class User {
    public static HashMap<UUID, User> USERS = new HashMap<>();

    private String username;
    private UUID uuid;

    public User(String Username, UUID UUID) {
        this.username = Username;
        this.uuid = UUID;
    }

    public String getName() {
        return this.username;
    }

    public UUID getUniqueId() {
        return this.uuid;
    }
    
    public static User resolveUser(String Username) {
        if (!Username.equals("") || Username != null) {
            try {
                // api.mojang.com is slow as fuck, but i'll make this a config option
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + Username);
                JsonElement jsonResponse = new JsonParser().parse(new InputStreamReader(url.openStream()));
                String uuid =  jsonResponse.getAsJsonObject().get("uuid").toString().replace("\"", "");
                String username = jsonResponse.getAsJsonObject().get("username").toString().replace("\"", "");

                if (uuid == null)
                    return null;
                return new User(username, UUID.fromString(uuid));

            } catch (IOException e) {
                // e.printStackTrace();
                return null;
            }
        }
        return null; // "Dead Code" but doesn't compile because you're required to return User, dumb...
    }

    public boolean isBanned() {
        try {
            return Database.isUserBanned(this.uuid).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOnline() {
        if (USERS.containsKey(this.uuid))
            return true;
        return false;
    }

    public String getAddress() {
        String ip = Database.getLastAddress(this.uuid.toString());
        if (ip != null)
            return ip;
        return null;
    }
}