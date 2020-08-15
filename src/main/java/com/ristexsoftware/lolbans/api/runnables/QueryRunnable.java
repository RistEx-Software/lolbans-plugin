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

// TODO: this class
package com.ristexsoftware.lolbans.api.runnables;

import inet.ipaddr.IPAddressString;
import java.sql.*;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.Database;

import org.bukkit.scheduler.BukkitRunnable;

public class QueryRunnable extends TimerTask {
    @Override
    public void run() {
        FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                // Main self = Main.getPlugin(Main.class);
                try {
                    for (Punishment punish : LolBans.getPlugin().getPunishmentCache().getAll()) {
                        if (punish.getExpiresAt().getTime() <= System.currentTimeMillis()) {
                            punish.expire();
                        }
                    }

                    // self.connection.prepareStatement("DELETE FROM LinkConfirmations WHERE Expiry
                    // <= NOW()").executeUpdate();
                    // TODO: Report expirations should be configurable
                    PreparedStatement punps = Database.getConnection().prepareStatement("UPDATE lolbans_punishments SET appealed = True, appeal_reason = 'Expired', appealed_by_name = 'CONSOLE', appealed_by_uuid = 'CONSOLE', appealed_at = NOW() WHERE expires_at <= NOW()");
                    Database.executeUpdate(punps);
                    // PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE
                    // lolbans_reports SET Closed = True, CloseReason = 'Expired' WHERE TimeAdded <=
                    // ?");
                    // ps.setTimestamp(1, new Timestamp((TimeUtil.GetUnixTime() * 1000L) +
                    // TimeUtil.Duration(self.getConfig().getString("General.ReportExpiry",
                    // "3d")).get()));
                    // Database.ExecuteUpdate(ps);

                    /*******************************************************************************
                     * Ensure our IP ban list is up to date.
                     */

                    // Grab all the latest IP bans from the databse and ensure everything is up to
                    // date.
                    ResultSet rs = Database.getConnection().prepareStatement(
                            "SELECT * FROM lolbans_punishments WHERE appealed = FALSE AND type = 5 AND ip_ban = TRUE")
                            .executeQuery();
                    while (rs.next()) {
                        IPAddressString addr = new IPAddressString(rs.getString("target_ip_address"));

                        // Try and find our address.
                        boolean found = false;
                        for (IPAddressString cb : LolBans.getPlugin().BANNED_ADDRESSES) {
                            if (cb.compareTo(addr) == 0) {
                                found = true;
                                break;
                            }
                        }

                        // Add our banned cidr range if not found.
                        if (!found)
                            LolBans.getPlugin().BANNED_ADDRESSES.add(addr);
                    
                
                    }   
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        if (!LolBans.getPlugin().getPool().isShutdown())
            LolBans.getPlugin().getPool().execute(t);
    }
}
