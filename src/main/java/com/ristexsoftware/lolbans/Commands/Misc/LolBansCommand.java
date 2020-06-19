package com.ristexsoftware.lolbans.Commands.Misc;

import java.util.TreeMap;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.RistExCommandAsync;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.ImportUtil;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.PermissionUtil;

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
        if (!PermissionUtil.Check(sender, "lolbans.reload"))
            return User.PermissionDenied(sender, "lolbans.reload");

        if (args.length < 1) {
            try {

                // self.saveConfig();
                Messages.Reload();
                self.reloadConfig();
                sender.sendMessage(Messages.Prefix + ChatColor.GREEN + "Reloaded LolBans successfully!");
            } catch (Exception e) {
                sender.sendMessage(Messages.ServerError);
            }
        } else if (args[0].equalsIgnoreCase("import")) {
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("essentials")) {
                    ImportUtil.Essentials(sender);
                } else if(args[1].equalsIgnoreCase("cancel")) {
                    if(ImportUtil.ActiveTask()) {
                        ImportUtil.Cancel();
                        sender.sendMessage(Messages.Prefix + ChatColor.GREEN + "Cancelled active import task!");
                    }
                }
            }

        }
        return true;
    }
}