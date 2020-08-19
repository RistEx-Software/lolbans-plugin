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
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.sql.Connection;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.api.utils.Cacheable;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Getter;
import lombok.Setter;

import com.ristexsoftware.lolbans.api.utils.PunishID;

public class Punishment implements Cacheable {
    private static LolBans self = LolBans.getPlugin();
    @Getter @Setter private User target; // Nullable
    @Getter @Setter private IPAddress ipAddress;

    @Getter @Setter private String reason;
    @Getter @Setter private String punishID;
    @Getter @Setter private PunishmentType type;
    @Getter @Setter private Timestamp timePunished;
    @Getter @Setter private Timestamp expiresAt;
    @Getter @Setter private Timestamp commitPunishmentBy;
    @Getter @Setter private Boolean commited = false;

    @Getter @Setter private User punisher;
    
    @Getter @Setter private Boolean appealed = false;
    @Getter @Setter private User appealedBy;
    @Getter @Setter private String appealReason;
    @Getter @Setter private Timestamp appealedAt;
    
    @Getter @Setter private Boolean silent = false;
    
    @Getter @Setter private Boolean warningAck = false;
    
    @Getter @Setter private String regex;
    
    public Punishment(PunishmentType type, User sender, User target, String reason, Timestamp expiry, Boolean silent, Boolean appealed) throws SQLException, InvalidPunishmentException {
        if (type == PunishmentType.UNKNOWN) {
            throw new InvalidPunishmentException("Unknown Punishment Type \"" + type.displayName() + "\" for " + target.getName() + " " + reason);
        }
        this.type = type;
        this.punishID = PunishID.generateID(Database.generateId("lolbans_punishments"));
        this.reason = reason;

        // Users
        setTarget(target);
        setPunisher(sender);

        // Punish timestamps
        this.timePunished = TimeUtil.now();
        this.expiresAt = expiry;

        // Appeals
        this.appealed = appealed;
        this.silent = silent;
    }

    /**
     * Construct a regex ban punishment.
     * 
     * @throws InvalidPunishmentException
     */
    public Punishment(User sender, String reason, Timestamp expiry, Boolean silent, Boolean appealed, String regex) throws SQLException, InvalidPunishmentException {
        this(PunishmentType.REGEX, sender, null, reason, expiry, silent, appealed);
        this.regex = regex;
    }
    
    /**
     * Construct an IP ban punishment.
     * 
     * @throws InvalidPunishmentException
     */
    public Punishment(User sender, String reason, Timestamp expiry, Boolean silent, Boolean appealed, IPAddress ipAddress) throws SQLException, InvalidPunishmentException {
        this(PunishmentType.IP, sender, null, reason, expiry, silent, appealed);
        this.ipAddress = ipAddress;
    }

    private Punishment() {} // Empty punishment - used by findPunishment

    /**
    * Find a punishment based on it's ID
    * @param id The ID of the punishment
    * @return The punishment if found
    */
    public static Punishment findPunishment(String id)
    {
        try
        {
            PreparedStatement ps = Database.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE punish_id = ?");
            ps.setString(1, id);

            Optional<ResultSet> result = Database.executeLater(ps).get();
            if (result.isPresent())
            {
                ResultSet res = result.get();
                if (res.next())
                {
                    // Fill in our structure.
                    Punishment p = new Punishment();

                    p.punishID = res.getString("punish_id");
                    p.type = PunishmentType.fromOrdinal(res.getInt("type"));
                    p.reason = res.getString("reason");
                    
                    try {
                        p.target = LolBans.getPlugin().getUser(UUID.fromString(res.getString("target_uuid")));
                        if (p.target == null) {
                            p.target = new User(res.getString("target_name"), UUID.fromString(res.getString("target_uuid")));
                        }
                    } catch (IllegalArgumentException e) {
                    }
                             
                    p.timePunished = res.getTimestamp("time_punished");
                    p.expiresAt = res.getTimestamp("expires_at");

                    p.appealed = res.getBoolean("appealed");
                    p.appealReason = res.getString("appeal_reason");
                    
                    // Per-punish type fields   
                    p.warningAck = res.getBoolean("warning_ack");

                    try {
                        p.ipAddress = new IPAddressString(res.getString("target_ip_address")).toAddress();
                    } catch(Exception e) {}
             
                    p.regex = res.getString("regex");

                    p.silent = res.getBoolean("silent");


                    String punished_by_uuid = res.getString("punished_by_uuid"),
                        appealed_by_uuid = res.getString("appealed_by_uuid");
                    
                    if (punished_by_uuid != null)
                    {   
                        if (punished_by_uuid.equalsIgnoreCase("00000000-0000-0000-0000-000000000000"))
                            p.punisher = User.getConsoleUser();
                        else
                            p.punisher = LolBans.getPlugin().getUser(UUID.fromString(res.getString("punished_by_uuid")));
                    }

                    if (appealed_by_uuid != null)
                    {   
                        if (appealed_by_uuid.equalsIgnoreCase("00000000-0000-0000-0000-000000000000"))
                            p.punisher = User.getConsoleUser();
                        else
                            LolBans.getPlugin().getUser(UUID.fromString(res.getString("appealed_by_uuid")));
                    }

                    return Optional.of(p).get();
                }
            }
        }
        catch (SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Find a punishment based on it's type and who was punished
     * @param type The type of punishment to look for
     * @param search The key to search for
     * @param appealed Whether the punishment was appealed
     * @return The punishment if found.
     */
    public static Punishment findPunishment(PunishmentType type, String search, boolean appealed)
    {
        try
        {
            PreparedStatement pst3 = Database.connection.prepareStatement("SELECT punish_id FROM lolbans_punishments WHERE (target_uuid = ? OR regex = ? OR target_ip_address = ?) AND type = ? AND appealed = ? ORDER BY time_punished DESC");
            pst3.setString(1, search);
            pst3.setString(2, search);
            pst3.setString(3, search);
            pst3.setInt(4, type.ordinal());
            pst3.setBoolean(5, appealed);

            Optional<ResultSet> ores = Database.executeLater(pst3).get();
            if (!ores.isPresent())
                return null;

            ResultSet result = ores.get();
            if (!result.next())
                return null;

            return Punishment.findPunishment(result.getString("punish_id"));
        }
        catch (SQLException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Commit the punishment to the database.
     * @param sender The command sender to notify if an error occures.
     */
    public void commit(User sender)
    {
        Punishment me = this;
        FutureTask<Void> t = new FutureTask<>(new Callable<Void>()
        {
            @Override
            public Void call()
            {

                if (!Database.isConnected()) {
                    Database.addPunishmentToQueue(me);
                    return null;
                }

                Connection connection = Database.getConnection();

                //This is where you should do your database interaction
                try 
                {
                    int i = 1;
                    PreparedStatement insertBanStatement = null;
                    
                    if (me.commited)
                    {
                        if (me.appealed && me.appealedAt == null)
                            me.appealedAt = TimeUtil.now();

                        insertBanStatement = connection.prepareStatement("UPDATE lolbans_punishments SET target_uuid = ?,"
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
                                                                    +"regex = ?"
                                                                    +" WHERE punish_id = ?");
                                                                    
                                                                    insertBanStatement.setString(i++, me.target == null ? null : me.target.getUniqueId().toString()); // UUID
                                                                    insertBanStatement.setString(i++, me.target == null ? null : me.target.getName()); // PlayerName
                                                                    insertBanStatement.setString(i++, me.ipAddress == null ? "#" : me.ipAddress.toString()); // IP Address
                        insertBanStatement.setString(i++, me.reason); // Reason
                        insertBanStatement.setString(i++, me.punisher.getName()); // ArbiterName 
                        insertBanStatement.setString(i++, me.punisher.getUniqueId().toString()); // ArbiterUUID
                        insertBanStatement.setString(i++, me.punishID); // PunishID
                        insertBanStatement.setTimestamp(i++, me.expiresAt); // Expiry
                        insertBanStatement.setInt(i++, me.type.ordinal());
                        insertBanStatement.setTimestamp(i++, me.timePunished); // TimePunished
                        insertBanStatement.setString(i++, me.appealReason); // AppealReason 
                        insertBanStatement.setString(i++, me.appealedBy == null ? null : me.appealedBy.getName()); // AppelleeName
                        insertBanStatement.setString(i++, me.appealedBy == null ? null : me.appealedBy.getUniqueId().toString()); // AppelleeUUID
                        insertBanStatement.setTimestamp(i++, me.appealedAt); //AppealTime
                        insertBanStatement.setBoolean(i++, me.appealed); //Appealed
                        insertBanStatement.setBoolean(i++, me.silent); //Silent
                        insertBanStatement.setBoolean(i++, me.warningAck); //WarningAck
                        insertBanStatement.setString(i++, me.regex); //regex
                        insertBanStatement.setString(i++, me.punishID); //id
                    }
                    else
                    {
                        // Preapre a statement
                        insertBanStatement = Database.connection
                                .prepareStatement(String.format("INSERT INTO lolbans_punishments (" + "target_uuid,"
                                        + "target_name," + "target_ip_address," + "reason," + "punished_by_name,"
                                        + "punished_by_uuid," + "punish_id," + "expires_at," + "type," + "silent,"
                                        + "regex)" + " VALUES" + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
                        insertBanStatement.setString(i++, me.target == null ? "" : me.target.getUniqueId().toString());
                        insertBanStatement.setString(i++, me.target == null ? "" : me.target.getName());
                        insertBanStatement.setString(i++, me.ipAddress == null ? "#" : me.ipAddress.toString());
                        insertBanStatement.setString(i++, me.reason);
                        insertBanStatement.setString(i++, me.punisher.getName());
                        insertBanStatement.setString(i++, me.punisher.getUniqueId().toString());
                        insertBanStatement.setString(i++, me.punishID);
                        insertBanStatement.setTimestamp(i++, me.expiresAt);
                        insertBanStatement.setInt(i++, me.type.ordinal());
                        insertBanStatement.setBoolean(i++, silent);
                        insertBanStatement.setString(i++, regex);
                    }

                    insertBanStatement.executeUpdate();

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

        LolBans.getPlugin().getPunishmentCache().update(this);
        LolBans.getPlugin().getPool().execute(t);
    }

    public void update() {
        setCommited(true);
        commit(User.getConsoleUser());
        // LolBans.getPlugin().getPunishmentCache().update(this);
    }

    public TreeMap<String, String> getVariableMap() {
        return new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
            {
                put("player", getTarget() == null ? null : getTarget().getName());
                put("ipaddress", getIpAddress() == null ? "#" : getIpAddress().toString());
                put("reason", getAppealed() ? getAppealReason() : getReason());
                put("arbiter", getAppealed() ? getAppealedBy().getName() : getPunisher().getName());
                put("expiry", getExpiresAt() == null ? "" : getExpiresAt().toString());
                put("silent", Boolean.toString(getSilent()));
                put("appealed",Boolean.toString(getAppealed()));
                put("expires",Boolean.toString(getExpiresAt() != null && !getAppealed()));
                put("punishid", getPunishID());
                put("warningack", Boolean.toString(getWarningAck()));
                put("regex", getRegex() == null ? "" : getRegex());
                put("type", getType().displayName());
            }
            
        };
    }

    /**
     * Erase this punishment record from the database.
     * NOTE: This should not be used if you are unbanning someone.
     */
    public void delete()
    {
        Punishment me = this;
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                //This is where you should do your database interaction
                try {
                    // Preapre a statement
                    PreparedStatement pst2 = Database.connection
                            .prepareStatement("DELETE FROM lolbans_punishments WHERE punish_id = ?");
                    pst2.setString(1, getPunishID());
                    pst2.executeUpdate();

                    // Nullify everything!
                    me.target = null;
                    me.ipAddress = null;
                    me.type = null;
                    me.timePunished = null;
                    me.expiresAt = null;
                    me.commitPunishmentBy = null;
                    me.commited = false;
                    me.punisher = null;
                    me.appealed = false;
                    me.appealedBy = null;
                    me.appealReason = null;
                    me.appealedAt = null;
                    me.silent = false;
                    me.warningAck = false;
                    me.regex = null;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        LolBans.getPlugin().getPool().execute(t);
    }

    public void broadcast() {
        PunishmentType type = this.getType();
        TreeMap<String, String> vars = getVariableMap();
        try {
            switch(type) {
                case BAN:
                    self.broadcastEvent(Messages.translate("ban.ban-announcement", vars), silent);
                    break;
                case MUTE:
                    self.broadcastEvent(Messages.translate("mute.mute-announcement", vars), silent);
                    break;
                case KICK:
                    self.broadcastEvent(Messages.translate("kick.kick-announcement", vars), silent);
                    break;
                case WARN:
                    self.broadcastEvent(Messages.translate("warn.warn-announcement", vars), silent);
                    break;
                case IP:
                    self.broadcastEvent(Messages.translate("ip-ban.ban-announcement", vars), silent);
                    break;
                case REGEX:
                    self.broadcastEvent(Messages.translate("regex-ban.ban-announcement", vars), silent);
                    break;
                case BANWAVE:
                    if (getAppealed()) 
                        self.broadcastEvent(Messages.translate("ban-wave.removed-from-wave", vars), silent);
                    else
                        self.broadcastEvent(Messages.translate("ban-wave.added-to-wave-announcement", vars), silent);
                    break;
                default:
                    break;
            }
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            if (getPunisher().isOnline())
                getPunisher().sendMessage(Messages.serverError);
        }
    }
    

    /**
     * Appeal a punishment
     * 
     * @param type   The punishment type to remove
     * @param reason The reason for removal
     * @param silent Is the punishment removal silent
     */
    public Punishment appeal(User unpunisher, String reason, boolean silent) {
        setAppealReason(reason);
        setAppealed(true);
        setAppealedAt(TimeUtil.now());
        setAppealedBy(unpunisher);

        update();
  
        // TODO: Discord util
        // try {
        //     DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        // } catch (InvalidConfigurationException e) {
        //     e.printStackTrace();
        // }

        return this;
    }

    /**
     * Manually expire a punishment
     */
    public Punishment expire() {
        setAppealReason("Expired");
        setAppealed(true);
        setAppealedAt(TimeUtil.now());
        setAppealedBy(User.getConsoleUser());

        update();
  
        // TODO: Discord util
        // try {
        //     DiscordUtil.GetDiscord().SendDiscord(punish, silent);
        // } catch (InvalidConfigurationException e) {
        //     e.printStackTrace();
        // }

        return this;
    }
    
    public String getKey() {
        return punishID;
    }
}
