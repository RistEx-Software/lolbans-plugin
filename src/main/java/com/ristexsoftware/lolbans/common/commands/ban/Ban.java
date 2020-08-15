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

package com.ristexsoftware.lolbans.common.commands.ban;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.ristexsoftware.knappy.util.Debugger;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.common.utils.Debug;
import com.ristexsoftware.lolbans.common.utils.Timing;

public class Ban {

	public static class BanCommand extends AsyncCommand {

		public BanCommand(LolBans plugin) {
			super("ban", plugin);
			setDescription("Ban a player");
			setPermission("lolbans.ban");
			setAliases(Arrays.asList(new String[] { "eban", "tempban" }));
		}

		@Override
		public void onSyntaxError(User sender, String label, String[] args) {
			sender.sendMessage(Messages.invalidSyntax);
			try {
				sender.sendMessage(
						Messages.translate("syntax.ban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
				sender.sendMessage(Messages.serverError);
			}
		}

		@Override
		public List<String> onTabComplete(User sender, String[] args) {
			if (args.length < 2) {
				ArrayList<String> usernames = new ArrayList<>();

				for (User user : LolBans.getPlugin().getUserCache().getAll()) {
					usernames.add(user.getName());
				}

				// TODO: Make this faster!!!
				// if(!args[0].equals("")) {
				// 	for(User user : LolBans.getPlugin().getUserCache().getAll()) {
				// 		if(user.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
				// 			usernames.add(user.getName());
				// 		}
				// 	}
				// } else {
				// 	// Instead of creating a stupid for loop here, let's just stream 
				// 	usernames = (ArrayList<String>) LolBans.getPlugin().getUserCache().getAll().stream()
				// 	.map(user -> user.getName())
				// 	.collect(Collectors.toList());
				// }

				return usernames;
			}
	
			if (args.length < 3) {	
				return ImmutableList.of("1m", "15m", "1h", "3h", "12h", "1d", "1w", "1mo", "1y");
			}

			return Arrays.asList(); // u cute
		}

		@Override
		public boolean run(User sender, String commandLabel, String[] args) {

			// Even if we set the permission in the constructor, we have to check here
			// just incase the user doesn't enable the real permission check in bukkit's 
			// command constructor, I don't want to force this because the command doesn't 
			// show up otherwise.
			if (!sender.hasPermission("lolbans.ban"))
				return sender.permissionDenied("lolbans.ban");
			
			// Let's start timing how long this command takes
			Debug debug = new Debug(getClass());
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
				
				boolean silent = a.getBoolean("silent");
				boolean overwrite = a.getBoolean("overwrite");
				String username = a.get("username");
				Timestamp expiry = !a.exists("expiry") ? null : a.getTimestamp("expiry");

				User target = User.resolveUser(username);

				if (target == null)
					return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("username"), true);

				if (overwrite && !sender.hasPermission("lolbans.ban.overwrite"))
					return sender.permissionDenied("lolbans.ban.overwrite");

				if (target.isPunished(PunishmentType.BAN) && !overwrite)
					return sender.sendReferencedLocalizedMessage("ban.player-is-banned", target.getName(), true);
			
				if (expiry == null && !sender.hasPermission("lolbans.ban.perm"))
					return sender.permissionDenied("lolbans.ban.perm");

				if (expiry != null && expiry.getTime() > sender.getTimeGroup().getTime())
					expiry = sender.getTimeGroup();

				String reason = a.get("reason");
				if (reason == null || reason.trim().equals("null")) {
					String configReason = Messages.getMessages().getConfig().getString("ban.default-reason");
					reason = configReason == null ? "Your account has been suspended!" : configReason;
				}
								
				Punishment punishment = new Punishment(PunishmentType.BAN, sender, target, reason, expiry, silent, false);
				
				if (overwrite) {
					target.removeLatestPunishmentOfType(PunishmentType.BAN, sender,
					"Overwritten by #" + punishment.getPunishID(), silent);
				}
				if (target.isOnline()) {
					if (target.hasPermission("lolbans.ban.immune"))
						return sender.permissionDenied("lolbans.ban.immune"); // TODO: Make a new message for this case?
					target.disconnect(punishment);
				}
				
				punishment.commit(sender);
				punishment.broadcast();
				time.finish(sender);
				debug.print("Command completed");
			} catch (Exception e ){ 
				e.printStackTrace();
				sender.sendMessage(Messages.serverError);
			}
			return true;
		}
    }

	// !IMPORTANT FIXME: Unban command doesn't pull from cache after a ban was just created
	public static class UnbanCommand extends AsyncCommand {

		public UnbanCommand(LolBans plugin) {
			super("unban", plugin);
			this.setDescription("Remove a player ban");
			this.setPermission("lolbans.unban");
			this.setAliases(Arrays.asList(new String[]{}));
		}

		@Override
		public void onSyntaxError(User sender, String label, String[] args) {
			sender.sendMessage(Messages.invalidSyntax);
			try {
				sender.sendMessage(
						Messages.translate("syntax.unban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
				sender.sendMessage(Messages.serverError);
			}
		}

		@Override
		public List<String> onTabComplete(User sender, String[] args) {
			if (args.length < 2) {
				ArrayList<String> punishments = new ArrayList<>();
				for (Punishment punishment : LolBans.getPlugin().getPunishmentCache().getAll()) {
					if (punishment.getType() == PunishmentType.BAN && !punishment.getAppealed() && !punishments.contains(punishment.getTarget().getName()))
						punishments.add(punishment.getTarget().getName());
				}
				return punishments;
				// TODO: Make this faster... And make it work
/* 				if(!args[0].equals("")) {
					for(Punishment punish : LolBans.getPlugin().getPunishmentCache().getAll()) {
						if (punish.getType() != PunishmentType.BAN && punish.getAppealed() && punishments.contains(punish.getTarget().getName())) continue;
						if(punish.getTarget().getName().toLowerCase().startsWith(args[0].toLowerCase())) {
							punishments.add(punish.getTarget().getName());
						}
					}
				} else {
					// Instead of creating a stupid for loop here, let's just stream 
					LolBans.getPlugin().getPunishmentCache().getAll().stream()
					.forEach(punish -> {
						if (punish.getType() == PunishmentType.BAN && !punish.getAppealed() && !punishments.contains(punish.getTarget().getName())) 
							punishments.add(punish.getTarget().getName());
					});
				}

				return punishments; */
			}

			return Arrays.asList();
		}

		@Override
		public boolean run(User sender, String commandLabel, String[] args) {
			Timing time = new Timing();

			Arguments a = new Arguments(args);
			a.optionalFlag("silent", "-s");
			a.requiredString("username");
			a.optionalSentence("reason"); 
			
			if (!a.valid()) 
				return false;
			
			boolean silent = a.getBoolean("silent");
			String username = a.get("username");

			User target = User.resolveUser(username);

			if (target == null)
				return sender.sendReferencedLocalizedMessage("player-doesnt-exist", a.get("username"), true);

			if (!target.isPunished(PunishmentType.BAN))
				return sender.sendReferencedLocalizedMessage("ban.player-is-not-banned", target.getName(), true);
		
			String reason = a.get("reason");
			if (reason == null || reason.trim().equals("null")) {
				String configReason = Messages.getMessages().getConfig().getString("ban.default-reason");
				reason = configReason == null ? "Your account has been suspended!" : configReason;
			}

			Punishment punishment = target.removeLatestPunishmentOfType(PunishmentType.BAN, sender, reason, silent);
			punishment.broadcast();
			time.finish(sender);
			
			boolean uwu = true;
			return uwu;
		}
		
	}
}