/* 
 *  LolBans - The advanced banning system for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.api.punishment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.database.Database;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;

public class Punishment
{
    // private static LolBans self = LolBans.getPlugin(LolBans.class);
    // Our class variables
    private User player = null;
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
    private User Executioner = null;
    private boolean IsConsoleExectioner = false;

    // Appealed stuff
    private String AppealReason = null;
    private Timestamp AppealedTime = null;
    private User AppealStaff = null;
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
     */
    public Punishment(PunishmentType Type, User sender, User target, String Reason, Timestamp Expiry, Boolean silent) throws SQLException {
        // Not supported yet, will be in the future.
        if (Type == PunishmentType.PUNISH_REGEX || Type == PunishmentType.PUNISH_IP)
            throw new UnknownError("Unsupported Punishment type");

        this.Type = Type;
        this.player = target;
        this.uuid = target.getUniqueId();
        this.PlayerName = target.getName();
        this.TimePunished = TimeUtil.TimestampNow();
        this.IPAddress = target.getAddress() == null ? "#" : target.getAddress();
        this.Reason = Reason;
        this.Expiry = Expiry;
        this.silent = silent;
        if (sender.getUniqueId() == null || sender.getUniqueId().toString() == "CONSOLE") 
            this.IsConsoleExectioner = true;
        else 
            this.Executioner = LolBans.getOfflineUser(target.getUniqueId());   

        switch (Type)
        {
            case PUNISH_BAN:
            case PUNISH_KICK:
            case PUNISH_MUTE:
            case PUNISH_WARN: this.PID = PunishID.GenerateID(Database.GenID("lolbans_punishments")); break;
            case PUNISH_REGEX: this.PID = PunishID.GenerateID(Database.GenID("lolbans_regexbans")); break;
            case PUNISH_IP: this.PID = PunishID.GenerateID(Database.GenID("lolbans_ipbans")); break;
            default:
                throw new UnknownError("Unknown Punishment Type \"" + Type.displayName() + "\" for " + target.getName() + " " + Reason);
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
            PreparedStatement ps = Database.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE PunishID = ?");
            ps.setString(1, PunishmentID);

            Optional<ResultSet> ores = Database.ExecuteLater(ps).get();
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
                    p.Type = PunishmentType.fromOrdinal(res.getInt("Type"));
                    p.TimePunished = res.getTimestamp("TimePunished");
                    p.Expiry = res.getTimestamp("Expiry");
                    p.AppealReason = res.getString("AppealReason");
                    p.Appealed = res.getBoolean("Appealed");
                    p.WarningAcknowledged = res.getBoolean("WarningAck");
                    p.silent = res.getBoolean("Silent");

                    // Find players now.
                    p.player = LolBans.getOfflineUser(p.uuid);
                    String ArbiterUUID = res.getString("ArbiterUUID"),
                           AppelleeUUID = res.getString("AppelleeUUID");

                    if (ArbiterUUID != null)
                    {
                        if (ArbiterUUID.equalsIgnoreCase("CONSOLE"))
                            p.IsConsoleExectioner = true;
                        else
                            p.Executioner = LolBans.getOfflineUser(UUID.fromString(res.getString("ArbiterUUID")));
                    }

                    if (AppelleeUUID != null)
                    {
                        if (AppelleeUUID.equalsIgnoreCase("CONSOLE"))
                            p.IsConsoleAppealer = true;
                        else
                            p.AppealStaff = LolBans.getOfflineUser(UUID.fromString(res.getString("AppelleeUUID")));
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
    public static Optional<Punishment> FindPunishment(PunishmentType Type, User Player, boolean Appealed)
    {
        try
        {
            PreparedStatement pst3 = Database.connection.prepareStatement("SELECT PunishID FROM lolbans_punishments WHERE UUID = ? AND Type = ? AND Appealed = ?");
            pst3.setString(1, Player.getUniqueId().toString());
            pst3.setInt(2, Type.ordinal());
            pst3.setBoolean(3, Appealed);

            Optional<ResultSet> ores = Database.ExecuteLater(pst3).get();
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
    public void Commit(User sender)
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

                        InsertBan = Database.connection.prepareStatement("UPDATE lolbans_punishments SET UUID = ?,"
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
                        InsertBan = Database.connection.prepareStatement(String.format("INSERT INTO lolbans_punishments (UUID, PlayerName, IPAddress, Reason, ArbiterName, ArbiterUUID, PunishID, Expiry, Type, Silent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
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
                    // sender.sendMessage("Error");
                }
                return null;
            }
        });

        LolBans.pool.execute(t);
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
                    PreparedStatement pst2 = Database.connection.prepareStatement("DELETE FROM lolbans_punishments WHERE id = ?");
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

        LolBans.pool.execute(t);
    }


    /* ***********************************
     * Getters/Setters
     */
    /* clang-format: off */
    public User GetPlayer() { return this.player; }
    public String GetPunishmentID() { return this.PID; }
    public UUID GetUUID() { return this.uuid; }
    public String GetPlayerName() { return this.PlayerName; }
    public String GetIPAddress() { return this.IPAddress; }
    public String GetReason() { return this.Reason; }
    public PunishmentType GetPunishmentType() { return this.Type; }
    public Timestamp GetTimePunished() { return this.TimePunished; }
    public Timestamp GetExpiry() { return this.Expiry; }
    public User GetExecutioner() { return this.Executioner; }
    public String GetExecutionerName() { return this.IsConsoleExectioner ? "CONSOLE" : this.Executioner.getName(); }
    public boolean IsConsoleExectioner() { return this.IsConsoleExectioner; }
    public String GetAppealReason() { return this.AppealReason; }
    public Timestamp GetAppealTime() { return this.AppealedTime; }
    public User GetAppealStaff() { return this.AppealStaff; }
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
    // public void SetAppealStaff(User sender) { this.SetAppealStaff(sender instanceof User ? (User)sender : null); }
    public void SetAppealStaff(User player)
    {
        if (player.getUniqueId() == null)
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