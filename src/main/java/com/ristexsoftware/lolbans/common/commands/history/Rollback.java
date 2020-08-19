package com.ristexsoftware.lolbans.common.commands.history;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.Database;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.common.utils.Debug;

public class Rollback extends AsyncCommand {
    public Rollback(LolBans plugin) {
        super("rollback", plugin);
        setDescription("Roll back the punishment history by the given duration.");
        setPermission("lolbans.rollback");
        setSyntax(Messages.getMessages().getConfig().getString("syntax.staff-rollback"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(Messages.invalidSyntax);
        try {
            sender.sendMessage(
                Messages.translate("syntax.staff-rollback", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        if (!sender.hasPermission("lolbans.rollback"))
            return sender.permissionDenied("lolbans.rollback");

        Arguments a = new Arguments(args);
        a.optionalFlag("silent", "-s");
        a.requiredString("name");
        a.requiredDuration("duration");
        
        if (!a.valid()) 
            return false;

        boolean silent = a.getFlag("silent");
        User target = User.resolveUser(a.get("name"));
        if (target == null) 
            return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("name"), true);

        Long duration = a.getDuration("duration");
        Timestamp pivot = Timestamp.from(TimeUtil.now().toInstant().minus(duration, ChronoUnit.SECONDS));
        
        PreparedStatement countQuery = Database.getConnection()
                .prepareStatement(
                        "SELECT COUNT(*) AS count FROM lolbans_punishments WHERE punished_by_uuid = ? AND time_punished >= ?");
        countQuery.setString(1, target.getUniqueId().toString());
        countQuery.setTimestamp(2, pivot);

        ResultSet countResult = countQuery.executeQuery();
        countResult.next();

        int count = countResult.getInt("count");
        if (count == 0) 
            return sender.sendReferencedLocalizedMessage("staff-rollback.no-rollback", null, true);

        PreparedStatement timeTravelStatement = Database.getConnection()
                .prepareStatement("DELETE FROM lolbans_punishments WHERE time_punished >= ? AND punished_by_uuid = ?");
        timeTravelStatement.setTimestamp(1, pivot);
        timeTravelStatement.setString(2, target.getUniqueId().toString());
        
        int affected = timeTravelStatement.executeUpdate();
        getPlugin().broadcastEvent(Messages.translate("staff-rollback.announcement", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
            put("player", sender.getName());
            put("affected", String.valueOf(affected));
        }}), silent);
        return sender.sendReferencedLocalizedMessage("staff-rollback.rollback-complete", String.valueOf(affected), true);
    }
}