package com.ristexsoftware.lolbans.Objects;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.MojangUtil;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Utils.MojangUtil.MojangUser;
import com.ristexsoftware.lolbans.Utils.PunishID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Punishment
{
    private static Main self = Main.getPlugin(Main.class);
    // Our class variables
    private OfflinePlayer player = null;
    private String DatabaseID = null;
    private String PID = null;
    private UUID uuid = null;
    private String PlayerName = null;
    private String IPAddress = null;
    private String Reason = null;
    private PunishmentType Type = null;
    private boolean silent = false;
    private Timestamp TimePunished = null;
    private Timestamp Expiry = null;
    // If Executioner is null but IsConsole is true, it's a console punishment.
    private OfflinePlayer Executioner = null;
    private boolean IsConsoleExectioner = false;

    // Appealed stuff
    private String AppealReason = null;
    private Timestamp AppealedTime = null;
    private OfflinePlayer AppealStaff = null;
    private boolean IsConsoleAppealer = false;
    private boolean Appealed = false;
    private boolean WarningAcknowledged = false;

    // Used in FindPunishment
    private Punishment() {}

    /**
     * Create a new punishment to commit to the database
     * 
     * @param Type   The type of punishment to create
     * @param senderUUID The UUID of the person that is creating the punishment
     * @param target Who (or what) is being punished
     * @param Reason The reason for the punishment
     * @param Expiry When the punishment expires (if applicable, null if permanent)
     * @param silent If the punishment is silent
     * @throws SQLException         If it cannot communicate with the SQL database
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Punishment(PunishmentType Type, String senderUUID, OfflinePlayer target, String Reason, Timestamp Expiry, Boolean silent) throws SQLException, InterruptedException, ExecutionException {
        // Not supported yet, will be in the future.
        if (Type == PunishmentType.PUNISH_REGEX || Type == PunishmentType.PUNISH_IP)
            throw new UnknownError("Unsupported Punishment type");

        this.Type = Type;
        this.player = target;
        this.uuid = target.getUniqueId();
        this.PlayerName = target.getName();
        this.TimePunished = TimeUtil.TimestampNow();
        this.IPAddress = target.isOnline() ? ((Player)target).getAddress().getAddress().getHostAddress()
             : User.getLastIP(target.getUniqueId().toString()).get() == null ? "#" : User.getLastIP(target.getUniqueId().toString()).get(); // Lets see if we can get an IP from the users table
        this.Reason = Reason;
        this.Expiry = Expiry;
        this.silent = silent;
        
        if (senderUUID == null)
            this.IsConsoleExectioner = true;
        else {
            MojangUser mUser = new MojangUtil().resolveUser(senderUUID);
            this.Executioner = Bukkit.getOfflinePlayer(mUser.getName());   
        }

        switch (Type)
        {
            case PUNISH_BAN:
            case PUNISH_KICK:
            case PUNISH_MUTE:
            case PUNISH_WARN: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("lolbans_punishments")); break;
            case PUNISH_REGEX: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("lolbans_regexbans")); break;
            case PUNISH_IP: this.PID = PunishID.GenerateID(DatabaseUtil.GenID("lolbans_ipbans")); break;
            default:
                throw new UnknownError("Unknown Punishment Type \"" + Type.DisplayName() + "\" for " + target.getName() + " " + Reason);
        }
    }

    /**
     * Find a punishment based on it's ID
     * @param PunishmentID The ID of the punishment
     * @return The punishment if found
     */
    public static Optional<Punishment> FindPunishment(String PunishmentID)
    {
        try 
        {
            PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE PunishID = ?");
            ps.setString(1, PunishmentID);

            Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();
            if (ores.isPresent())
            {
                ResultSet res = ores.get();
                if (res.next())
                {
                    // Fill in our structure.
                    Punishment p = new Punishment();
                    p.PID = res.getString("PunishID");
                    p.DatabaseID = res.getString("id");
                    p.uuid = UUID.fromString(res.getString("UUID"));
                    p.PlayerName = res.getString("PlayerName");
                    p.IPAddress = res.getString("IPAddress");
                    p.Reason = res.getString("Reason");
                    p.Type = PunishmentType.FromOrdinal(res.getInt("Type"));
                    p.TimePunished = res.getTimestamp("TimePunished");
                    p.Expiry = res.getTimestamp("Expiry");
                    p.AppealReason = res.getString("AppealReason");
                    p.Appealed = res.getBoolean("Appealed");
                    p.WarningAcknowledged = res.getBoolean("WarningAck");
                    p.silent = res.getBoolean("Silent");

                    // Find players now.
                    p.player = Bukkit.getOfflinePlayer(p.uuid);
                    String ArbiterUUID = res.getString("ArbiterUUID"),
                           AppelleeUUID = res.getString("AppelleeUUID");

                    if (ArbiterUUID != null)
                    {
                        if (ArbiterUUID.equalsIgnoreCase("CONSOLE"))
                            p.IsConsoleExectioner = true;
                        else
                            p.Executioner = Bukkit.getOfflinePlayer(UUID.fromString(res.getString("ArbiterUUID")));
                    }

                    if (AppelleeUUID != null)
                    {
                        if (AppelleeUUID.equalsIgnoreCase("CONSOLE"))
                            p.IsConsoleAppealer = true;
                        else
                            p.AppealStaff = Bukkit.getOfflinePlayer(UUID.fromString(res.getString("AppelleeUUID")));
                    }

                    return Optional.of(p);
                }
            }
        }
        catch (SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Find a punishment based on it's type and who was punished
     * @param Type The type of punishment to look for
     * @param Player The player being punished
     * @param Appealed Whether the punishment was appealed
     * @return The punishment if found.
     */
    public static Optional<Punishment> FindPunishment(PunishmentType Type, OfflinePlayer Player, boolean Appealed)
    {
        try
        {
            PreparedStatement pst3 = self.connection.prepareStatement("SELECT PunishID FROM lolbans_punishments WHERE UUID = ? AND Type = ? AND Appealed = ?");
            pst3.setString(1, Player.getUniqueId().toString());
            pst3.setInt(2, Type.ordinal());
            pst3.setBoolean(3, Appealed);

            Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(pst3).get();
            if (!ores.isPresent())
                return Optional.empty();

            ResultSet result = ores.get();
            if (!result.next())
                return Optional.empty();

            return Punishment.FindPunishment(result.getString("PunishID"));
        }
        catch (SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Commit the punishment to the database.
     * @param sender The command sender to notify if an error occures.
     */
    public void Commit(CommandSender sender)
    {
        Punishment me = this;
        FutureTask<Void> t = new FutureTask<>(new Callable<Void>()
        {
            @Override
            public Void call()
            {
                //This is where you should do your database interaction
                try 
                {
                    int i = 1;
                    PreparedStatement InsertBan = null;
                    if (me.DatabaseID != null)
                    {
                        if (me.Appealed && me.AppealedTime == null)
                            me.AppealedTime = TimeUtil.TimestampNow();

                        InsertBan = self.connection.prepareStatement("UPDATE lolbans_punishments SET UUID = ?,"
                                                                    +"PlayerName = ?,"
                                                                    +"IPAddress = ?,"
                                                                    +"Reason = ?,"
                                                                    +"ArbiterName = ?,"
                                                                    +"ArbiterUUID = ?,"
                                                                    +"PunishID = ?,"
                                                                    +"Expiry = ?, "
                                                                    +"Type = ?,"
                                                                    +"TimePunished = ?,"
                                                                    +"AppealReason = ?,"
                                                                    +"AppelleeName = ?,"
                                                                    +"AppelleeUUID = ?,"
                                                                    +"AppealTime = ?,"
                                                                    +"Appealed = ?,"
                                                                    +"Silent = ?,"
                                                                    +"WarningAck = ? "
                                                                    +"WHERE id = ?");
                        InsertBan.setString(i++, me.uuid.toString()); // UUID
                        InsertBan.setString(i++, me.PlayerName); // PlayerName
                        InsertBan.setString(i++, me.IPAddress); // IP Address
                        InsertBan.setString(i++, me.Reason); // Reason
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : Executioner.getName().toString()); // ArbiterName 
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : me.Executioner.getUniqueId().toString()); // ArbiterUUID
                        InsertBan.setString(i++, me.PID); // PunishID
                        InsertBan.setTimestamp(i++, Expiry); // Expiry
                        InsertBan.setInt(i++, Type.ordinal());
                        InsertBan.setTimestamp(i++, me.TimePunished); // TimePunished
                        InsertBan.setString(i++, me.AppealReason); // AppealReason 
                        InsertBan.setString(i++, me.IsConsoleAppealer ? "CONSOLE" : me.AppealStaff.getName()); // AppelleeName
                        InsertBan.setString(i++, me.IsConsoleAppealer ? "CONSOLE" : me.AppealStaff.getUniqueId().toString()); // AppelleeUUID
                        InsertBan.setTimestamp(i++, me.AppealedTime); //AppealTime
                        InsertBan.setBoolean(i++, me.Appealed); //Appealed
                        InsertBan.setBoolean(i++, me.silent); //Silent
                        InsertBan.setBoolean(i++, me.WarningAcknowledged); //WarningAck
                        InsertBan.setString(i++, me.DatabaseID); //id
                    }
                    else
                    {
                        // Preapre a statement
                        InsertBan = self.connection.prepareStatement(String.format("INSERT INTO lolbans_punishments (UUID, PlayerName, IPAddress, Reason, ArbiterName, ArbiterUUID, PunishID, Expiry, Type, Silent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
                        InsertBan.setString(i++, me.uuid.toString());
                        InsertBan.setString(i++, me.PlayerName);
                        InsertBan.setString(i++, me.IPAddress);
                        InsertBan.setString(i++, me.Reason);
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : Executioner.getName().toString());
                        InsertBan.setString(i++, me.IsConsoleExectioner ? "CONSOLE" : me.Executioner.getUniqueId().toString());
                        InsertBan.setString(i++, me.PID);
                        InsertBan.setTimestamp(i++, Expiry);
                        InsertBan.setInt(i++, Type.ordinal());
                        InsertBan.setBoolean(i++, silent);
                    }
                    InsertBan.executeUpdate();
                } 
                catch (Throwable e)
                {
                    e.printStackTrace();
                    sender.sendMessage(Messages.ServerError);
                }
                return null;
            }
        });

        Main.pool.execute(t);
    }

    /**
     * Erase this punishment record from the database.
     * NOTE: This should not be used if you are unbanning someone.
     */
    public void Delete()
    {
        Punishment me = this;
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>()
        {
            @Override
            public Boolean call()
            {
                //This is where you should do your database interaction
                try 
                {
                    // Preapre a statement
                    PreparedStatement pst2 = self.connection.prepareStatement("DELETE FROM lolbans_punishments WHERE id = ?");
                    pst2.setString(1, me.DatabaseID);
                    pst2.executeUpdate();

                    // Nullify everything!
                    me.player = null;
                    me.DatabaseID = null;
                    me.PID = null;
                    me.uuid = null;
                    me.PlayerName = null;
                    me.IPAddress = null;
                    me.Reason = null;
                    me.Type = null;
                    me.TimePunished = null;
                    me.Expiry = null;
                    // If Executioner is null but IsConsole is true, it's a console punishment.
                    me.Executioner = null;
                    me.IsConsoleExectioner = false;
                
                    // Appealed stuff
                    me.AppealReason = null;
                    me.AppealedTime = null;
                    me.AppealStaff = null;
                    me.IsConsoleAppealer = false;
                    me.Appealed = false;
                    me.silent = false;
                    me.WarningAcknowledged = false;
                } 
                catch (SQLException e) 
                {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        Main.pool.execute(t);
    }


    /* ***********************************
     * Getters/Setters
     */
    /* clang-format: off */
    public OfflinePlayer GetPlayer() { return this.player; }
    public String GetPunishmentID() { return this.PID; }
    public UUID GetUUID() { return this.uuid; }
    public String GetPlayerName() { return this.PlayerName; }
    public String GetIPAddress() { return this.IPAddress; }
    public String GetReason() { return this.Reason; }
    public PunishmentType GetPunishmentType() { return this.Type; }
    public Timestamp GetTimePunished() { return this.TimePunished; }
    public Timestamp GetExpiry() { return this.Expiry; }
    public OfflinePlayer GetExecutioner() { return this.Executioner; }
    public String GetExecutionerName() { return this.IsConsoleExectioner ? "CONSOLE" : this.Executioner.getName(); }
    public boolean IsConsoleExectioner() { return this.IsConsoleExectioner; }
    public String GetAppealReason() { return this.AppealReason; }
    public Timestamp GetAppealTime() { return this.AppealedTime; }
    public OfflinePlayer GetAppealStaff() { return this.AppealStaff; }
    public boolean IsConsoleAppealer() { return this.IsConsoleAppealer; }
    public boolean GetAppealed() { return this.Appealed; }
    public boolean GetSilent() { return this.silent; }
    public boolean AcknowledgedWarning() { return this.WarningAcknowledged; }

    public String GetExpiryString() { return this.Expiry != null ? this.Expiry.toString() : ""; }

    // Setters
    public void SetAppealReason(String Reason) { this.AppealReason = Reason; }
    public void SetAppealTime(Timestamp time) { this.AppealedTime = time; }
    public void SetAppealed(Boolean value) { this.Appealed = value; }
    public void SetSilent(Boolean value) { this.silent = value; }
    public void SetWarningAcknowledged(Boolean value) { this.WarningAcknowledged = value; }
    public void SetAppealStaff(CommandSender sender) { this.SetAppealStaff(sender instanceof OfflinePlayer ? (OfflinePlayer)sender : null); }
    public void SetAppealStaff(OfflinePlayer player)
    {
        if (player == null)
        {
            this.AppealStaff = null;
            this.IsConsoleAppealer = true;
        }
        else
        {
            this.AppealStaff = player;
            this.IsConsoleAppealer = false;
        }
    }
}