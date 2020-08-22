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
import java.util.Arrays;
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

public class PruneHistory extends AsyncCommand {

    public PruneHistory(LolBans plugin) {
        super("prunehistory", plugin);
        setDescription("Delete the history of a specific player or everyone");
        setPermission("lolbans.history.prune");
        setAliases(Arrays.asList(new String[] { "clearhistory", "purgehistory" }));
        setSyntax(getPlugin().getLocaleProvider().get("syntax.prune-history"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
        sender.sendMessage(
                LolBans.getPlugin().getLocaleProvider().translate("syntax.prune-history", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        a.optionalString("searchable");
        a.optionalTimestamp("duration");

        if (!a.valid()) 
            return false;

        if (a.getBoolean("searchable") == null && !sender.hasPermission("lolbans.history.prune.all"))
            return sender.permissionDenied("lolbans.history.prune.all");

        String searchable = a.get("searchable") == null ? "%" : a.get("searchable");

        // Check for ID
        if (PunishID.validateID(searchable)) {
            Punishment punishment = Punishment.findPunishment(searchable);
            if (punishment != null) {
                punishment.delete();
                sender.sendMessage(LolBans.getPlugin().getLocaleProvider().translate("prune-history.deleted-single-punishment", punishment.getVariableMap()));
                return true;
            }
        }

        String message = null;
        // Try resolve user
        User target = User.resolveUser(searchable);
        if (target == null)
            searchable = "%";

        else
            searchable = target.getUniqueId().toString();

        PreparedStatement punishmentDeleteQuery = Database.getConnection()
            .prepareStatement("DELETE FROM lolbans_punishments WHERE target_uuid LIKE ? AND time_punished <= ?");
        punishmentDeleteQuery.setString(1, searchable);
        punishmentDeleteQuery.setTimestamp(2, a.getTimestamp("duration") == null ? TimeUtil.now() : a.getTimestamp("duration"));

        int res = punishmentDeleteQuery.executeUpdate();
        final String MuStBeFiNaL = searchable;
        sender.sendMessage(LolBans.getPlugin().getLocaleProvider().translate(target == null ? "prune-history.cleared-history-all" : "prune-history.cleared-history-player"
        , new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
            put("count", String.valueOf(res));
            put("player", target == null ? MuStBeFiNaL : target.getName());
        }}));

        return true;
    }
}