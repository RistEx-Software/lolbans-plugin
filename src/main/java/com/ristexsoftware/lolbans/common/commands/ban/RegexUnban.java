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

public class RegexUnban extends AsyncCommand {

    public RegexUnban(LolBans plugin) {
        super("regexunban", plugin);
        this.setDescription("Ban a username/hostname/ip address based on a Regular Expression");
        this.setPermission("lolbans.regexunban");
        setSyntax(getPlugin().getLocaleProvider().get("syntax.regex-unban"));
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
        if (!sender.hasPermission("lolbans.regexunban"))
            return sender.permissionDenied("lolbans.regexunban");

        Timing timing = new Timing();
        try {
            Arguments a = new Arguments(args);
            a.optionalFlag("silent", "-s");
            a.optionalFlag("contains", "-c");
            a.optionalFlag("ignoreCase", "-i");
            a.requiredString("regex");
            a.optionalSentence("reason");

            if (!a.valid())
                return false;

            boolean silent = a.getFlag("silent");
            String regexString = a.get("regex");
            String reason = a.get("reason");
            boolean contains = a.getFlag("contains");
            boolean ignoreCase = a.getFlag("ignoreCase");

            if (contains) 
                regexString = ("(?<=|^)" + regexString + "(?=|$)");

            if (ignoreCase) 
                regexString = ("(?i)" + regexString);
            
            Pattern regex = null;
            
            try {
                regex = Pattern.compile(regexString);
            } catch (Exception e ) {
                return false;
            }
            
            Punishment punishment = Punishment.findPunishment(PunishmentType.REGEX, regex.toString(), false);
            if (punishment == null)
                return sender.sendReferencedLocalizedMessage("regex-ban.regex-is-not-banned", regex.toString(), true);

            punishment.appeal(sender, reason, silent);
            punishment.broadcast();
            
        } catch (Exception e) {
            e.printStackTrace(); 
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
        }
        return true;
    }
    
}