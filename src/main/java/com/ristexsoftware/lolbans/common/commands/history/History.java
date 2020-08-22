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

package com.ristexsoftware.lolbans.common.commands.history;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.User;

public class History extends AsyncCommand {

    public History(LolBans plugin) {
        super("history", plugin);
        setPermission("lolbans.history");
        setDescription("View the punishment history of a specific player or everyone");

        setSyntax(getPlugin().getLocaleProvider().get("syntax.history"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
        sender.sendMessage(
                LolBans.getPlugin().getLocaleProvider().translate("syntax.history", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        if (args.length < 2) {
            ArrayList<String> usernames = new ArrayList<>();

            if(!args[0].equals("")) {
                for(User user : LolBans.getPlugin().getUserCache().getAll()) {
                    if(user.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        usernames.add(user.getName());
                    }
                }
            } else {
                // Instead of creating a stupid for loop here, let's just stream 
                usernames = (ArrayList<String>) LolBans.getPlugin().getUserCache().getAll().stream()
                .map(user -> user.getName())
                .collect(Collectors.toList());
            }

            return usernames;
        }

        return Arrays.asList();
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        try {
            if (!sender.hasPermission("lolbans.history"))
                return sender.permissionDenied("lolbans.history");
            Arguments a = new Arguments(args);
            a.optionalString("targetOrPage");
            a.optionalInt("pageGivenPlayer");
            
            if (!a.valid())
                return false;

            String targetOrPage = a.get("targetOrPage");
            User target = User.resolveUser(targetOrPage);

            Integer page = a.getInt("pageGivenPlayer");
            if (target == null)
                page = a.getInt("targetOrPage");
            

            if (page == null)
                page = 1;

            // Convert to SQL-readable page - is one minus user-readable.
            page--;
            if (page < 0)
                return false;

            // Count rows to ensure page isn't out of range.
            PreparedStatement countQuery = Database.getConnection()
                    .prepareStatement( 
                            "SELECT COUNT(*) AS count FROM lolbans_punishments WHERE target_name LIKE ? ORDER BY time_punished DESC");
            countQuery.setString(1, target == null ? "%" : target.getName());

            ResultSet countResult = countQuery.executeQuery();
            countResult.next();
            int count = countResult.getInt("count");
            int pageSize = Integer.valueOf(getPlugin().getLocaleProvider().get("history.page-size"));

            if (count < page * pageSize) {
                return false;
            }

            // Fetch fixed amount of punishments (always less than 50).
            PreparedStatement punishmentQuery = Database.getConnection()
                    .prepareStatement("SELECT * FROM lolbans_punishments WHERE target_name LIKE ? ORDER BY time_punished DESC LIMIT ?, ?");
            punishmentQuery.setString(1, target == null ? "%" : target.getName());
            punishmentQuery.setInt(2, page * pageSize);
            punishmentQuery.setInt(3, pageSize);

            ResultSet res = punishmentQuery.executeQuery();
            if (!res.next() || res.wasNull()) {
                return target == null ? sender.sendReferencedLocalizedMessage("history.no-history", "", true) : sender.sendReferencedLocalizedMessage("history.no-history-player", target.getName(), true);
            }

            // We use a do-while loop because we already checked if there was a result above.
            List<String> history = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                PunishmentType type = PunishmentType.fromOrdinal(res.getInt("type"));
                Timestamp ts = res.getTimestamp("expires_at");

                history.add(LolBans.getPlugin().getLocaleProvider().translate(ts == null ? "history.history-message-perm" : "history.history-message-temp", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                        put("playername", res.getString("target_name"));
                        put("punishid", res.getString("punish_id"));
                        put("reason", res.getString("reason"));
                        put("arbiter", res.getString("punished_by_name"));
                        // TODO: check for temporary/permanent bans
                        put("type", type.displayName());
                        put("date", res.getTimestamp("time_punished").toString());
                        put("expiry", ts == null ? "" : ts.toString());
                        // TODO: Add more variables for people who want more info?
                    }}
                ));
            }
            while(res.next());

            String message = String.join("\n", history);
            sender.sendMessage(message);

        } catch(Exception e) {
            e.printStackTrace();
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
        }        

        return true;
    }

}