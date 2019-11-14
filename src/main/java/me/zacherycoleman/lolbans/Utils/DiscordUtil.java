package me.zacherycoleman.lolbans.Utils; // Zachery's package owo


import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/*
               _   ___      ___   _ 
              | | | \ \ /\ / / | | |
              | |_| |\ V  V /| |_| |
               \__,_| \_/\_/  \__,_|

    　　　　　　　　　　▄█▀█▀█▄
    　　　　　　　　▄█▀　　█　　▀█▄
    　　　　　　　▄█▀　　　　　　　▀█▄
    　　　　　　　█　　　　　　　　　　　█
    　　　　　　　█　　　　　　　　　　　█
    　　　　　　　▀█▄▄　　█　　　▄█▀
    　　　　　　　　　█　　▄▀▄　　█
    　　　　　　　　　█　▀　　　▀　█
    　　　　　　　　　█　　　　　　　█
    　　　　　　　　　█　　　　　　　█
    　　　　　　　　　█　　　　 　　 █
    　　　　　　　　　█　　　　　　　█
    　　　　　　　　　█　　　　　　　█
    　　　▄█▀▀█▄█　　　　　　　█▄█▀█▄
    　▄█▀▀　　　　▀　　　　　　　　　　　　▀▀█
    █▀　　　　　　　　　　　　　　　　　　　　　　▀█
    █　　　　　　　　　　　　　　　　　　　　　　　　█
    █　　　　　　　　　　　▄█▄　　　　　　　　　　█
    ▀█　　　　　　　　　█▀　▀█　　　　　　　　█▀
    　▀█▄　　　　　　█▀　　　▀█　　　　　▄█▀
    　　　▀█▄▄▄█▀　　　　　　▀█▄▄▄█▀
                        
*/
public class DiscordUtil
{
    public static String Webhook = "";

    public static void Send(String message, Object... args)
    {
        // I have no idea what any of this does, it just works...
        try
        {
            URLConnection con = new URL(DiscordUtil.Webhook).openConnection();
            
            HttpsURLConnection https = (HttpsURLConnection)con;
            https.setRequestMethod("POST");
            https.setRequestProperty("User-Agent", "Mozilla/5.0");
            https.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            https.setDoOutput(true);
            
            String out = "{\"content\":\"" + String.format(message, args) + "\"}";

            https.setFixedLengthStreamingMode(out.length());
            https.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            https.connect();

            try
            {
                try
                {
                    DataOutputStream os = new DataOutputStream(con.getOutputStream());

                    try
                    {
                        os.write(out.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                        os.close();
                    }
                    finally
                    {
                        if (os != null)
                            os.close();
                    }
                }
                catch (Throwable throwable)
                {
                    throw throwable;
                }
            }
            catch (Throwable exception)
            {
                exception.printStackTrace();
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
}