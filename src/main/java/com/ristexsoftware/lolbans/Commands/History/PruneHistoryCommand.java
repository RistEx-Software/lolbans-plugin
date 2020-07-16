/* 
 *     LolBans - The advanced banning system for Minecraft
 *     Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *     Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *   
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *   
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *   
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *  
 */

package com.ristexsoftware.lolbans.Commands.History;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

// import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressString;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.TimeUtil;
import com.ristexsoftware.lolbans.Objects.Punishment;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ArgumentUtil;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.PunishID;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.Optional;
import java.util.TreeMap;
import java.sql.*;

// TODO: All the messages need to be run through Messages.yml
public class PruneHistoryCommand extends RistExCommandAsync
{
    private Main self = (Main) this.getPlugin();
    
    public PruneHistoryCommand(Plugin owner)
    {
        super("prunehistory", owner);
        this.setDescription("Delete the punishment history for a date range");
        this.setPermission("lolbans.history.prune");
    }

    private boolean HandlePunishmentID(CommandSender sender, String searchable, String[] args) throws SQLException, InterruptedException, ExecutionException
    {
        PreparedStatement RegexStatement = self.connection.prepareStatement("SELECT * FROM lolbans_regexbans WHERE PunishID = ?");
        PreparedStatement IPBanStatement = self.connection.prepareStatement("SELECT * FROM lolbans_ipbans WHERE PunishID = ?");
        PreparedStatement Reports = self.connection.prepareStatement("SELECT * FROM lolbans_reports WHERE PunishID = ?");
        
        Future<Optional<ResultSet>> regexres = DatabaseUtil.ExecuteLater(RegexStatement);
        Future<Optional<ResultSet>> ipbansres = DatabaseUtil.ExecuteLater(IPBanStatement);
        Future<Optional<ResultSet>> reportsres = DatabaseUtil.ExecuteLater(Reports);
        Optional<Punishment> punish = Punishment.FindPunishment(searchable);

        Integer recordnum = 0;
        // Delete a punishment
        if (punish.isPresent())
        {
            Punishment p = punish.get();
            p.Delete();
            recordnum = recordnum + 1;
            return true;
        }

        // Delete regex bans
        if (regexres.isDone())
        {
            Optional<ResultSet> ores = regexres.get();
            if (ores.isPresent())
            {
                ResultSet res = ores.get();
                PreparedStatement RegexDelete = self.connection.prepareStatement("DELETE FROM lolbans_regexbans WHERE id = ?");

                while (res.next())
                {
                    RegexDelete.setInt(1, res.getInt("id"));
                    RegexDelete.addBatch();
                }
                DatabaseUtil.ExecuteUpdate(RegexDelete);
                sender.sendMessage("History Cleared.");
            }
        }

        // Delete ip bans
		if (ipbansres.isDone())
		{
			Optional<ResultSet> ores = ipbansres.get();
			if (ores.isPresent())
            {
                ResultSet res = ores.get();
                PreparedStatement IPBanDelete = self.connection.prepareStatement("DELETE FROM lolbans_ipbans WHERE id = ?");

                while (res.next())
                {
                    IPBanDelete.setInt(1, res.getInt("id"));
                    IPBanDelete.addBatch();
                }
                DatabaseUtil.ExecuteUpdate(IPBanDelete);
                sender.sendMessage("History Cleared.");
            }
        }
        
        // Delete reports
        if (reportsres.isDone())
		{
			Optional<ResultSet> ores = reportsres.get();
			if (ores.isPresent())
            {
                ResultSet res = ores.get();
                PreparedStatement ReportDelete = self.connection.prepareStatement("DELETE FROM lolbans_reports WHERE id = ?");

                while (res.next())
                {
                    ReportDelete.setInt(1, res.getInt("id"));
                    ReportDelete.addBatch();
                }
                DatabaseUtil.ExecuteUpdate(ReportDelete);
                sender.sendMessage("History Cleared.");
            }
        }

        return true;
	}

    private boolean HandleIPBan(CommandSender sender, String searchable, Timestamp bantime, String[] args) throws SQLException
    {
        IPAddressString thingy = new IPAddressString(searchable);
        for (IPAddressString cb : Main.BannedAddresses) 
        {
            if (cb.contains(thingy)) 
            {
                PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM lolbans_ipbans WHERE IPAddress = ? AND TimeAdded >= ?");
                pst.setString(1, cb.toString());
                pst.setTimestamp(2, bantime);

                ResultSet res = pst.executeQuery();
                // Clear the history for this address range
                if (!res.next())
                {
                    sender.sendMessage("The IP " + cb.toString() + " is not banned.");
                    return true;
                }

                PreparedStatement ps = self.connection.prepareStatement("DELETE FROM lolbans_ipbans WHERE IPAddress = ? AND TimeAdded >= ?");
                ps.setString(1, cb.toString());
                ps.setTimestamp(2, bantime);
                
                DatabaseUtil.ExecuteUpdate(ps);
                sender.sendMessage("History Cleared.");
            }
        }

        return true;
    }

    private boolean HandlePlayer(CommandSender sender, OfflinePlayer player, Timestamp bantime, String[] args) throws SQLException, ExecutionException, InterruptedException
    {
		PreparedStatement ps = self.connection.prepareStatement("SELECT * FROM lolbans_punishments WHERE UUID = ? AND TimePunished >= ?");
        ps.setString(1, player.getUniqueId().toString());
        ps.setTimestamp(2, bantime);

		Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(ps).get();
		if (ores.isPresent())
		{
			ResultSet res = ores.get();
			if (res.next())
			{
				ps = self.connection.prepareStatement("DELETE FROM lolbans_punishments WHERE UUID = ? AND TimePunished >= ?");
                ps.setString(1, player.getUniqueId().toString());
				ps.setTimestamp(2, bantime);
				DatabaseUtil.ExecuteUpdate(ps);
				sender.sendMessage("History Cleared.");
			}
			else
				sender.sendMessage("Player " + player.getName() + " is not punished.");
        }
        return true;
	}

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args)
    {
        try 
        {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(Messages.Translate("Syntax.PruneHistory", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }
        catch (InvalidConfigurationException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args)
    {
        if (!PermissionUtil.Check(sender, "lolbans.history.prune"))
            return User.PermissionDenied(sender, "lolbans.history.prune");
            
        try 
        {
            // /prunehistory <Player|PunishID|CIDR|Regex> <Distance In Time|*>
            ArgumentUtil a = new ArgumentUtil(args);
            a.RequiredString("Searchable", 0);
            a.RequiredString("Distance", 1);

            if (!a.IsValid())
                return false;

            String searchable = a.get("Searchable");
            String TimePeriod = a.get("Distance");
            @SuppressWarnings("deprecation") OfflinePlayer player = Bukkit.getOfflinePlayer(searchable);
            Timestamp bantime = null;

            // Parse ban time.
            // NOTE: We explicitly do this here because it is going back in time instead of forward
            // which means we cannot use TimeUtil for this. Maybe I will refactor it sometime in the
            // future but for now this is fine.
            if (!Messages.CompareMany(TimePeriod, new String[]{"*", "0"}))
            {
                Optional<Long> dur = TimeUtil.Duration(TimePeriod);
                if (dur.isPresent())
                    bantime = new Timestamp(System.currentTimeMillis() - (dur.get() * 1000L));
                else
                    return false;
            }

            // This only prunes the specific punishment
            if (PunishID.ValidateID(searchable))
                return this.HandlePunishmentID(sender, searchable, args);
            else if (searchable.contains(".") || searchable.contains(":"))
                return this.HandleIPBan(sender, searchable, bantime, args);
            else if (player != null)
                return this.HandlePlayer(sender, player, bantime, args);
            else
            {
                Pattern regex = Pattern.compile(searchable);
                PreparedStatement pst = self.connection.prepareStatement("SELECT * FROM lolbans_regexbans WHERE Regex = ? AND TimeAdded >= ?");
                pst.setString(1, regex.toString());
                pst.setTimestamp(2, bantime);

                Optional<ResultSet> ores = DatabaseUtil.ExecuteLater(pst).get();
                if (ores.isPresent())
                {
                    ResultSet res = ores.get();
                    if (res.next())
                    {
                        pst = self.connection.prepareStatement("DELETE FROM lolbans_regexbans WHERE id = ?");
                        DatabaseUtil.ExecuteUpdate(pst);
                        sender.sendMessage("History Cleared.");
                    }
                    else
                        sender.sendMessage("Regex " + searchable + " is not banned.");
                }
            }
        }
        catch (InterruptedException | ExecutionException | SQLException e)
        {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
        return true;
    }
}
