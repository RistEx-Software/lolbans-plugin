package me.zacherycoleman.lolbans.Utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.BoxDrawer;
import net.md_5.bungee.api.ChatColor;

public class User
{
    static Main self = Main.getPlugin(Main.class);
    
    private Player pl;

    public User(Player pl)
    {
        this.pl = pl;
    }

    public Player getPlayer()
    {
        return this.pl;
    }

    public Location getLocation()
    {
        return this.pl.getLocation();
    }

    public String getName()
    {
        return this.pl.getName();
    }

    // All voids after this.

    public void sendMessage(String message)
    {
        this.pl.sendMessage(message);
    }

    public static boolean IsPlayerInWave(OfflinePlayer user)
    {
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM BanWave WHERE UUID = ? LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean IsPlayerBanned(OfflinePlayer user)
    {
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM BannedPlayers WHERE UUID = ? LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public static OfflinePlayer FindPlayerByBanID(String BanID)
    {
        // Try stupid first. If the BanID is just a nickname, then avoid DB queries.
        OfflinePlayer op = Bukkit.getOfflinePlayer(BanID);
        if (op != null)
            return op;
        
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT UUID FROM BannedPlayers WHERE BanID = ? LIMIT 1");
            ps.setString(1, BanID);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next())
            {
                UUID uuid = UUID.fromString(rs.getString("UUID"));
                op = Bukkit.getOfflinePlayer(uuid);

                // Try and query from history
                if (op == null)
                {
                    ps = self.connection.prepareStatement("SELECT UUID FROM BannedHistory WHERE BanID = ? LIMIT 1");
                    ps.setString(1, BanID);
                    rs = ps.executeQuery();

                    if (rs.next())
                    {
                        uuid = UUID.fromString(rs.getString("UUID"));
                        op = Bukkit.getOfflinePlayer(uuid);
                        return op;
                    }
                }
                else
                    return op;
            }
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        
        return null;
    }

    public static void KickPlayer(String sender, Player target, String BanID, String reason, Timestamp BanTime)
    {
        //StringBuilder builder = new StringBuilder();

        //%player% %reason% %banner% %timetoexpire% %banid%
        BoxDrawer bd = new BoxDrawer();

        if (BanTime != null)
            self.TempBanMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("TempBanMessage").replace("%player%", target.getName()).replace("%reason%", reason).replace("%banner%", sender).replace("%timetoexpire%", BanTime.toString()).replace("%banid%", "#"+BanID));
        self.PermBanMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("PermBanMessage").replace("%player%", target.getName()).replace("%reason%", reason).replace("%banner%", sender).replace("%banid%", "#"+BanID));

        bd.AddString(self.PermBanMessage);

        if (BanTime != null)
            target.kickPlayer(self.TempBanMessage);
        else
            target.kickPlayer(bd.RenderBox());  
    }

}