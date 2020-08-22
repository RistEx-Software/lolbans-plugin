package com.ristexsoftware.lolbans.common.commands.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ristexsoftware.knappy.translation.Translation;
import com.ristexsoftware.knappy.util.NumberUtil;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.utils.MemoryUtil.Unit;
import com.ristexsoftware.lolbans.common.commands.ban.Ban;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;

public class LolBans extends AsyncCommand {

    public LolBans(com.ristexsoftware.lolbans.api.LolBans plugin) {
        super("lolbans", plugin);
        setPermission("lolbans.plugin");
        setDescription("The LolBans help and reload command");
        setSyntax(getPlugin().getLocaleProvider().get("syntax.lolbans"));
    }

    @Override
    public void onSyntaxError(User sender, String label, String[] args) {

    }

    @Override
    public List<String> onTabComplete(User sender, String[] args) {
        return null;
    }

    @Override
    public boolean run(User sender, String commandLabel, String[] args) throws Throwable {
        if (!sender.hasPermission("lolbans.plugin"))
            return sender.permissionDenied("lolbans.plugin");

        if (args.length < 1)
            return runHelp(sender, args);

        Arguments a = new Arguments(args);
        a.requiredString("subCommand");
        String subCommand = a.get("subCommand").toLowerCase();
        String[] subArgs = Arrays.copyOfRange(a.getUnparsedArgs().toArray(new String[a.getUnparsedArgs().size()]), 1,
                a.getUnparsedArgs().size());
        switch (subCommand) {
            case "help":
                return runHelp(sender, subArgs);
            case "reload":
                return runReload(sender, subArgs);
            case "stats":
            case "statistics":
            case "analytics":
                return runStats(sender, subArgs);
            case "clear-cache":
                return runClearCache(sender, subArgs);
            default:
                return false;
        }
    }

    public boolean runClearCache(User sender, String[] args) {
        if (!sender.hasPermission("lolbans.clear-cache"))
            return sender.permissionDenied("lolbans.clear-cache");

        if (args.length < 1) {
            getPlugin().getPunishmentCache().getAll().clear();
            getPlugin().getUserCache().getAll().clear();
            sender.sendMessage(Translation.translateColors("&", getPlugin().getLocaleProvider().getDefaultTranslation("prefix") + "&aSuccessfully cleared cached!"));
            return true;
        } else {
            switch (args[0].toLowerCase()) {
                case "punishments":
                    getPlugin().getPunishmentCache().getAll().clear();
                    break;
                case "users":
                    getPlugin().getUserCache().getAll().clear();
                    break;
                default:
                    return false;
            }
            sender.sendMessage(Translation.translateColors("&", getPlugin().getLocaleProvider().getDefaultTranslation("prefix") + "&aSuccessfully cleared cached!"));
        }
        return true;
    }

    public boolean runReload(User sender, String[] args) {
        try {
            if (args.length < 1) {
                sender.sendMessage(Translation.translateColors("&", getPlugin().getLocaleProvider().getDefaultTranslation("prefix") + "&cWarning! This will clear the LolBans cache!"));
                sender.sendMessage(Translation.translateColors("&", "&f » &cPlease type &7/lolbans reload confirm &cto continue..."));
            } else if (args[0].equalsIgnoreCase("confirm")) {
                getPlugin().getConfigProvider().reloadConfig();
                getPlugin().getPunishmentCache().getAll().clear();
                getPlugin().getUserCache().getAll().clear();
                sender.sendMessage(Translation.translateColors("&", getPlugin().getLocaleProvider().getDefaultTranslation("prefix") + "&aSuccessfully reloaded LolBans!"));
            } else {
                return false;
            }
        } catch (Exception e) {
            sender.sendMessage(getPlugin().getLocaleProvider().getDefaultTranslation("serverError"));
        }
        return true;
    }

    // idk this is wack i've never used this api before
    public boolean runStats(User sender, String[] args) {

        TextComponent punishments = new TextComponent(Translation.translateColors("&", "&f» &bPunishment Cache: &f"
                                 + NumberUtil.getPercentage(getPlugin().getPunishmentCache().memoryUsage(Unit.MEGABYTES).intValue(), 
                        getPlugin().getPunishmentCache().getMaxMemoryUsage() / 1000 / 8 / 1024, 0.00)
                        + "%"));
                        
        // TODO add in more info to hover events
        punishments.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(Translation.translateColors("&", "&bClick to clear cache.")).create()));
        punishments.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/lolbans clear-cache punishments"));
        

        TextComponent users = new TextComponent(Translation.translateColors("&", "&f» &bUser Cache: &f" 
        + NumberUtil.getPercentage(getPlugin().getUserCache().memoryUsage(Unit.MEGABYTES).intValue(), getPlugin().getUserCache().getMaxMemoryUsage() / 1000 / 8 / 1024, 0.00) 
                + "%"));
        
        users.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(Translation.translateColors("&", "&bClick to clear cache.")).create()));
        users.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/lolbans clear-cache users"));

        // TextComponent reports = new TextComponent(bla bla)
        
        // punishmentsDelete.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/lolbans clear-cache punishments"));

        // TextComponent usersDelete = new TextComponent(Translation.translateColors("&", "&c [CLEAR USER CACHE]&r"));
        // usersDelete.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/lolbans clear-cache users"));
        
        // TextComponent cacheDelete = new TextComponent();
        // cacheDelete.addExtra(punishmentsDelete);
        // cacheDelete.addExtra(usersDelete);

       
        
        
        // users.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(Translation.translateColors("&", "&bUsed: &f"+getPlugin().getUserCache().memoryUsage(Unit.MEGABYTES).intValue()+"mb" + "\n" + "&bMax: &f" + getPlugin().getUserCache().getMaxMemoryUsage() / 1000 / 8 / 1024+"mb"+"\n"+"&bEntries: &f" + getPlugin().getUserCache().size()+"/"+getPlugin().getUserCache().getMaxSize())).create()));
        
        // sender.sendMessage(Translation.translateColors("&", Messages.prefix + "LolBans Statistics" + "&7 (Hover for detail)"));
        // sender.sendMessage(punishments);
        // sender.sendMessage(users);

        // if (sender.hasPermission("lolbans.clear-cache")) {
        //     sender.sendMessage(cacheDelete);
        // }
        return true;
    }

    public boolean runHelp(User sender, String[] args) {

        List<TextComponent> commands = new ArrayList<TextComponent>();

        // TODO: Paginate this bullshit
        for (AsyncCommand command : getPlugin().REGISTERED_COMMANDS.values()) {
            // if (commands.size() > pageSize)
                // continue;
            // Let's create a cool hover message!
            if (command == null) continue;
            if (command.getName() == null || command.getDescription() == null || command.getPermission() == null || command.getSyntax() == null) continue;
            TextComponent message = new TextComponent(Translation.translateColors("&", "&f» &b/" + command.getName() + "&7 (" + command.getPermission()+")"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Translation.translateColors("&", "&bCommand: &f" + command.getName() + "\n" 
                                                                                                                                  + "&bDescription: &f" + command.getDescription() + "\n"
                                                                                                                                  + "&bUsage: &f" + command.getSyntax().replace("Usage: ", "") )).create()));
            commands.add(message);
        }

        sender.sendMessage(Translation.translateColors("&", getPlugin().getLocaleProvider().getDefaultTranslation("prefix") + "Command Help" + "&7 (Hover for detail)"));
        for (TextComponent tclist : commands) {
            sender.sendMessage(tclist);
        }
        return true;
    }

}