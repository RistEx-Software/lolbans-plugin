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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class GeoLocation
{
    private static String ReadURL(String urlString) throws Exception 
	{
        BufferedReader reader = null;
        try 
		{
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();

            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        }
		finally 
		{
            if (reader != null)
                reader.close();
        }
    }

    /**
     * Returns array of string with Country and CountryCode
     * @param ipaddr The IP you want to get geolocaiton data from
     * @return A string array with 2 things. (0 = CountryCode, 1 = CountryName)
     */
    public static String[] GetIPLocation(String ipaddr)
	{
        try
        {
            String json = null;
            json = ReadURL("https://geolocation-db.com/json/" + ipaddr);

            JsonElement jelement = new JsonParser().parse(json);
            JsonObject  jobject = jelement.getAsJsonObject();
            String result = jobject.get("country_code").getAsString();
            String result2 = jobject.get("country_name").getAsString();
            String[] results = {result, result2};
            String[] error = {"--", "--"};
            if (result.equalsIgnoreCase("Not Found") || result2.equalsIgnoreCase("Not Found"))
                return error;
            return results;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
