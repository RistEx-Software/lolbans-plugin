package com.ristexsoftware.lolbans.Runnables;

import java.sql.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.DiscordUtil;
import com.ristexsoftware.lolbans.Utils.User;

// Get reflection
import java.lang.reflect.Field; 

//     public static void KickPlayer(String sender, Player target, String BanID, String reason, Timestamp BanTime)
class BannedUser
{
    public BannedUser(OfflinePlayer op, String Executioner, String BanID, String BanReason)
    {
        this.BannedPlayer = op;
        this.Executioner = Executioner;
        this.BanID = BanID;
        this.BanReason = BanReason;
    }

    public OfflinePlayer BannedPlayer;
    public String Executioner;
    public String BanID;
    public String BanReason;
}

public class BanWaveRunnable extends BukkitRunnable
{
    public CommandSender sender;

    public void run()
    {
        Main self = Main.getPlugin(Main.class);
        try
        {

            //DiscordUtil.SendFormatted("%s started a ban wave.", sender.getName());
            // First we query for all the people in the BanWave database.
            PreparedStatement PlayersToBanQuery = self.connection.prepareStatement("SELECT * FROM BanWave");
            // Array of users to be banned.
            PreparedStatement PlayersToBanQueryArr = self.connection.prepareStatement("SELECT PlayerName, GROUP_CONCAT(PlayerName) AS PlayerNames FROM BanWave");
            PreparedStatement BanBatchQuery = self.connection.prepareStatement("INSERT INTO BannedPlayers (UUID, PlayerName, IPAddress, Reason, Executioner, BanID) VALUES (?, ?, ?, ?, ?, ?)");
            PreparedStatement BanHistoryQuery = self.connection.prepareStatement("INSERT INTO BannedHistory (UUID, PlayerName, IPAddress, Reason, Executioner, BanID) VALUES (?, ?, ?, ?, ?, ?)");
            
            List<BannedUser> BannedPlayers = new ArrayList<BannedUser>();
            ResultSet ptbqr = PlayersToBanQueryArr.executeQuery();
            
            // Make sure there are actually people in the database!!!!
            if (ptbqr.next())
            {
                // just announcing the banned players
                String ntbq = ptbqr.getString("PlayerNames");
                String ptbqa[] = ntbq.split(",");
                String ptbqa1 = String.join(", ", ptbqa);
                // Java is stupid, comand sender doesn't exist.
                DiscordUtil.SendBanWave("A ban wave was executed.", ptbqa1);
                // Iterate the banned users, move them to be banned tables
                ResultSet PlayersToBan = PlayersToBanQuery.executeQuery();
                while (PlayersToBan.next())
                {
                    int i = 1;
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(PlayersToBan.getString("UUID")));
                    BannedUser bp = new BannedUser(op, PlayersToBan.getString("Executioner"), PlayersToBan.getString("BanID"), PlayersToBan.getString("Reason"));
                    BannedPlayers.add(bp);

                    BanBatchQuery.setString(i++, PlayersToBan.getString("UUID"));
                    BanBatchQuery.setString(i++, PlayersToBan.getString("PlayerName"));
                    BanBatchQuery.setString(i++, PlayersToBan.getString("IPAddress"));
                    BanBatchQuery.setString(i++, bp.BanReason);
                    BanBatchQuery.setString(i++, bp.Executioner);
                    BanBatchQuery.setString(i++, bp.BanID);
                    BanBatchQuery.addBatch();

                    i = 1;

                    BanHistoryQuery.setString(i++, PlayersToBan.getString("UUID"));
                    BanHistoryQuery.setString(i++, PlayersToBan.getString("PlayerName"));
                    BanHistoryQuery.setString(i++, PlayersToBan.getString("IPAddress"));
                    BanHistoryQuery.setString(i++, PlayersToBan.getString("Reason"));
                    BanHistoryQuery.setString(i++, PlayersToBan.getString("Executioner"));
                    BanHistoryQuery.setString(i++, PlayersToBan.getString("BanID"));
                    BanHistoryQuery.addBatch();
                }

                // Execute queries
                BanBatchQuery.executeUpdate();
                BanHistoryQuery.executeUpdate();
                self.connection.prepareStatement("DELETE FROM BanWave").executeUpdate();

                // Syncronize with the main thread and kick players.
                Bukkit.getScheduler().runTaskLater(self, () -> {
                    for (BannedUser bu : BannedPlayers)
                    {
                        if (bu.BannedPlayer.isOnline())
                            User.KickPlayer(bu.Executioner, (Player)bu.BannedPlayer, bu.BanID, bu.BanReason, null);

                        self.getLogger().info(String.format("%s was banned: %s (#%s)", bu.BannedPlayer.getName(), bu.BanReason, bu.BanID));
                    }
                }, 1L);

                sender.sendMessage(String.format(ChatColor.RED + "Banned" + ChatColor.GRAY + " %d " + ChatColor.RED + "player%s.", BannedPlayers.size(), BannedPlayers.size() != 1 ? "s" : ""));
                //DiscordUtil.SendFormatted("Banned %d player%s.", BannedPlayers.size(), BannedPlayers.size() != 1 ? "s" : "");
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Error! No users in the ban wave!");
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }
}