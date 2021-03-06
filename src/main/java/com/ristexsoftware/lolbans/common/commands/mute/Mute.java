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

package com.ristexsoftware.lolbans.common.commands.mute;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.common.utils.Timing;

public class Mute {

    public static class MuteCommand extends AsyncCommand {

        public MuteCommand(LolBans plugin) {
            super("mute", plugin);
            setDescription("Mute a player");
            setPermission("lolbans.mute");
            setAliases(Arrays.asList(new String[] { "emute", "tempmute" }));
            setSyntax(getPlugin().getLocaleProvider().get("syntax.mute"));
        }

        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
            sender.sendMessage(
                    LolBans.getPlugin().getLocaleProvider().translate("syntax.mute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }

        @Override
        public List<String> onTabComplete(User sender, String[] args) {
            if (args.length < 2) {
                ArrayList<String> usernames = new ArrayList<>();

                if (!args[0].equals("")) {
                    for (User user : LolBans.getPlugin().getUserCache().getAll()) {
                        if (user.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                            usernames.add(user.getName());
                        }
                    }
                } else {
                    // Instead of creating a stupid for loop here, let's just stream
                    usernames = (ArrayList<String>) LolBans.getPlugin().getUserCache().getAll().stream()
                            .map(user -> user.getName()).collect(Collectors.toList());
                }

                return usernames;
            }

            if (args.length < 3) {
                return ImmutableList.of("1m", "15m", "1h", "3h", "12h", "1d", "1w", "1mo", "1y");
            }

            return Arrays.asList();
        }

        @Override
        public boolean run(User sender, String commandLabel, String[] args) {
            if (!sender.hasPermission("lolbans.mute"))
                return sender.permissionDenied("lolbans.mute");

            // Debug debug = new Debug();
            Timing time = new Timing();
            try {
                Arguments a = new Arguments(args);
                a.optionalFlag("silent", "-s");
                a.optionalFlag("overwrite", "-o");
                a.requiredString("username");
                a.optionalTimestamp("expiry");
                a.optionalSentence("reason");

                if (!a.valid())
                    return false;

                boolean silent = a.getFlag("silent");
                boolean overwrite = a.getFlag("overwrite");
                String username = a.get("username");
                Timestamp expiry = a.getBoolean("expiry") ? null : a.getTimestamp("expiry");

                User target = User.resolveUser(username);

                if (target == null)
                    return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("username"), true);

                if (overwrite && !sender.hasPermission("lolbans.mute.overwrite"))
                    return sender.permissionDenied("lolbans.mute.overwrite");

                if (target.isPunished(PunishmentType.MUTE) && !overwrite)
                    return sender.sendReferencedLocalizedMessage("mute.player-is-muted", target.getName(), false);

                if (expiry == null && !sender.hasPermission("lolbans.mute.perm"))
                    return sender.permissionDenied("lolbans.mute.perm");

                if (expiry != null && expiry.getTime() > sender.getTimeGroup().getTime())
                    expiry = sender.getTimeGroup();

                String reason = a.get("reason");
                if (reason == null || reason.trim().equals("null")) {
                    reason = getPlugin().getLocaleProvider().get("mute.default-reason", "You have been muted!");
                    // reason = configReason == null ? "Your account has been suspended!" : configReason;
                }

                Punishment punishment = new Punishment(PunishmentType.MUTE, sender, target, reason, expiry, silent,
                        false);

                if (overwrite && target.isPunished(PunishmentType.MUTE)) {
                    target.removeLatestPunishmentOfType(PunishmentType.MUTE, sender,
                            "Overwritten by #" + punishment.getPunishID(), silent);
                }
                if (target.isOnline()) {
                    if (target.hasPermission("lolbans.mute.immune"))
                        return sender.sendReferencedLocalizedMessage("cannot-punish-operator", target.getName(), true);
                    target.sendMessage(punishment);
                }
                // target.disconnect(punishment);

                punishment.commit(sender);
                punishment.broadcast();
                time.finish(sender);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
            }
            return true;
        }
    }

}