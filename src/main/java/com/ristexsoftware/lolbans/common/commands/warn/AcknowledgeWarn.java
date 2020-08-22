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

package com.ristexsoftware.lolbans.common.commands.warn;

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
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.common.utils.Debug;

public class AcknowledgeWarn extends AsyncCommand {
    public AcknowledgeWarn(LolBans plugin) {
        super("acknowledge", plugin);
        setDescription("Allows players to acknowledge warns they receive");
        setPermission("lolbans.acknowledge");
        setSyntax(getPlugin().getLocaleProvider().get("syntax.warn-accept"));
    }
    
    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
        sender.sendMessage(
                LolBans.getPlugin().getLocaleProvider().translate("syntax.unwarn", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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