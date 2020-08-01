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

 // TODO: this class
// package com.ristexsoftware.lolbans.api.punishment.runnables;

// import inet.ipaddr.IPAddressString;
// import java.sql.*;
// import com.ristexsoftware.lolbans.api.LolBans;
// import com.ristexsoftware.lolbans.api.Database;

// import org.bukkit.scheduler.BukkitRunnable;


// public class Query extends BukkitRunnable
// {
//     public void run()
//     {
//         // Main self = Main.getPlugin(Main.class);
//         try
//         {
//             // self.connection.prepareStatement("DELETE FROM LinkConfirmations WHERE Expiry <= NOW()").executeUpdate();
//             // TODO: Report expirations should be configurable
//             PreparedStatement punps = Database.connection.prepareStatement("UPDATE lolbans_punishments SET appealed = True, appeal_reason = 'Expired', unpunisher_name = 'CONSOLE', unpunisher_uuid = 'CONSOLE', appeal_time = NOW() WHERE expires_at <= NOW()");
    
//             Database.ExecuteUpdate(punps);
//             Database.ExecuteUpdate(regps);
//             Database.ExecuteUpdate(ipps);
//             // PreparedStatement ps = Database.connection.prepareStatement("UPDATE lolbans_reports SET Closed = True, CloseReason = 'Expired' WHERE TimeAdded <= ?");
//             // ps.setTimestamp(1, new Timestamp((TimeUtil.GetUnixTime() * 1000L) + TimeUtil.Duration(self.getConfig().getString("General.ReportExpiry", "3d")).get()));
//             // Database.ExecuteUpdate(ps);

//             /*******************************************************************************
//              * Ensure our IP ban list is up to date.
//              */

//             // Grab all the latest IP bans from the databse and ensure everything is up to date.
//             ResultSet rs = Database.connection.prepareStatement("SELECT * FROM lolbans_ipbans WHERE appealed = false").executeQuery();
//             while (rs.next())
//             {
//                 IPAddressString addr = new IPAddressString(rs.getString("target_ip_address"));

//                 // Try and find our address.
//                 boolean found = false;
//                 for (IPAddressString cb : LolBans.BANNED_ADDRESSES)
//                 {
//                     if (cb.compareTo(addr) == 0)
//                     { 
//                         found = true;
//                         break;
//                     }
//                 }

//                 // Add our banned cidr range if not found.
//                 if (!found)
//                 LolBans.BANNED_ADDRESSES.add(addr);
//             }
//         }
//         catch(SQLException e)
//         {
//             e.printStackTrace();
//         }
//     }
// }