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
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.utils.TimeUtil;
import com.ristexsoftware.lolbans.common.utils.Debug;
import com.ristexsoftware.lolbans.common.utils.Timing;

// !IMPORTANT FIXME: Unban command doesn't pull from cache after a ban was just created
public class Unban extends AsyncCommand {

    public Unban(LolBans plugin) {
        super("unban", plugin);
        this.setDescription("Remove a player ban");
        this.setPermission("lolbans.unban");
        this.setAliases(Arrays.asList(new String[]{}));
        setSyntax(getPlugin().getLocaleProvider().get("syntax.unban"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("syntaxError"));
        sender.sendMessage(
                LolBans.getPlugin().getLocaleProvider().translate("syntax.unban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
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

        if (target.getLatestPunishmentOfType(PunishmentType.BAN) != null && target.getLatestPunishmentOfType(PunishmentType.BAN).getPunisher().getUniqueId() != sender.getUniqueId() && !sender.hasPermission("lolbans.unban.others"))
            return sender.sendReferencedLocalizedMessage("ban.cannot-unban-other", target.getName(), true);
    
        String reason = a.get("reason");
        if (reason == null || reason.trim().equals("null")) {
            String configReason = getPlugin().getLocaleProvider().get("ban.default-reason");
            reason = configReason == null ? "Your account has been suspended!" : configReason;
        }

        Punishment punishment = target.removeLatestPunishmentOfType(PunishmentType.BAN, sender, reason, silent);
        punishment.broadcast();
        time.finish(sender);
        
        boolean uwu = true;
        return uwu;
    }
    
}