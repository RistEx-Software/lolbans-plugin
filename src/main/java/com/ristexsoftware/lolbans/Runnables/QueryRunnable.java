package com.ristexsoftware.lolbans.Runnables;

import inet.ipaddr.IPAddressString;
import java.net.UnknownHostException;
import java.sql.*;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.TimeUtil;

import org.bukkit.scheduler.BukkitRunnable;


public class QueryRunnable extends BukkitRunnable
{
    public void run()
    {
        Main self = Main.getPlugin(Main.class);
        try
        {
            /*
            PreparedStatement ps2 = self.connection.prepareStatement("SELECT * FROM Punishments WHERE Expiry IS NOT NULL AND Expiry <= NOW()");
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next())
            {
                String name = rs2.getString("PlayerName"), id = rs2.getString("PunishID");
                self.getLogger().info(String.format("Expiring punishment on %s (#%s)", name, id));
                DiscordUtil.SendFormatted(String.format("Expiring punishment on %s (#%s)", name, id));
            }*/

            self.connection.prepareStatement("DELETE FROM LinkConfirmations WHERE Expiry <= NOW()").executeUpdate();
            PreparedStatement ps = self.connection.prepareStatement("UPDATE Reports SET Closed = True, CloseReason = 'Expired' WHERE TimeAdded <= ?");
            ps.setTimestamp(1, new Timestamp((TimeUtil.GetUnixTime() * 1000L) + TimeUtil.Duration(self.getConfig().getString("General.ReportExpiry", "3d")).get()));
            DatabaseUtil.ExecuteUpdate(ps);

            /*******************************************************************************
             * Ensure our IP ban list is up to date.
             */

            // Grab all the latest IP bans from the databse and ensure everything is up to date.
            ResultSet rs = self.connection.prepareStatement("SELECT * FROM IPBans").executeQuery();
            while (rs.next())
            {
                IPAddressString addr = new IPAddressString(rs.getString("IPAddress"));

                // Try and find our address.
                boolean found = false;
                for (IPAddressString cb : Main.BannedAddresses)
                {
                    if (cb.compareTo(addr) == 0)
                    { 
                        found = true;
                        break;
                    }
                }

                // Add our banned cidr range if not found.
                if (!found)
                    Main.BannedAddresses.add(addr);
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }
}