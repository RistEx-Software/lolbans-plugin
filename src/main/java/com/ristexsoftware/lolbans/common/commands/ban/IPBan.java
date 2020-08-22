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

public class IPBan {

    public static class Ban extends AsyncCommand {

        public Ban(LolBans plugin) {
            super("ipban", plugin);
            setDescription("Ban an ip address or cidr range");
            setPermission("lolbans.ipban");
            setAliases(Arrays.asList(new String[] { "ip-ban", "banip" }));
            setSyntax(getPlugin().getLocaleProvider().get("syntax.ip-ban"));
        }

        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
            sender.sendMessage(
                    LolBans.getPlugin().getLocaleProvider().translate("syntax.ban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        }

        @Override
        public List<String> onTabComplete(User sender, String[] args) {
            if (args.length < 2) {
				ArrayList<String> usernames = new ArrayList<>();
				for (User user : LolBans.getPlugin().getUserCache().getAll()) {
					usernames.add(user.getName());
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
            Timing t = new Timing();
            
            try {
                Arguments a = new Arguments(args);
                a.optionalFlag("silent", "-s");
                a.requiredString("cidr");
                a.optionalTimestamp("expiry");
                a.requiredSentence("reason");

                if (!a.valid())
                    return false;

                if (a.get("cidr") == null) 
                    return false;
                    
                boolean silent = a.getFlag("silent");
                Timestamp expiry = a.getTimestamp("expiry");
                String reason = expiry == null ? a.get("Time") + " " + (a.get("Reason") == null ? "" : a.get("Reason")) : a.get("Reason");
                
                if (reason == null || reason.trim().equals("null")) {
                    reason = getPlugin().getLocaleProvider().getDefault("ban.default-reason" , "Your account has been suspended!");
                };

                if (expiry == null && !sender.hasPermission("lolbans.ipban.perm"))
                    return sender.permissionDenied("lolbans.ipban.perm"); 
                
                // Is a future, needed != null for some reason.
                IPAddress target = null;
                System.out.println(LolBans.getPlugin().getOnlineUser(a.get("cidr")));
                try {
                    target = LolBans.getPlugin().getOnlineUser(a.get("cidr")) == null ? new IPAddressString(a.get("cidr")).toAddress() : LolBans.getPlugin().getOnlineUser(a.get("cidr")).getAddress();
                } catch (Exception e) {
                    // TODO: Add a ip is invalid message!
                    return false;
                }
                    
                // TODO: handle this better? Send the banned subnet string instead of the address they tried to ban?
                Optional<ResultSet> res = IPUtil.isBanned(target.toInetAddress()).get();
                if (res.isPresent() && res.get().next())
                    return sender.sendReferencedLocalizedMessage("ip-ban.ip-is-banned", target.toString(), true);

                if (IPUtil.isTargetIpInsane(target) && !sender.hasPermission("lolbans.insanityoverride")) {
                        // Format our messages.
            
                    final int fuckingfinal = IPUtil.getBanAffected(target);
                    final IPAddress javaisfuckingstupidaboutfinalvariables = target;
                    String insanity = LolBans.getPlugin().getLocaleProvider().translate("ip-ban.insanity",
                        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                        {{
                            put("ipaddress", String.valueOf(javaisfuckingstupidaboutfinalvariables.toString()));
                            put("arbiter", sender.getName());
                            put("AFFECTEDPLAYERS", String.valueOf(fuckingfinal));
                            put("TOTALPLAYERS", String.valueOf(LolBans.getPlugin().getOnlineUsers().size()));
                            put("INSANEPERCENT", String.valueOf(IPUtil.getBanPercentage(javaisfuckingstupidaboutfinalvariables)));
                            put("INSANETHRESHOLD", String.valueOf(LolBans.getPlugin().getConfig().getDouble("ban-settings.insane.trigger")));
                        }}
                    );
                    sender.sendMessage(insanity);
                
                    return true;
                }
                
                Punishment punishment = new Punishment(sender, reason, expiry, silent, false, target);
                punishment.commit(sender);
                punishment.broadcast();

                // Send messages to all players (if not silent) or only to admins (if silent)
                // and also kick players who match the ban.
                for (User user : LolBans.getPlugin().getOnlineUsers())
                {
                    HostName host = new HostName(user.getAddress());
                    if (target.contains(host.asAddress())) {
                        user.disconnect(punishment);
                    }
                }

                // SendIP
                // DiscordUtil.GetDiscord().SendBanObject(sender, thingy.toString(), reason, banid, punishtime);
                
                t.finish(sender);
                
            } catch(Exception e) {
                e.printStackTrace();
                sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
            }
			return true;
        }
        
    }


}  