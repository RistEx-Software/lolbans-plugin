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

package com.ristexsoftware.lolbans.common.commands;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

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
			return null;
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
			debug.print(sender.getName() + " is executing a command");
			try {
				Arguments a = new Arguments(args);
				a.optionalFlag("silent", "-s");
				a.optionalFlag("overwrite", "-o");
				a.requiredString("username");
				a.optionalTimestamp("expiry");
				a.optionalSentence("reason"); 
				
				if (!a.valid()) 
					return false;
				
				boolean silent = a.exists("silent");
				boolean overwrite = a.exists("overwrite");
				String username = a.get("username");
				Timestamp expiry = !a.exists("expiry") ? null : a.getTimestamp("expiry");

				User target = User.resolveUser(username);

				if (overwrite && !sender.hasPermission("lolbans.ban.overwrite"))
					return sender.permissionDenied("lolbans.ban.overwrite");

				if (target.isBanned() && !overwrite)
					return sender.sendReferencedLocalizedMessage("ban.player-is-banned", target.getName(), false);
			
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
				punishment.commit(sender);
			
				if (overwrite) {
					target.removeLatestPunishmentOfType(PunishmentType.BAN, sender,
							"Overwritten by #" + punishment.getPunishID(), silent);
				}
				if (target.isOnline())
					target.disconnect(punishment);

				punishment.broadcast();

				time.finish(sender);
				debug.print("Command completed");
			} catch (Exception e ){ 
				e.printStackTrace();
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
			return null;
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
			
			boolean silent = a.exists("silent");
			String username = a.get("username");

			User target = User.resolveUser(username);

			if (!target.isBanned())
				return sender.sendReferencedLocalizedMessage("ban.player-is-not-banned", target.getName(), false);
		
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