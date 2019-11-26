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
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import me.zacherycoleman.lolbans.Main;
import net.md_5.bungee.api.ChatColor;

public class User
{
    public boolean IsWarn;
    static Main self = Main.getPlugin(Main.class);
    
    private Player pl;
    private Location WarnLocation;
    private String WarnMessage;

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

    public Location GetWarnLocation()
    {
        return this.WarnLocation;
    }

    public String GetWarnMessage()
    {
        return this.WarnMessage;
    }

    public String getName()
    {
        return this.pl.getName();
    }

    public boolean IsWarn()
    {
        return this.IsWarn;
    }

    // ALL VOIDS GO AFTER HERE

    public void SetWarned(boolean IsWarn, Location warnLocation, String WarnMessage)
    {
        this.IsWarn = IsWarn;
        if (!IsWarn)
            this.SpawnBox(false, Material.AIR.createBlockData());
        this.WarnLocation = warnLocation;
        this.WarnMessage = WarnMessage;
    }

    public void SendMessage(String message)
    {
        //target.sendMessage(message);
        this.pl.sendMessage(message);
    }

    public void SpawnBox(boolean teleport, BlockData BlockType)
    {
        if (BlockType == null)
            BlockType = Material.BARRIER.createBlockData();
        Location loc = this.GetWarnLocation();
        // Create a barrier block.
        //BlockData BlockType = Material.GLASS.createBlockData();

        // We must first ensure the player is actually in a safe space to
        // lock them down on. We do this by finding the ground, then teleporting
        // them both to the ground and the center of the block.
        // If we don't find the ground and teleport them there, then the player is
        // considered to be "flying" and can be kicked as such.
        Location TeleportLoc = this.pl.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ()).getLocation();
        // Preserve the player's pitch/yaw values
        TeleportLoc.setPitch(loc.getPitch());
        TeleportLoc.setYaw(loc.getYaw());
        // Add to get to center of the block
        TeleportLoc.add(0.5, 0, 0.5);
        // Teleport.
        if (teleport)
            this.pl.teleport(TeleportLoc);

        // Set the block under them
        //this.pl.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);
        // Turns out that setting the block can cause client desyncs.
        TeleportLoc.subtract(0, 1, 0);
        // set the block above them
        this.pl.sendBlockChange(TeleportLoc.add(0, 3, 0), BlockType);

        // Reset our TeleportLoc.
        TeleportLoc.subtract(0, 2, 0);

        // Now set the blocks to all sides of them.
        this.pl.sendBlockChange(TeleportLoc.add(1, 0, 0), BlockType);
        this.pl.sendBlockChange(TeleportLoc.add(0, 1, 0), BlockType);

        this.pl.sendBlockChange(TeleportLoc.subtract(2, 0, 0), BlockType);
        this.pl.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);

        this.pl.sendBlockChange(TeleportLoc.add(1, 0, 1), BlockType);
        this.pl.sendBlockChange(TeleportLoc.add(0, 1, 0), BlockType);

        this.pl.sendBlockChange(TeleportLoc.subtract(0, 0, 2), BlockType);
        this.pl.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);
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
        if (BanTime != null)
            Configuration.TempBanMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("TempBanMessage").replace("%player%", target.getName()).replace("%reason%", reason).replace("%banner%", sender).replace("%timetoexpire%", BanTime.toString()).replace("%banid%", BanID));
        Configuration.PermBanMessage = ChatColor.translateAlternateColorCodes('&', self.getConfig().getString("PermBanMessage").replace("%player%", target.getName()).replace("%reason%", reason).replace("%banner%", sender).replace("%banid%", BanID));

       // bd.AddString(Configuration.PermBanMessage);

        if (BanTime != null)
            target.kickPlayer(Configuration.TempBanMessage);
        else
            target.kickPlayer(Configuration.PermBanMessage);
    }

}