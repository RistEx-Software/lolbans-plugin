
package com.ristexsoftware.lolbans.common.commands.mute;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.InvalidConfigurationException;
import com.ristexsoftware.lolbans.api.punishment.Punishment;
import com.ristexsoftware.lolbans.api.punishment.PunishmentType;
import com.ristexsoftware.lolbans.common.utils.Timing;

public class Unmute extends AsyncCommand {

    public Unmute(LolBans plugin) {
        super("unmute", plugin);
        this.setDescription("Remove a player mute");
        this.setPermission("lolbans.unmute");
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {
        sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("invalidSyntax"));
        sender.sendMessage(LolBans.getPlugin().getLocaleProvider().translate("syntax.unmute",
                new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        if (args.length < 2) {
            ArrayList<String> punishments = new ArrayList<>();
            for (Punishment punishment : LolBans.getPlugin().getPunishmentCache().getAll()) {
                if (punishment.getType() == PunishmentType.MUTE && !punishment.getAppealed()
                        && punishments.contains(punishment.getTarget().getName()))
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
        Punishment oldPunishment = target.getLatestPunishmentOfType(PunishmentType.MUTE);
        if (oldPunishment != null && oldPunishment.getPunisher().getUniqueId() != sender.getUniqueId() && !sender.hasPermission("lolbans.unban.others"))
            return sender.sendReferencedLocalizedMessage("mute.cannot-unmute-other", target.getName(), true);

        String reason = a.get("reason");
        if (reason == null || reason.trim().equals("null")) {
            String configReason = getPlugin().getLocaleProvider().get("mute.default-reason", "You have been muted!");
        }

        Punishment punishment = target.removeLatestPunishmentOfType(PunishmentType.MUTE, sender, reason, silent);
        punishment.broadcast();
        time.finish(sender);

        return true;
    }

}