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
import com.ristexsoftware.lolbans.api.configuration.Messages;
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
                    
                boolean silent = a.getBoolean("silent");
                Timestamp expiry = a.getTimestamp("expiry");
                String reason = expiry == null ? a.get("Time") + " " + (a.get("Reason") == null ? "" : a.get("Reason")) : a.get("Reason");
                
                if (reason == null || reason.trim().equals("null")) {
                    String configReason = LolBans.getPlugin().getConfig().getString("BanSettings.DefaultReason");
                    reason = configReason == null ? "Your account has suspended!" : configReason;
                };

                if (expiry == null && !sender.hasPermission("lolbans.ipban.perm"))
                    return sender.permissionDenied("lolbans.ipban.perm"); 
                
                // Is a future, needed != null for some reason.
                IPAddress target = new IPAddressString(a.get("cidr")).toAddress();
                if (target == null) 
                    return false;
                    
                // TODO: handle this better? Send the banned subnet string instead of the address they tried to ban?
                Optional<ResultSet> res = IPUtil.isBanned(target.toInetAddress()).get();
                if (res.isPresent() && res.get().next())
                    return sender.sendReferencedLocalizedMessage("ip-ban.ip-is-banned", target.toString(), true);

                if (IPUtil.isTargetIpInsane(target) && !sender.hasPermission("lolbans.insanityoverride")) {
                        // Format our messages.
                    try 
                    {
                        final int fuckingfinal = IPUtil.getBanAffected(target);
                        String insanity = Messages.translate("ip-ban.insanity",
                            new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                            {{
                                put("ipaddress", String.valueOf(target.toAddressString()));
                                put("arbiter", sender.getName());
                                put("AFFECTEDPLAYERS", String.valueOf(fuckingfinal));
                                put("TOTALPLAYERS", String.valueOf(LolBans.getPlugin().getOnlineUsers().size()));
                                put("INSANEPERCENT", String.valueOf(IPUtil.getBanPercentage(target)));
                                put("INSANETHRESHOLD", String.valueOf(LolBans.getPlugin().getConfig().getDouble("ban-settings.insane.trigger")));
                            }}
                        );
                        sender.sendMessage(insanity);
                    }
                    catch (InvalidConfigurationException e)
                    {
                        e.printStackTrace();
                        sender.sendMessage(Messages.serverError);
                    }
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
                sender.sendMessage(Messages.serverError);
            }
			return true;
        }
        
    }

    public static class Unban extends AsyncCommand {

        public Unban(LolBans plugin) {
            super("unipban", plugin);
            this.setDescription("Remove an IP or cidr range ban");
            this.setPermission("lolbans.unipban");
            this.setAliases(Arrays.asList(new String[] { "unbanip", "ip-unban", "ipunban" }));
        }

        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
            sender.sendMessage(Messages.invalidSyntax);
            try {
                sender.sendMessage(
                        Messages.translate("syntax.unipban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
                sender.sendMessage(Messages.serverError);
            }
            return true;
        }
        
    }

}  