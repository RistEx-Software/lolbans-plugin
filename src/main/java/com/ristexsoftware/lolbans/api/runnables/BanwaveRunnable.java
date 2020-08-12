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
import com.ristexsoftware.lolbans.api.configuration.Messages;
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
                User user = LolBans.getPlugin().getOnlineUsers().get(UUID.fromString(res.getString("target_uuid")));
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
                    user.disconnect(Messages.translate("ban.ban-announcement", vars));
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
        } catch (SQLException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}