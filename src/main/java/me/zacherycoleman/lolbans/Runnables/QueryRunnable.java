package me.zacherycoleman.lolbans.Runnables;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.sql.*;
import org.bukkit.scheduler.BukkitRunnable;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.CIDRBan;

public class QueryRunnable extends BukkitRunnable
{
    public void run()
    {
        Main self = Main.getPlugin(Main.class);
        try
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM BannedPlayers WHERE Expiry IS NOT NULL AND Expiry <= NOW()");
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                String name = rs.getString("PlayerName"), id = rs.getString("BanID");
                self.getLogger().info(String.format("Expiring ban on %s (#%s)", name, id));
                DiscordUtil.Send(name, id);
            }

            self.connection.prepareStatement("DELETE FROM BannedPlayers WHERE Expiry IS NOT NULL AND Expiry <= NOW()").executeUpdate();

            /*******************************************************************************
             * Ensure our IP ban list is up to date.
             */

            // Grab all the latest IP bans from the databse and ensure everything is up to date.
            rs = self.connection.prepareStatement("SELECT * IPBans").executeQuery();
            while (rs.next())
            {
                try 
                {
                    Blob ipaddress = rs.getBlob("IPAddress");
                    // Convert the ipaddress to an InetAddress
                    InetAddress addr = InetAddress.getByAddress(ipaddress.getBytes(0L, (int)ipaddress.length()));

                    // Try and find our address.
                    CIDRBan cb = new CIDRBan(addr, rs.getInt("CIDR"));
                    boolean found = false;
                    for (CIDRBan c : Main.BannedCIDRs)
                    {
                        if (c.compare(cb))
                        {
                            found = true;
                            break;
                        }
                    }

                    // Add our banned cidr range if not found.
                    if (!found)
                        Main.BannedCIDRs.add(cb);
                }
                catch (UnknownHostException e)
                {
                    // Maybe unnecessary?
                    e.printStackTrace();
                    continue;
                }
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }
}