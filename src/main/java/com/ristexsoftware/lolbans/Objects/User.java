package com.ristexsoftware.lolbans.Objects;

import java.net.InetAddress;
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

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

import com.google.common.net.InetAddresses;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;

public class User {
    static Main self = Main.getPlugin(Main.class);
    public boolean IsWarn;

    private Player pl;
    private boolean frozen;
    private Location WarnLocation;
    private String WarnMessage;

    /**
     * Create a new user based on a player
     * 
     * @param pl The player we're making the user based on.
     */
    public User(Player pl) {
        this.pl = pl;
    }

    /**
     * Get the player from this user
     * 
     * @return the bukkit player object
     */
    public Player getPlayer() {
        return this.pl;
    }

    /**
     * Get the current location of the player
     * 
     * @return the bukkit location object
     */
    public Location getLocation() {
        return this.pl.getLocation();
    }

    /**
     * Get the warned location of the player, freezing them in this spot if they are
     * warned
     * 
     * @return The location object of where the player was frozen for a warning
     */
    public Location GetWarnLocation() {
        return this.WarnLocation;
    }

    /**
     * Get the warning message if the player was warned.
     * 
     * @return a string with the warning message
     */
    public String GetWarnMessage() {
        return this.WarnMessage;
    }

    /**
     * Get the user's name
     * 
     * @return the name of the user/player
     */
    public String getName() {
        return this.pl.getName();
    }

    /**
     * Checks if the player is currently warned
     * 
     * @return True if the player has not acknowledged a warning
     */
    public boolean IsWarn() {
        return this.IsWarn;
    }

    /**
     * Check if the player is frozen and unable to perform any actions
     * 
     * @return True if the player is frozen in place.
     */
    public boolean IsFrozen() {
        return this.frozen;
    }

    /**
     * Sets whether this player has been warned and should be frozen until they
     * acknowledge it.
     * 
     * @param IsWarn       Whether they are currently warned or not
     * @param warnLocation The location where they should be frozen until the
     *                     warning is acknowledged
     * @param WarnMessage  The message telling them the warning they received.
     */
    public void SetWarned(boolean IsWarn, Location warnLocation, String WarnMessage) {
        this.IsWarn = IsWarn;
        if (!IsWarn)
            this.SpawnBox(false, Material.AIR.createBlockData());
        this.WarnLocation = warnLocation;
        this.WarnMessage = WarnMessage;
    }

    /**
     * Sets whether they're able to perform in game actions or not.
     * 
     * @param IsFrozen True if they are frozen, false to unfreeze
     */
    public void SetFrozen(boolean IsFrozen) {
        this.frozen = IsFrozen;
        if (!IsFrozen)
            this.SpawnBox(true, Material.AIR.createBlockData());
    }

    /**
     * Send the player/user a message
     * 
     * @param message Message to send
     */
    public void SendMessage(String message) {
        this.pl.sendMessage(message);
    }

    /**
     * Spawn a client-only box around the player, disallowing them from leaving the
     * frozen space. This helps combat client-side rubberbanding.
     * 
     * @param teleport  Whether to teleport the player to a safe location on the
     *                  ground, can trigger the server's anti-fly mechanism
     *                  otherwise
     * @param BlockType The kind of block to encase the player in (Barrier blocks by
     *                  default)
     */
    public void SpawnBox(boolean teleport, BlockData BlockType) {
        if (BlockType == null)
            BlockType = Material.BARRIER.createBlockData();
        Location loc = this.GetWarnLocation();
        // Create a barrier block.
        // BlockData BlockType = Material.GLASS.createBlockData();

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
        TeleportLoc.add(0.5, 1, 0.5);
        // Teleport.
        if (teleport)
            this.pl.teleport(TeleportLoc);

        // Set the block under them
        // this.pl.sendBlockChange(TeleportLoc.subtract(0, 1, 0), BlockType);
        // Turns out that setting the block can cause client desyncs.
        TeleportLoc.subtract(0, 1, 0);
        // set the block above them
        this.pl.sendBlockChange(TeleportLoc.add(0, 3, 0), BlockType);
        // and below them!
        // this.pl.sendBlockChange(TeleportLoc.add(0, 4, 0), BlockType);
        // This doesn't work and idk why, will fix later!

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

    /**
     * Check if the player is in a ban wave
     * 
     * @param user The player to check if they're in a ban wave
     * @return True if they are in the next wave
     */
    public static boolean IsPlayerInWave(OfflinePlayer user) {
        try {
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM BanWave WHERE UUID = ? LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Check if the player is banned by lolbans
     * 
     * @param user the user to find
     * @return True if the player is banned.
     */
    public static boolean IsPlayerBanned(OfflinePlayer user) {
        try {
            PreparedStatement ps = self.connection.prepareStatement(
                    "SELECT 1 FROM Punishments WHERE UUID = ? AND Type = 0 AND Appealed = FALSE LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Check if the staff has performed any punishment actions
     * 
     * @param user the user to check
     * @return True if they have performed any punishments.
     */
    public static boolean StaffHasHistory(CommandSender user) {
        try {
            // TODO: Update this.
            PreparedStatement ps = self.connection
                    .prepareStatement("SELECT * FROM Punishments WHERE ExecutionerUUID = ? LIMIT 1");

            if (user instanceof Player) {
                OfflinePlayer user2 = (OfflinePlayer) user;
                ps.setString(1, user2.getUniqueId().toString());
            } else if (user instanceof ConsoleCommandSender) {
                ps.setString(1, "console");
            }

            return ps.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Check if the player is actively muted
     * 
     * @param user Player to check
     * @return True if the player has been muted.
     */
    public static boolean IsPlayerMuted(OfflinePlayer user) {
        try {
            PreparedStatement ps = self.connection.prepareStatement(
                    "SELECT 1 FROM Punishments WHERE UUID = ? AND Type = 1 AND Appealed = false LIMIT 1");
            ps.setString(1, user.getUniqueId().toString());

            return ps.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Find the player by UUID, Punishment ID, or their name
     * 
     * @param PunishID the Punishment ID, UUID, or player name
     * @return The player if found
     */
    public static OfflinePlayer FindPlayerByAny(String PunishID) {
        // TODO: Find players by IP addresses too?
        // Try stupid first. If the PunishID is just a nickname, then avoid DB queries.
        @SuppressWarnings("deprecation")
        OfflinePlayer op = Bukkit.getOfflinePlayer(PunishID);
        if (op != null)
            return op;

        // Now we move to more expensive operations.
        try {
            PreparedStatement bplay = self.connection.prepareStatement(
                    "SELECT UUID FROM Punishments WHERE PunishID = ? OR PlayerName = ? OR UUID = ? LIMIT 1");
            bplay.setString(1, PunishID);
            bplay.setString(2, PunishID);
            bplay.setString(3, PunishID);

            Optional<ResultSet> bpres = DatabaseUtil.ExecuteLater(bplay).get();

            if (bpres.isPresent()) {
                ResultSet res = bpres.get();
                if (res.next()) {
                    UUID uuid = UUID.fromString(res.getString("UUID"));
                    return Bukkit.getOfflinePlayer(uuid);
                }
            }
        } catch (SQLException | InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static IPAddress FindAddressByAny(String any) {
        Player p = Bukkit.getPlayer(any);
        if (p != null) {
            try {
                return new IPAddressString(p.getAddress().getAddress().getHostAddress()).toAddress();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        if (InetAddresses.isUriInetAddress(any))
            return new IPAddressString(any).getAddress();

        try {
            PreparedStatement ps = self.connection.prepareStatement("SELECT IPAddress FROM Users WHERE PlayerName = ?");
            ps.setString(1, any);

            Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();

            if (ores.isPresent()) {
                ResultSet res = ores.get();
                if (res.next())
                    return new IPAddressString(res.getString("IPAddress")).getAddress();
            }
        }
        catch (SQLException | InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            return null;
        }

        return null;
    }

    /**
     * Kick the player from the server based on a punishment.
     * @param p The punishment containing the player and reasons for punishment.
     */
    public static void KickPlayer(Punishment p)
    {
        if (p.GetPunishmentType() != PunishmentType.PUNISH_KICK)
            User.KickPlayerBan(p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getName(), (Player)p.GetPlayer(), p.GetPunishmentID(), p.GetReason(), p.GetTimePunished(), p.GetExpiry());
        else
            User.KickPlayer(p.IsConsoleExectioner() ? "CONSOLE" : p.GetExecutioner().getName(), (Player)p.GetPlayer(), p.GetPunishmentID(), p.GetReason());
    }

    /**
     * Kick the player from the server
     * @param sender The person kicking the player
     * @param target The person being kicked from the server
     * @param PunishID The punishment ID of the kick
     * @param reason The reason for the kick
     * @param TimePunished When the punishment happened
     * @param Expiry If the punishment has an expiration
     */
    public static void KickPlayerBan(String sender, Player target, String PunishID, String reason, Timestamp TimePunished, Timestamp Expiry)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.Translate(Expiry != null ? "Ban.TempBanMessage" : "Ban.PermBanMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("ARBITER", sender);
                    put("TimePunished", TimePunished.toString());
                    if (Expiry != null)
                        put("expiry", Expiry.toString());
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

    /**
     * Kick a player due to their IP address matching an IP or regex ban
     * @param sender The person who ran the command
     * @param target The person who is banned/kicked
     * @param PunishID The punishment ID
     * @param reason The reason for the kicking
     * @param TimePunished The time the punishment was issued
     * @param Expiry The time the punishment expires, if it expires
     * @param IP The IP or regex of the punishment
     */
    public static void KickPlayerIP(String sender, Player target, String PunishID, String reason, Timestamp TimePunished, Timestamp Expiry, String IP)
    {
        try
        {
            // (String message, String ColorChars, Map<String, String> Variables)
            String KickMessage = Messages.Translate(Expiry != null ? "IPBan.TempIPBanMessage" : "IPBan.PermIPBanMessage",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", target.getName());
                    put("reason", reason);
                    put("ARBITER", sender);
                    put("TimePunished", TimePunished.toString());
                    if (Expiry != null)
                        put("Expiry", Expiry.toString());
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

    /**
     * Kick a player for a reason with an ID
     * @param sender The person who is kicking the player
     * @param target The person being kicked from the server
     * @param KickID The ID of the kick
     * @param reason The reason for the kick
     */
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

    /**
     * Send the player a permission denied message
     * @param sender the person who is executing the command
     * @param PermissionNode The permission node they're being denied for
     * @return always true, for use in the command classes.
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

    /**
     * Send the player the "No Such Player" message
     * @param sender The person who executes the command
     * @param PlayerName The name of the player that doesn't exist
     * @param ret The value to return from this function
     * @return the value provided as `ret`
     */
    public static boolean NoSuchPlayer(CommandSender sender, String PlayerName, boolean ret)
    {
        return User.PlayerOnlyVariableMessage("PlayerDoesntExist", sender, PlayerName, ret);
    }

    /**
     * Send the player the "IP Is already banned" message
     * @param sender The person who executed the command
     * @param iPAddresString The ip address that is already banned
     * @param ret The value to return from this function
     * @return The value provided as `ret`
     */
    public static boolean IPIsBanned(CommandSender sender, String iPAddresString, boolean ret)
    {
        return User.PlayerOnlyVariableMessage("IPIsBanned", sender, iPAddresString, ret);
    }

    /**
     * Send the player the "Player is offline" message
     * @param sender The person who executed the command
     * @param PlayerName The name of the offline player
     * @param ret The value to return from this function
     * @return The value provided as `ret`
     */
    public static boolean PlayerIsOffline(CommandSender sender, String PlayerName, boolean ret)
    {
        return User.PlayerOnlyVariableMessage("PlayerIsOffline", sender, PlayerName, ret);
    }

    /**
     * Send a message to the player from messages.yml 
     * @param MessageName The message node from messages.yml
     * @param sender The person executing the command
     * @param thingy An IP address passed as the "Player" variable
     * @param ret The value to return from this function
     * @return The value provided as `ret`
     */
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

	/**
	 * Send a message to a player from messages.yml whose only argument is the player name
	 * @param MessageName The message node from messages.yml
	 * @param sender The person executing the command
	 * @param name The name of the player to use as a placeholder
	 * @param ret The value to return from this function
	 * @return The value provided as `ret`
	 */
  public
	static boolean PlayerOnlyVariableMessage(String MessageName, CommandSender sender, String name, boolean ret)
    {
        try 
        {
            sender.sendMessage(Messages.Translate(MessageName,
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("player", name);
                    put("ipaddress", name);
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
