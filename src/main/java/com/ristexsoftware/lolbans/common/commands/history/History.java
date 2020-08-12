package com.ristexsoftware.lolbans.common.commands.history;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.google.common.collect.ImmutableList;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.User;

public class History extends AsyncCommand {

    public History(LolBans plugin) {
        super("history", plugin);
        
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {

    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        if (args.length < 2) {
            ArrayList<String> usernames = new ArrayList<>();
            for (User user : LolBans.getPlugin().getUserCache().getAll()) {
                usernames.add(user.getName());
            }
            return usernames;
        }

        return Arrays.asList();
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        try {
            Arguments a = new Arguments(args);
            a.optionalString("targetOrPage");
            a.optionalInt("pageGivenPlayer");
            
            if (!a.valid()) {
                return false;
            }

            String targetOrPage = a.get("targetOrPage");
            User target = User.resolveUser(targetOrPage);

            Integer page = a.getInt("pageGivenPlayer");
            if (target == null) {
                page = a.getInt("targetOrPage");
            }

            if (page == null) {
                page = 1;
            }

            // Convert to SQL-readable page - is one minus user-readable.
            page--;
            if (page < 0) {
                return false;
            }

            // Count rows to ensure page isn't out of range.
            PreparedStatement countQuery = Database.getConnection()
                    .prepareStatement(
                            "SELECT COUNT(*) AS count FROM lolbans_punishments WHERE target_name LIKE ? ORDER BY time_punished");
            countQuery.setString(1, target == null ? "%" : target.getName());

            ResultSet countResult = countQuery.executeQuery();
            countResult.next();
            int count = countResult.getInt("count");
            int pageSize = Messages.getMessages().getConfig().getInt("history.page-size");

            if (count < page * pageSize) {
                return false;
            }

            // Fetch fixed amount of punishments (always less than 50).
            PreparedStatement punishmentQuery = Database.getConnection()
                    .prepareStatement("SELECT * FROM lolbans_punishments WHERE target_name LIKE ? ORDER BY time_punished LIMIT ?, ?");
            punishmentQuery.setString(1, target == null ? "%" : target.getName());
            punishmentQuery.setInt(2, page * pageSize);
            punishmentQuery.setInt(3, pageSize);

            ResultSet res = punishmentQuery.executeQuery();
            if (!res.next() || res.wasNull())
                return sender.sendReferencedLocalizedMessage("history.no-history", args[0], true);

            // We use a do-while loop because we already checked if there was a result above.
            List<String> history = new ArrayList<String>();
            do 
            {
                // First, we have to calculate our punishment type.
                PunishmentType type = PunishmentType.fromOrdinal(res.getInt("type"));
                Timestamp ts = res.getTimestamp("expires_at");

                history.add(Messages.translate(ts == null ? "history.history-message-perm" : "history.history-message-temp", 
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
            sender.sendMessage(Messages.serverError);
        }        

        return true;
    }

}