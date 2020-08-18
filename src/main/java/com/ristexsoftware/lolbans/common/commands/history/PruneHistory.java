package com.ristexsoftware.lolbans.common.commands.history;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.utils.PunishID;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;

public class PruneHistory extends AsyncCommand {

    public PruneHistory(LolBans plugin) {
        super("prunehistory", plugin);
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(Messages.invalidSyntax);
        try {
            sender.sendMessage(
                    Messages.translate("syntax.purgehistory", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.serverError);
        }
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        return null;
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) throws Exception {
        
        if (!sender.hasPermission("lolbans.history.prune"))
            return sender.permissionDenied("lolbans.history.prune");

        Arguments a = new Arguments(args);
        a.requiredString("searchable");
        a.optionalTimestamp("duration");

        if (!a.valid()) {
            return false;
        }

        String searchable = a.get("searchable");
        // Boolean potentiallyReallyFuckingLargeDataErasure = !a.exists("duration");

        // // TODO Handle this
        // if (potentiallyReallyFuckingLargeDataErasure) {
        //     return false;
        // }

        // Check for ID
        if (PunishID.validateID(searchable)) {
            Punishment punishment = Punishment.findPunishment(searchable);
            if (punishment != null) {
                punishment.delete();
                sender.sendMessage("Deleted punishment " + punishment.getPunishID() + ".");
                return true;
            }
        }

        // Try resolve user
        User target = User.resolveUser(searchable);
        if (target == null) {
            return false;
        }

        PreparedStatement punishmentDeleteQuery = Database.getConnection()
            .prepareStatement("DELETE FROM lolbans_punishments WHERE target_uuid LIKE ? AND time_punished <= ?");
        punishmentDeleteQuery.setString(1, target.getUniqueId().toString());
        punishmentDeleteQuery.setTimestamp(2, a.getTimestamp("duration") == null ? TimeUtil.now() : a.getTimestamp("duration"));

        int res = punishmentDeleteQuery.executeUpdate();
        sender.sendMessage("Cleared " + res + " punishments.");

        return true;
    }
    
}