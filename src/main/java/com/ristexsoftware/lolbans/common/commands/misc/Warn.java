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

package com.ristexsoftware.lolbans.common.commands.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.common.utils.Debug;

public class Warn {

    public static class WarnCommand extends AsyncCommand {

        public WarnCommand(LolBans plugin) {
			super("warn", plugin);
			setDescription("Warn a player");
			setPermission("lolbans.warn");
		}

        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
            sender.sendMessage(Messages.invalidSyntax);
            try {
                sender.sendMessage(
                        Messages.translate("syntax.warn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
            Debug debug = new Debug(getClass());

            if (!sender.hasPermission("lolbans.warn"))
                return sender.permissionDenied("lolbans.warn");
            
            Arguments a = new Arguments(args);
            a.optionalFlag("silent", "-s");
            a.requiredString("username");
            a.optionalSentence("reason"); 

            if (!a.valid())
                return false;

            User target = User.resolveUser(a.get("username"));
            if (target == null)
                return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("username"), true);

            Punishment punishment = new Punishment(PunishmentType.WARN, sender, target, a.get("reason"), a.getTimestamp("expiry"), a.getFlag("silent"), false);
            
            debug.print("Created new warning - acknowledged=" + String.valueOf(punishment.getWarningAck()));

            punishment.commit(sender);
            punishment.broadcast();

            return true;
        }
    }

    public static class UnwarnCommand extends AsyncCommand {
        public UnwarnCommand(LolBans plugin) {
			super("unwarn", plugin);
			setDescription("Removes a player's previous warn");
			setPermission("lolbans.unwarn");
		}

        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
            sender.sendMessage(Messages.invalidSyntax);
            try {
                sender.sendMessage(
                        Messages.translate("syntax.unwarn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
                sender.sendMessage(Messages.serverError);
            }
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
            if (!sender.hasPermission("lolbans.unwarn"))
                return sender.permissionDenied("lolbans.unwarn");
            
            Arguments a = new Arguments(args);
            a.requiredString("username");
            a.optionalFlag("silent", "-s");
            a.optionalSentence("reason"); 

            if (!a.valid())
                return false;

            User target = User.resolveUser(a.get("username"));
            if (target == null)
                return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("username"), true);
            
            if (!target.isPunished(PunishmentType.WARN))
				return sender.sendReferencedLocalizedMessage("warn.player-not-warned", target.getName(), false);

            Punishment punishment = target.removeLatestPunishmentOfType(PunishmentType.WARN, sender, a.get("reason"),
                    a.exists("silent"));
            
            punishment.broadcast();
            target.sendMessage(punishment);

            return sender.sendReferencedLocalizedMessage("warn.removed-success", target.getName(), true);
        }
    }

    public static class AcknowledgeWarnCommand extends AsyncCommand {
        public AcknowledgeWarnCommand(LolBans plugin) {
			super("acknowledge", plugin);
			setDescription("Allows players to acknowledge warns they receive");
			setPermission("lolbans.acknowledge");
        }
        
        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
            sender.sendMessage(Messages.invalidSyntax);
            try {
                sender.sendMessage(
                        Messages.translate("syntax.unwarn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
        public boolean run(User sender, String commandLabel, String[] args) {
            Debug debug = new Debug(getClass());
            
            if (!sender.hasPermission("lolbans.acknowledge"))
                return sender.permissionDenied("lolbans.acknowledge");

            Punishment punishment = sender.getLatestPunishmentOfType(PunishmentType.WARN);

            // if (punishment != null) {
            //     debug.print(String.valueOf(punishment.getWarningAck()));
            // }

            // if (punishment == null) {
            //     debug.print("Blocking acknowledge from executing - no punishment found");
            // }

            // if (punishment != null && punishment.getWarningAck()) {
            //     debug.print("Blocking acknowledge from executing - warning is marked as acknowledged");
            // }
            
            if (punishment == null || punishment.getWarningAck()) {
                return sender.sendReferencedLocalizedMessage("warn.not-warned", null, true);
            }

            punishment.setWarningAck(true);
            punishment.update();
            // punishment.broadcast();

            return sender.sendReferencedLocalizedMessage("warn.accept-message", null, true);
        }
    }
}