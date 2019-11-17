package me.zacherycoleman.lolbans.Runnables;

import java.sql.*;
import org.bukkit.scheduler.BukkitRunnable;
import me.zacherycoleman.lolbans.Utils.DiscordUtil;
import me.zacherycoleman.lolbans.Main;

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
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }
}