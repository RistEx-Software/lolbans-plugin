package com.ristexsoftware.lolbans.Objects;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

public class User
{
    static Main self = Main.getPlugin(Main.class);
    public boolean IsWarn;
    
    private Player pl;
    private boolean frozen;
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

    public boolean IsFrozen()
    {
        return this.frozen;
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

    public void SetFrozen(boolean IsFrozen)
    {
        this.frozen = IsFrozen;
        if (!IsFrozen)
            this.SpawnBox(false, Material.AIR.createBlockData());
    }

    public void SendMessage(String message)
    {
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
            PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM Punishments WHERE UUID = ? AND Type = 0 AND Appealed = FALSE LIMIT 1");
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
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM Punishments Kicks WHERE ExecutionerUUID = ? LIMIT 1");
            
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
            PreparedStatement ps = self.connection.prepareStatement("SELECT 1 FROM Punishments WHERE UUID = ? AND Type = 1 AND Appealed = false LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        }
        catch (SQLException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }

    public static OfflinePlayer FindPlayerByAny(String PunishID)
    {
        // Try stupid first. If the PunishID is just a nickname, then avoid DB queries.
        @SuppressWarnings("deprecation") OfflinePlayer op = Bukkit.getOfflinePlayer(PunishID);
        if (op != null)
            return op;

        // Now we move to more expensive operations.
        try 
        {
            PreparedStatement bplay = self.connection.prepareStatement("SELECT UUID FROM Punishments WHERE PunishID = ? OR PlayerName = ? OR UUID = ? LIMIT 1");
            bplay.setString(1, PunishID);
            bplay.setString(2, PunishID);
            bplay.setString(3, PunishID);

            Optional<ResultSet> bpres = DatabaseUtil.ExecuteLater(bplay).get();
            
            if (bpres.isPresent())
            {
                ResultSet res = bpres.get();
                if (res.next())
                {
                    UUID uuid = UUID.fromString(res.getString("UUID"));
                    return Bukkit.getOfflinePlayer(uuid);
                }
            }
        }
        catch (SQLException | InterruptedException | ExecutionException ex)
        {
            ex.printStackTrace();
        }
        
        return null;
    }

    public static void KickPlayer(Punishment p)
    {
        if (p.GetPunishmentType() != PunishmentType.PUNISH_KICK)
            User.KickPlayerBan(p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getName(), (Player)p.GetPlayer(), p.GetPunishmentID(), p.GetReason(), p.GetTimePunished());
        else
            User.KickPlayer(p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getName(), (Player)p.GetPlayer(), p.GetPunishmentID(), p.GetReason());
    }

    public static void KickPlayerBan(String sender, Player target, String PunishID, String reason, Timestamp BanTime)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.Translate(BanTime != null ? "Ban.TempBanMessage" : "Ban.PermBanMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("ARBITER", sender);
                    put("date", BanTime != null ? BanTime.toString() : "Never");
                    put("PunishID", PunishID);
                }}
            );
            target.kickPlayer(KickMessage);
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    public static void KickPlayerIP(String sender, Player target, String PunishID, String reason, Timestamp BanTime, String IP)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.Translate(BanTime != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("ARBITER", sender);
                    put("date", BanTime != null ? BanTime.toString() : "Never");
                    put("PunishID", PunishID);
                    put("IPAddress", IP);
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
            String KickMessage = Messages.Translate("Kick.KickMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("ARBITER", sender);
                    put("punishid", KickID);
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

    public static boolean PermissionDenied(CommandSender sender, String PermissionNode)
    {
        try
        {
            sender.sendMessage(Messages.Translate("NoPermission", 
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("arbiter", sender.getName());
                    put("permission", PermissionNode);
                }}
            ));
        }
        catch (InvalidConfigurationException ex)
        {
            ex.printStackTrace();
            sender.sendMessage("Permission Denied!");
        }
        return true;
    }

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
            sender.sendMessage(Messages.Translate(MessageName,
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

    public static boolean PlayerOnlyVariableMessage(String MessageName, CommandSender sender, String name, boolean ret)
    {
        try 
        {
            sender.sendMessage(Messages.Translate(MessageName,
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", name);
                    // TODO: More appropriate name?
                    put("sender", sender.getName());
                }}
            ));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
        return ret;
	}
}