/* 
 *  LolBans - An advanced punishment management system made for Minecraft
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
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;

import inet.ipaddr.IPAddress;
import lombok.Getter;
import lombok.Setter;

import com.ristexsoftware.lolbans.api.utils.PunishID;

public class Punishment {
    @Getter @Setter private User target; // Nullable
    @Getter @Setter private IPAddress ipAddress;

    @Getter @Setter private String reason;
    @Getter @Setter private String punishId;
    @Getter @Setter private PunishmentType type;
    @Getter @Setter private Timestamp timePunished;
    @Getter @Setter private Timestamp expiresAt;
    @Getter @Setter private Timestamp commitPunishmentBy;
    @Getter @Setter private Boolean commited = false;

    @Getter @Setter private User punisher;
    
    @Getter @Setter private Boolean appealed = false;
    @Getter @Setter private User unpunisher;
    @Getter @Setter private String appealReason;
    @Getter @Setter private Timestamp appealedAt;
    
    @Getter @Setter private Boolean silent = false;

    
    @Getter @Setter private Boolean warningAck = false;
    
    @Getter @Setter private Boolean ipBan = false;
    
    @Getter @Setter private Boolean regexBan = false;
    @Getter @Setter private String regex;
    
    @Getter @Setter private Boolean banwave = false;
    
    public Punishment(PunishmentType type, User sender, User target, String reason, Timestamp expiry, Boolean silent, Boolean appealed) throws SQLException, InvalidPunishmentException {
        if (type == PunishmentType.UNKNOWN) {
            throw new InvalidPunishmentException("Unknown Punishment Type \"" + type.displayName() + "\" for " + target.getName() + " " + reason);
        }
        this.type = type;
        this.punishId = PunishID.generateID(Database.genID("lolbans_punishments"));
        this.reason = reason;

        // Users
        setTarget(target);
        setPunisher(sender);

        // Punish timestamps
        this.timePunished = TimeUtil.TimestampNow();
        this.expiresAt = expiry;

        // Appeals
        this.appealed = appealed;
    }

    /**
     * Construct a regex ban punishment.
     * 
     * @throws InvalidPunishmentException
     */
    public Punishment(User sender, String reason, Timestamp expiry, Boolean silent, Boolean appealed, String regex) throws SQLException, InvalidPunishmentException {
        this(PunishmentType.REGEX, sender, null, reason, expiry, silent, appealed);
        this.regexBan = true;
        this.regex = regex;
    }
    
    /**
     * Construct an IP ban punishment.
     * 
     * @throws InvalidPunishmentException
     */
    public Punishment(User sender, String reason, Timestamp expiry, Boolean silent, Boolean appealed, IPAddress ipaddress) throws SQLException, InvalidPunishmentException {
        this(PunishmentType.IP, sender, null, reason, expiry, silent, appealed);
        this.ipBan = true;
        this.ipAddress = ipaddress;
    }

    private Punishment() {} // Empty punishment - used by findPunishment

    /**
    * Find a punishment based on it's ID
    * @param id The ID of the punishment
    * @return The punishment if found
    */
    public static Optional<Punishment> findPunishment(String id)
    {
        try
        {
            PreparedStatement ps = Database.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE punish_id = ?");
            ps.setString(1, id);

            Optional<ResultSet> result = Database.ExecuteLater(ps).get();
            if (result.isPresent())
            {
                ResultSet res = result.get();
                if (res.next())
                {
                    // Fill in our structure.
                    Punishment p = new Punishment();

                    p.punishId = res.getString("punish_id");
                    p.type = PunishmentType.fromOrdinal(res.getInt("type"));
                    p.reason = res.getString("reason");
                        
                    p.target = LolBans.getUser(UUID.fromString(res.getString("target_uuid")));
                    if (p.target == null) {
                        p.target = new User(res.getString("target_name"), UUID.fromString(res.getString("target_uuid")));
                    }
                    
                    p.timePunished = res.getTimestamp("time_punished");
                    p.expiresAt = res.getTimestamp("expires_at");

                    p.appealed = res.getBoolean("appealed");
                    p.appealReason = res.getString("appeal_reason");

                    p.warningAck = res.getBoolean("WarningAck");

                    p.silent = res.getBoolean("Silent");


                    String punisher_uuid = res.getString("punisher_uuid"),
                        unpunisher_uuid = res.getString("unpunisher_uuid");
                    
                    if (punisher_uuid != null)
                    {   
                        if (punisher_uuid.equalsIgnoreCase("00000000-0000-0000-0000-000000000000"))
                            p.punisher = User.getConsoleUser();
                        else
                            p.punisher = LolBans.getUser(UUID.fromString(res.getString("punisher_uuid")));
                    }

                    if (unpunisher_uuid != null)
                    {   
                        if (unpunisher_uuid.equalsIgnoreCase("00000000-0000-0000-0000-000000000000"))
                            p.punisher = User.getConsoleUser();
                        else
                            LolBans.getUser(UUID.fromString(res.getString("unpunisher_uuid")));
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
    public static Optional<Punishment> findPunishment(PunishmentType Type, User Player, boolean Appealed)
    {
        try
        {
            PreparedStatement pst3 = Database.connection.prepareStatement("SELECT punish_id FROM lolbans_punishments WHERE target_uuid = ? AND type = ? AND appealed = ?");
            pst3.setString(1, Player.getUniqueId().toString());
            pst3.setInt(2, Type.ordinal());
            pst3.setBoolean(3, Appealed);

            Optional<ResultSet> ores = Database.ExecuteLater(pst3).get();
            if (!ores.isPresent())
                return Optional.empty();

            ResultSet result = ores.get();
            if (!result.next())
                return Optional.empty();

            return Punishment.findPunishment(result.getString("punish_id"));
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
                    
                    if (me.commited)
                    {
                        if (me.appealed && me.appealedAt == null)
                            me.appealedAt = TimeUtil.TimestampNow();

                        InsertBan = Database.connection.prepareStatement("UPDATE lolbans_punishments SET target_uuid = ?,"
                                                                    +"target_name = ?,"
                                                                    +"target_ip_address = ?,"
                                                                    +"reason = ?,"
                                                                    +"punished_by_name = ?,"
                                                                    +"punished_by_uuid = ?,"
                                                                    +"punish_id = ?,"
                                                                    +"expires_at = ?, "
                                                                    +"type = ?,"
                                                                    +"time_punished = ?,"
                                                                    +"appeal_reason = ?,"
                                                                    +"appealed_by_name = ?,"
                                                                    +"appealed_by_uuid = ?,"
                                                                    +"appealed_at = ?,"
                                                                    +"appealed = ?,"
                                                                    +"silent = ?,"
                                                                    +"warning_ack = ?,"
                                                                    +"ip_ban = ?,"
                                                                    +"regex = ?,"
                                                                    +"regex_ban = ?,"
                                                                    +"banwave = ?,"
                                                                    +"WHERE punish_id = ?");
                                                                    
                        InsertBan.setString(i++, me.target == null ? null : me.target.getUniqueId().toString()); // UUID
                        InsertBan.setString(i++, me.target == null ? null : me.target.getName()); // PlayerName
                        InsertBan.setString(i++, me.ipAddress.toString()); // IP Address
                        InsertBan.setString(i++, me.reason); // Reason
                        InsertBan.setString(i++, me.punisher.getName()); // ArbiterName 
                        InsertBan.setString(i++, me.punisher.getUniqueId().toString()); // ArbiterUUID
                        InsertBan.setString(i++, me.punishId); // PunishID
                        InsertBan.setTimestamp(i++, me.expiresAt); // Expiry
                        InsertBan.setInt(i++, me.type.ordinal());
                        InsertBan.setTimestamp(i++, me.timePunished); // TimePunished
                        InsertBan.setString(i++, me.appealReason); // AppealReason 
                        InsertBan.setString(i++, me.unpunisher == null ? null : me.unpunisher.getName()); // AppelleeName
                        InsertBan.setString(i++, me.unpunisher == null ? null : me.unpunisher.getUniqueId().toString()); // AppelleeUUID
                        InsertBan.setTimestamp(i++, me.appealedAt); //AppealTime
                        InsertBan.setBoolean(i++, me.appealed); //Appealed
                        InsertBan.setBoolean(i++, me.silent); //Silent
                        InsertBan.setBoolean(i++, me.warningAck); //WarningAck
                        InsertBan.setBoolean(i++, me.ipBan); //is this an ipban?
                        InsertBan.setString(i++, me.regex); //regex
                        InsertBan.setBoolean(i++, me.regexBan); //regex ban?
                        InsertBan.setBoolean(i++, me.banwave); //banwave
                        InsertBan.setString(i++, me.punishId); //id
                    }
                    else
                    {
                        // Preapre a statement
                        InsertBan = Database.connection.prepareStatement(String.format("INSERT INTO lolbans_punishments ("
                                                                                    + "target_uuid,"
                                                                                    + "target_name,"
                                                                                    + "target_ip_address,"
                                                                                    + "reason,"
                                                                                    + "punished_by_name,"
                                                                                    + "punished_by_uuid,"
                                                                                    + "punish_id,"
                                                                                    + "expires_at,"
                                                                                    + "type,"
                                                                                    + "silent,"
                                                                                    + "ip_ban,"
                                                                                    + "regex,"
                                                                                    + "regex_ban,"
                                                                                    + "banwave)"
                                                                                    + " VALUES"
                                                                                            + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
                        InsertBan.setString(i++, me.target == null ? "" : me.target.getUniqueId().toString());
                        InsertBan.setString(i++, me.target == null ? "" : me.target.getName());
                        InsertBan.setString(i++, me.ipAddress == null ? "#" : me.ipAddress.toString());
                        InsertBan.setString(i++, me.reason);
                        InsertBan.setString(i++, me.punisher.getName());
                        InsertBan.setString(i++, me.punisher.getUniqueId().toString());
                        InsertBan.setString(i++, me.punishId);
                        InsertBan.setTimestamp(i++, me.expiresAt);
                        InsertBan.setInt(i++, me.type.ordinal());
                        InsertBan.setBoolean(i++, silent);
                        InsertBan.setBoolean(i++, ipBan);
                        InsertBan.setString(i++, regex);
                        InsertBan.setBoolean(i++, regexBan);
                        InsertBan.setBoolean(i++, banwave);
                    }
                    InsertBan.executeUpdate();

                    commited = true;
                } 
                catch (Throwable e)
                {
                    e.printStackTrace();
                    if (sender == null)
                        LolBans.getLogger().severe(Messages.serverError);
                    else
                        sender.sendMessage(Messages.serverError);
                }
                return null;
            }
        });

        LolBans.pool.execute(t);
    }
}
