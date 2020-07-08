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