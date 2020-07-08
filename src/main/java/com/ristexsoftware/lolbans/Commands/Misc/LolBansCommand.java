package com.ristexsoftware.lolbans.Commands.Misc;

import java.util.Map;
import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ImportUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.NumberUtil;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;
import com.ristexsoftware.lolbans.Utils.Statistics;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

// Yeet
public class LolBansCommand extends RistExCommandAsync {
    private static Main self = Main.getPlugin(Main.class);

    public LolBansCommand(Plugin owner) {
        super("lolbans", owner);
        this.setDescription("Reload config.yml and messages.yml");
        this.setPermission("lolbans.reload");
    }

    @Override
    public void onSyntaxError(CommandSender sender, String label, String[] args) {
        try {
            sender.sendMessage(Messages.InvalidSyntax);
            sender.sendMessage(
                    Messages.Translate("Syntax.LolBans", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            sender.sendMessage(Messages.ServerError);
        }
    }

    @Override
    public boolean Execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!PermissionUtil.Check(sender, "lolbans.reload"))
                return User.PermissionDenied(sender, "lolbans.reload");

            try {
                // self.saveConfig();
                Messages.Reload();
                self.reloadConfig();
                sender.sendMessage(Messages.Prefix + ChatColor.GREEN + "Reloaded LolBans successfully!");
            } catch (Exception e) {
                sender.sendMessage(Messages.ServerError);
            }
        } else if (args[0].equalsIgnoreCase("import")) {
            if (!PermissionUtil.Check(sender, "lolbans.import"))
                return User.PermissionDenied(sender, "lolbans.import");

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("essentials")) {
                    ImportUtil.importEssentials(sender);
                } else if (args[1].equalsIgnoreCase("litebans")) {
                    ImportUtil.importLitebans(sender);
                } else if (args[1].equalsIgnoreCase("cancel")) {
                    if (ImportUtil.ActiveTask()) {
                        ImportUtil.Cancel();
                        sender.sendMessage(Messages.Prefix + ChatColor.GREEN + "Cancelled active import task!");
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("stats")) {
            if (!PermissionUtil.Check(sender, "lolbans.stats"))
                return User.PermissionDenied(sender, "lolbans.stats");

            Map<String, String> Variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {
                {
                    put("player", sender.getName());
                    put("punishcount", Statistics.getTotalPunishments().toString());
                    put("bans", Statistics.getBansCount().toString());
                    put("banpercent", String.valueOf(NumberUtil.getPercentage(Statistics.getBansCount() + Statistics.getIPCount() + Statistics.getRegexCount(), Statistics.getTotalPunishments(), 0.00)));
                    put("ipbans", Statistics.getIPCount().toString());
                    put("regex", Statistics.getRegexCount().toString());
                    put("mutes", Statistics.getMutesCount().toString());
                    put("mutepercent", String.valueOf(NumberUtil.getPercentage(Statistics.getMutesCount(), Statistics.getTotalPunishments(), 0.00)));
                    put("warns", Statistics.getWarnsCount().toString());
                    put("warnpercent", String.valueOf(NumberUtil.getPercentage(Statistics.getWarnsCount(), Statistics.getTotalPunishments(), 0.00)));
                    put("kicks", Statistics.getKicksCount().toString());
                    put("kickpercent", String.valueOf(NumberUtil.getPercentage(Statistics.getKicksCount(), Statistics.getTotalPunishments(), 0.00)));
                    put("userscount", Statistics.getUsersCount().toString());
                    put("userspunished", Statistics.getUniquePunishmentsCount().toString());
                    put("percentpunished", String.valueOf(NumberUtil.getPercentage(Statistics.getUniquePunishmentsCount(), Statistics.getUsersCount(), 0.00)));
                }
            };
            try {
                sender.sendMessage(Messages.Translate("Statistics", Variables));
            } catch (InvalidConfigurationException e) {
                sender.sendMessage(Messages.ServerError);
                e.printStackTrace();
            }
        }
        return true;
    }
}