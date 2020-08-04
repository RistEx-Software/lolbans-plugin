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

package com.ristexsoftware.lolbans.commands;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;

public class Ban {

	public static class BanCommand extends AsyncCommand {

		public BanCommand(LolBans plugin) {
			super("ban", plugin);
			setDescription("Ban a player");
			setPermission("lolbans.ban");
			setAliases(Arrays.asList(new String[] { "eban","tempban" }));
		}

		@Override
		public void onSyntaxError(User sender, String label, String[] args) {
			sender.sendMessage(Messages.invalidSyntax);
			try {
				sender.sendMessage(Messages.translate("syntax.ban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
			if (!sender.hasPermission("lolbans.ban"))
				return sender.permissionDenied("lolbans.ban");

			sender.sendMessage("hello!");
			return true;
		}

    }

	public static class UnbanCommand extends AsyncCommand {

		public UnbanCommand(LolBans plugin) {
			super("unban", plugin);
		}

		@Override
		public void onSyntaxError(User sender, String label, String[] args) {

		}

		@Override
		public List<String> onTabComplete(User sender, String[] args) {
			return null;
		}

		@Override
		public boolean run(User sender, String commandLabel, String[] args) {
			return false;
		}
		
	}
}