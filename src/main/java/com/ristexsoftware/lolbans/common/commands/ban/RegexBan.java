package com.ristexsoftware.lolbans.common.commands.ban;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.common.utils.Timing;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexBan extends AsyncCommand {

    public RegexBan(LolBans plugin) {
        super("regexban", plugin);
        this.setDescription("Ban a username/hostname/ip address based on a Regular Expression");
        this.setPermission("lolbans.regexban");
        setSyntax(getPlugin().getLocaleProvider().get("syntax.regex-ban"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
        sender.sendMessage(
                LolBans.getPlugin().getLocaleProvider().translate("syntax.regexban", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        return null;
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) {
        if (!sender.hasPermission("lolbans.regexban"))
            return sender.permissionDenied("lolbans.regexban");

        Timing time = new Timing();

        try {
            Arguments a = new Arguments(args);
            a.optionalFlag("silent", "-s");
            a.optionalFlag("overwrite", "-o");
            a.optionalFlag("contains", "-c");
            a.optionalFlag("ignoreCase", "-i");
            a.requiredString("regex");
            a.optionalTimestamp("expiry");
            a.optionalSentence("reason");

            if (!a.valid())
                return false;

            boolean silent = a.getFlag("silent");
            boolean overwrite = a.getFlag("overwrite");
            String regexString = a.get("regex");
            Timestamp expiry = a.getTimestamp("expiry");
            Pattern regex = null;
            boolean contains = a.getFlag("contains");
            boolean ignoreCase = a.getFlag("ignoreCase");

            if (contains) 
                regexString = ("(?<=|^)" + regexString + "(?=|$)");

            if (ignoreCase) 
                regexString = ("(?i)" + regexString);

            try {
                regex = Pattern.compile(regexString);
            } catch (PatternSyntaxException ex) {
                return false;
            }

            if (overwrite && !sender.hasPermission("lolbans.ban.overwrite"))
                return sender.permissionDenied("lolbans.ban.overwrite");

            Punishment exists = Punishment.findPunishment(PunishmentType.REGEX, regex.toString(), false);
            if (exists != null && !overwrite)
                return sender.sendReferencedLocalizedMessage("regex-ban.regex-is-banned", regex.toString(), false);

            if (expiry == null && !sender.hasPermission("lolbans.regexban.perm"))
                return sender.permissionDenied("lolbans.regexban.perm");

            if (expiry != null && expiry.getTime() > sender.getTimeGroup().getTime())
                expiry = sender.getTimeGroup();

            String reason = a.get("reason");
            if (reason == null || reason.trim().equals("null"))
                reason = getPlugin().getLocaleProvider().get("ban.default-reason", "Your account has been suspended!");

            Punishment punishment = new Punishment(sender, reason, expiry, silent, false, regex.toString());
            
            if (overwrite) {
                Punishment last = Punishment.findPunishment(PunishmentType.REGEX, regex.toString(), false);
                if (last != null) {
                    last.appeal(sender, "Overwritten by #" + punishment.getPunishID(), silent);
                }
            }
            punishment.commit(sender);
            punishment.broadcast();
            time.finish();
            
        } catch (Exception e) {

        }
        return true;
    }
    
}