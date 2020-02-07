package me.zacherycoleman.lolbans.Utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import inet.ipaddr.IPAddress;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

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

    public static boolean StaffHasHistory(CommandSender user)
    {
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM BannedPlayers, BannedHistory, Warnings, MutedHistory, MutedPlayers, Kicks WHERE ExecutionerUUID = ? LIMIT 1");
            
            if (user instanceof Player)
            {
                OfflinePlayer user2 = (OfflinePlayer) user;
                ps.setString(1, user2.getUniqueId().toString());
            }
            else if (user instanceof ConsoleCommandSender)
            {
                ps.setString(1, "console");
            }
            

            return ps.executeQuery().next();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean IsPlayerMuted(OfflinePlayer user)
    {
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM MutedPlayers WHERE UUID = ? LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    // FIXME: Return a Future<> instead of an OfflinePlayer
    public static OfflinePlayer FindPlayerByBanID(String BanID)
    {
        // Try stupid first. If the BanID is just a nickname, then avoid DB queries.
        OfflinePlayer op = Bukkit.getOfflinePlayer(BanID);
        if (op != null)
            return op;
        
        try 
        {
            PreparedStatement bplay = self.connection.prepareStatement("SELECT UUID FROM BannedPlayers WHERE PunishID = ? LIMIT 1");
            PreparedStatement bhist = self.connection.prepareStatement("SELECT UUID FROM BannedHistory WHERE PunishID = ? LIMIT 1");
            bplay.setString(1, BanID);
            bhist.setString(1, BanID);

            Future<Optional<ResultSet>> bhres = DatabaseUtil.ExecuteLater(bhist);
            Optional<ResultSet> bpres = DatabaseUtil.ExecuteLater(bplay).get();
            
            if (bpres.isPresent())
            {
                ResultSet res = bpres.get();
                if (res.next())
                {
                    UUID uuid = UUID.fromString(res.getString("UUID"));
                    op = Bukkit.getOfflinePlayer(uuid);
                }

                // Try and query from history
                if (op == null)
                {
                    Optional<ResultSet> opt = bhres.get();
                    if (opt.isPresent())
                    {
                        res = opt.get();
                        if (res.next())
                        {
                            UUID uuid = UUID.fromString(res.getString("UUID"));
                            op = Bukkit.getOfflinePlayer(uuid);
                            return op;
                        }
                    }
                }
                else
                    return op;
            }
        }
        catch (SQLException | InterruptedException | ExecutionException ex)
        {
            ex.printStackTrace();
        }
        
        return null;
    }

    public static void KickPlayer(String sender, Player target, String BanID, String reason, Timestamp BanTime)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.GetMessages().Translate(BanTime != null ? "Ban.TempBanMessage" : "Ban.PermBanMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("banner", sender);
                    put("fullexpiry", BanTime != null ? String.format("%s (%s)", TimeUtil.TimeString(BanTime), TimeUtil.Expires(BanTime)) : "Never");
                    put("expiryduration", BanTime != null ? TimeUtil.Expires(BanTime) : "Never");
                    put("dateexpiry", BanTime != null ? TimeUtil.TimeString(BanTime) : "Never");
                    put("BanID", BanID);
                }}
            );
            target.kickPlayer(KickMessage);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    public static void KickPlayer(String sender, Player target, String BanID, String reason, Timestamp BanTime, IPAddress IP)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.GetMessages().Translate(BanTime != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("banner", sender);
                    put("fullexpiry", BanTime != null ? String.format("%s (%s)", TimeUtil.TimeString(BanTime), TimeUtil.Expires(BanTime)) : "Never");
                    put("expiryduration", BanTime != null ? TimeUtil.Expires(BanTime) : "Never");
                    put("dateexpiry", BanTime != null ? TimeUtil.TimeString(BanTime) : "Never");
                    put("BanID", BanID);
                    put("IPAddress", IP.toString());
                }}
            );
            target.kickPlayer(KickMessage);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    public static void KickPlayer(String sender, Player target, String KickID, String reason)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.GetMessages().Translate("Kick.KickMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("KICKER", sender);
                    put("kickid", KickID);
                }}
            );
            target.kickPlayer(KickMessage);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    /************************************************************************
     * Convenience functions to make the code cleaner.
     */

    public static boolean NoSuchPlayer(CommandSender sender, String PlayerName, boolean ret)
    {
        return User.PlayerOnlyVariableMessage("PlayerDoesntExist", sender, PlayerName, ret);
    }

    public static boolean IPIsBanned(CommandSender sender, String iPAddresString, boolean ret)
    {
        return User.PlayerOnlyVariableMessage("IPIsBanned", sender, iPAddresString, ret);
    }

    public static boolean PlayerIsOffline(CommandSender sender, String PlayerName, boolean ret)
    {
        return User.PlayerOnlyVariableMessage("PlayerIsOffline", sender, PlayerName, ret);
    }

    public static boolean PlayerOnlyVariableMessage(String MessageName, CommandSender sender, IPAddress thingy, boolean ret)
    {
        try 
        {
            sender.sendMessage(Messages.GetMessages().Translate(MessageName,
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", thingy.toString());
                    put("prefix", Messages.Prefix);
                }}
            ));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
        return ret;
    }

	public static boolean PlayerOnlyVariableMessage(String messageName, CommandSender sender, String name,
			boolean ret) {
		return false;
	}

}