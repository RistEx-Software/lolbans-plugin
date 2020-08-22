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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.google.common.collect.ImmutableList;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.utils.IPUtil;
import com.ristexsoftware.lolbans.api.utils.PunishID;
import com.ristexsoftware.lolbans.common.utils.Timing;
import com.ristexsoftware.lolbans.common.utils.Debug;

import inet.ipaddr.IPAddress;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddressString;


public class UnIPBan extends AsyncCommand {

    public UnIPBan(LolBans plugin) {
        super("unipban", plugin);
        this.setDescription("Remove an IP or cidr range ban");
        this.setPermission("lolbans.unipban");
        this.setAliases(Arrays.asList(new String[] { "unbanip", "ip-unban", "ipunban" }));
        setSyntax(getPlugin().getLocaleProvider().get("syntax.ip-unban"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
        sender.sendMessage(
                LolBans.getPlugin().getLocaleProvider().translate("syntax.unipban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        if (args.length < 2) {
            ArrayList<String> punishments = new ArrayList<>();
            for (Punishment punishment : LolBans.getPlugin().getPunishmentCache().getAll()) {
                if (punishment.getType() == PunishmentType.IP && !punishment.getAppealed() && punishments.contains(punishment.getIpAddress().toString()))
                    punishments.add(punishment.getIpAddress().toString());
            }
            return punishments;
        }

        return Arrays.asList();
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        Timing t = new Timing();
        Debug debug = new Debug(getClass());    
        
        if (!sender.hasPermission("lolbans.ipunban"))
            return sender.permissionDenied("lolbans.ipunban");
        
        // Syntax: /unipban [-s] <CIDR|PunishID> <Reason>
        try 
        {
            Arguments a = new Arguments(args);
            a.optionalFlag("silent", "-s");
            a.requiredString("cidrOrId");
            a.requiredSentence("reason");

            if (!a.valid())
                return false;

            boolean silent = a.getBoolean("-s");
            String cidrOrId = a.get("cidrOrId");
            String reason = a.get("reason");

            Punishment punishment = null;

            HostName host = new HostName(cidrOrId);
            if (host.asInetAddress() != null) {
                debug.print("Found valid hostname " + cidrOrId + " - checking for existing punishments...");
                punishment = Punishment.findPunishment(PunishmentType.IP, cidrOrId, false);
            } else if (PunishID.validateID(cidrOrId)) {
                debug.print("Found valid punishment ID " + cidrOrId + " - checking for existing punishments...");
                punishment = Punishment.findPunishment(cidrOrId);
            }

            if (punishment == null) {
                debug.print("Could not find existing punishment");
                return sender.sendReferencedLocalizedMessage("ip-ban.ip-is-not-banned", cidrOrId, true);
            }

            punishment.appeal(sender, reason, silent);
            punishment.broadcast();

            t.finish(sender);
            
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
        }
        return true;
    }
    
}