package com.ristexsoftware.lolbans.common.commands.mute;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.common.utils.Debug;
import com.ristexsoftware.lolbans.common.utils.Timing;

public class Mute {

    public static class MuteCommand extends AsyncCommand {

        public MuteCommand(String name, LolBans plugin) {
            super("mute", plugin);
            setDescription("Mute a player");
			setPermission("lolbans.mute");
			setAliases(Arrays.asList(new String[] { "emute", "tempmute" }));
        }

        @Override
        public void onSyntaxError(User sender, String label, String[] args) {
			sender.sendMessage(Messages.invalidSyntax);
			try {
				sender.sendMessage(
						Messages.translate("syntax.mute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
                
                boolean silent = a.getBoolean("silent");
                boolean overwrite = a.getBoolean("overwrite");
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
                    String configReason = Messages.getMessages().getConfig().getString("ban.default-reason");
                    reason = configReason == null ? "Your account has been suspended!" : configReason;
                }
                                
                Punishment punishment = new Punishment(PunishmentType.MUTE, sender, target, reason, expiry, silent, false);

                if (overwrite && target.isPunished(PunishmentType.MUTE)) {
                    target.removeLatestPunishmentOfType(PunishmentType.MUTE, sender, "Overwritten by #" + punishment.getPunishID(), silent);
                }
                if (target.isOnline())
                    target.disconnect(punishment);
                
                punishment.commit(sender);
                punishment.broadcast();
                time.finish(sender);
            } catch(Exception e) { 
                e.printStackTrace();
                sender.sendMessage(Messages.serverError);
            }
            return true;
        }

        public static class UnmuteCommand extends AsyncCommand {

            public UnmuteCommand(String name, LolBans plugin) {
                super("unmute", plugin);
                this.setDescription("Remove a player mute");
			    this.setPermission("lolbans.unmute");
            }

            @Override
            public void onSyntaxError(User sender, String label, String[] args) {
                sender.sendMessage(Messages.invalidSyntax);
                try {
                    sender.sendMessage(
                            Messages.translate("syntax.unmute", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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
                        if (punishment.getType() == PunishmentType.MUTE && !punishment.getAppealed() && punishments.contains(punishment.getTarget().getName()))
                            punishments.add(punishment.getTarget().getName());
                    }
                    return punishments;
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

			if (!target.isPunished(PunishmentType.MUTE))
				return sender.sendReferencedLocalizedMessage("mute.player-is-not-muted", target.getName(), false);
		
			String reason = a.get("reason");
			if (reason == null || reason.trim().equals("null")) {
				String configReason = Messages.getMessages().getConfig().getString("mute.default-reason");
				reason = configReason == null ? "You have been muted!" : configReason;
			}

			Punishment punishment = target.removeLatestPunishmentOfType(PunishmentType.MUTE, sender, reason, silent);
			punishment.broadcast();
			time.finish(sender);
			
            return true;
            }
            
        }
    }
}