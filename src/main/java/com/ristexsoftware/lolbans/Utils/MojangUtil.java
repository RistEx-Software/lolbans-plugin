/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */


package com.ristexsoftware.lolbans.Utils;

import java.io.IOException;
import java.util.UUID;
import java.io.InputStreamReader;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class MojangUtil {

    public class MojangUser {

        private String username;
        private UUID uuid;

        public MojangUser(String pUsername, UUID pUUID) {
            this.username = pUsername;
            this.uuid = pUUID;
        }

        public String getName() {
            return this.username;
        }

        public UUID getUniqueId() {
            return this.uuid;
        }
    }


    public MojangUser resolveUser(String pUsername) {
        if (!pUsername.equals("") || pUsername != null) {
            try {
                // api.mojang.com is slow as fuck, but i'll make this a config option
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + pUsername);
                JsonElement jsonResponse = new JsonParser().parse(new InputStreamReader(url.openStream()));
                String uuid =  jsonResponse.getAsJsonObject().get("uuid").toString().replace("\"", "");
                String username = jsonResponse.getAsJsonObject().get("username").toString().replace("\"", "");

                if (uuid == null)
                    return null;
                return new MojangUser(username, UUID.fromString(uuid));

            } catch (IOException e) {
                // e.printStackTrace();
                return null;
            }
        }
        return null; // "Dead Code" but doesn't compile because you're required to return MojangUser, dumb...
    }
}