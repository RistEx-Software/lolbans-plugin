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

package com.ristexsoftware.lolbans.api.runnables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.common.utils.Debug;

public class BanwaveRunnable implements Runnable  {
     @Override
    public void run() {
        try {
            Debug debug = new Debug(BanwaveRunnable.class);

            debug.print("Fetching outstanding banwave punishments...");
            PreparedStatement banwaveQuery = Database.getConnection().prepareStatement("SELECT " + 
                                                                                        "target_uuid,"+ 
                                                                                        "target_name,"+ 
                                                                                        "target_ip_address," + 
                                                                                        "reason," +  
                                                                                        "punished_by_name," +
                                                                                        "expires_at," + 
                                                                                        "silent," +
                                                                                        "punish_id" +
                                                                                        " FROM lolbans_punishments WHERE type = 6 AND appealed = FALSE");
            
            ResultSet res = banwaveQuery.executeQuery();

            while (res.next()) {
                User user = LolBans.getPlugin().getUser(UUID.fromString(res.getString("target_uuid")));
                if (user != null && user.isOnline()) {
                    TreeMap<String, String> vars = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                        {
                            put("player", res.getString("target_name"));
                            put("ipaddress", res.getString("target_ip_address"));
                            put("reason", res.getString("reason"));
                            put("arbiter", res.getString("punished_by_name"));
                            put("expiry", res.getTimestamp("expires_at") == null ? null : res.getTimestamp("expires_at").toString());
                            put("silent", res.getString("silent"));
                            put("appealed", Boolean.toString(false));
                            put("punishid", res.getString("punish_id"));
                        }
                    };
                    user.disconnect(LolBans.getPlugin().getLocaleProvider().translate("ban.ban-announcement", vars));
                }             
            }

            debug.print("Disconnected the relevant players, if there were any");
            debug.print("Updating punishment cache...");

            for (Punishment punishment : LolBans.getPlugin().getPunishmentCache().getAll()) {
                if (punishment.getType() == PunishmentType.BANWAVE && !punishment.getAppealed()) {
                    punishment.setType(PunishmentType.BAN);
                }
            }   

            debug.print("Changing punishment type to BAN...");
            PreparedStatement banwaveUpdateQuery = Database.getConnection().prepareStatement(
                    "UPDATE lolbans_punishments SET type = 0 WHERE type = 6 AND appealed = FALSE");
            int updated = banwaveUpdateQuery.executeUpdate();
            
            debug.print("Banwave execution complete - updated " + String.valueOf(updated) + " punishments");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}