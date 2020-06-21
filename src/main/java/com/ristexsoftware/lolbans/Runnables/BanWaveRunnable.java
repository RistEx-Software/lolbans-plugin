package com.ristexsoftware.lolbans.Runnables;

import java.sql.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.ArrayList;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.BroadcastUtil;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PunishmentType;
import com.ristexsoftware.lolbans.Utils.Timing;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.User;


class BannedUser
{
    public BannedUser(OfflinePlayer op, String ExecutionerName, String ExecutionerUUID, String PunishID, String BanReason, Timestamp Expiry)
    {
        this.BannedPlayer = op;
        this.ExecutionerName = ExecutionerName;
        this.ExecutionerUUID = ExecutionerUUID;
        this.PunishID = PunishID;
        this.BanReason = BanReason;
        this.Expiry = Expiry;
    }

    public OfflinePlayer BannedPlayer;
    public String ExecutionerName;
    public String ExecutionerUUID;
    public String PunishID;
    public String BanReason;
    public Timestamp Expiry;
}

public class BanWaveRunnable extends BukkitRunnable
{
    public CommandSender sender;
    public boolean silent;

    public void run()
    {
        Main self = Main.getPlugin(Main.class);
        try
        {
            Timing t = new Timing();
            // First we query for all the people in the BanWave database.
            PreparedStatement PlayersToBanQuery = self.connection.prepareStatement("SELECT * FROM lolbans_banwave");
            // Array of users to be banned so we can send it to Discord.
            PreparedStatement PlayersToBanQueryArr = self.connection.prepareStatement("SELECT GROUP_CONCAT(PlayerName) AS PlayerNames FROM lolbans_banwave");
            PreparedStatement BanBatchQuery = self.connection.prepareStatement("INSERT INTO lolbans_punishments (UUID, PlayerName, IPAddress, Reason, PunishID, Type, ArbiterName, ArbiterUUID, Expiry) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            List<BannedUser> BannedPlayers = new ArrayList<BannedUser>();
            ResultSet ptbqr = PlayersToBanQueryArr.executeQuery();
            
            // Make sure there are actually people in the database!!!!
            if (!ptbqr.next())
            {
                User.PlayerOnlyVariableMessage("BanWave.EmptyWave", sender, "", false);
                return;
            }
        
            // just announcing the banned players
            String nameslist = String.join(", ", ptbqr.getString("PlayerNames").split(","));
            // Java is stupid, comand sender doesn't exist.
            DiscordUtil.GetDiscord().SendBanWave(sender.getName(), nameslist);
            // Iterate the banned users, move them to be banned tables
            ResultSet PlayersToBan = PlayersToBanQuery.executeQuery();
            while (PlayersToBan.next())
            {
                System.out.println(PlayersToBan.getString("PlayerName"));
                int i = 1;
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(PlayersToBan.getString("UUID")));
                BannedUser bp = new BannedUser(op, PlayersToBan.getString("ArbiterName"), PlayersToBan.getString("ArbiterUUID"), PlayersToBan.getString("PunishID"), PlayersToBan.getString("Reason"), PlayersToBan.getTimestamp("Expiry"));
                BannedPlayers.add(bp);
                BanBatchQuery.setString(i++, PlayersToBan.getString("UUID"));
                BanBatchQuery.setString(i++, PlayersToBan.getString("PlayerName"));
                BanBatchQuery.setString(i++, PlayersToBan.getString("IPAddress"));
                BanBatchQuery.setString(i++, bp.BanReason);
                BanBatchQuery.setString(i++, bp.PunishID);
                BanBatchQuery.setInt(i++, PunishmentType.PUNISH_BAN.ordinal());
                BanBatchQuery.setString(i++, bp.ExecutionerName);
                BanBatchQuery.setString(i++, bp.ExecutionerUUID);
                BanBatchQuery.setTimestamp(i++, bp.Expiry);
                BanBatchQuery.addBatch();
            }

            // Execute queries
            BanBatchQuery.executeUpdate();
            self.connection.prepareStatement("DELETE FROM BanWave").executeUpdate();

            // Syncronize with the main thread and kick players.
            Bukkit.getScheduler().runTaskLater(self, () -> {
                for (BannedUser bu : BannedPlayers)
                {
                    if (bu.BannedPlayer.isOnline())
                    {
                        User.KickPlayerBan(bu.ExecutionerName, (Player)bu.BannedPlayer, bu.PunishID, bu.BanReason, TimeUtil.TimestampNow(), bu.Expiry);
                        try
                        {
                            BroadcastUtil.BroadcastEvent(silent, Messages.Translate("Ban.BanAnnouncement",
                                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                                    {{
                                        put("ARBITER", bu.ExecutionerName);
                                        put("PLAYER", bu.BannedPlayer.getName());
                                        put("REASON", bu.BanReason);
                                        put("PUNISHID", bu.PunishID);
                                        put("SILENT", Boolean.toString(silent));
                                    }}
                            ));
                        }
                        catch (InvalidConfigurationException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                }
            }, 1L);

            // TODO: Broadcast to discord and ops on server that
            // the ban wave has completed.
            sender.sendMessage(Messages.Translate("BanWave.BanWaveFinished",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                {{
                    put("playercount", String.valueOf(BannedPlayers.size()));
                    put("time", String.valueOf(t.Finish()));
                }}
            ));
        }
        catch(SQLException | InvalidConfigurationException e)
        {
            e.printStackTrace();
        }
    }
}