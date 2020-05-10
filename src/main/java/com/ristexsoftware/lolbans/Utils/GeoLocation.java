package com.ristexsoftware.lolbans.Utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class GeoLocation {

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    /**
     * Returns array of string with Country and CountryCode
     * @param ipaddr The IP you want to get geolocaiton data from
     * @return A string array with 2 things. (0 = CountryCode, 1 = CountryName)
     */
    public static String[] GetIPLocation(String ipaddr) {
        try
        {
            String json = null;
            json = readUrl("https://geolocation-db.com/json/" + ipaddr);

            JsonElement jelement = new JsonParser().parse(json);
            JsonObject  jobject = jelement.getAsJsonObject();
            String result = jobject.get("country_code").getAsString();
            String result2 = jobject.get("country_name").getAsString();
            String[] results = {result, result2};
            return results;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        
    }

}
