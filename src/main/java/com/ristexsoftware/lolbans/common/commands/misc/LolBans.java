package com.ristexsoftware.lolbans.common.commands.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ristexsoftware.knappy.translation.Translation;
import com.ristexsoftware.knappy.util.NumberUtil;
import com.ristexsoftware.lolbans.api.User;
import com.ristexsoftware.lolbans.api.command.Arguments;
import com.ristexsoftware.lolbans.api.command.AsyncCommand;
import com.ristexsoftware.lolbans.api.configuration.Messages;
import com.ristexsoftware.lolbans.common.commands.ban.Ban;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class LolBans extends AsyncCommand {

    public LolBans(com.ristexsoftware.lolbans.api.LolBans plugin) {
        super("lolbans", plugin);
        setPermission("lolbans.plugin");
        setDescription("The LolBans help and reload command");
        setSyntax(Messages.getMessages().getConfig().getString("syntax.lolbans"));
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
            return false;
        Arguments a = new Arguments(args);
        a.requiredString("subCommand");
        String subCommand = a.get("subCommand").toLowerCase();
        String[] subArgs = Arrays.copyOfRange(a.getUnparsedArgs().toArray(new String[a.getUnparsedArgs().size()]), 1,
                a.getUnparsedArgs().size());
        switch (subCommand) {
            case "help":
                return runHelp(sender, subArgs);
            case "reload":
                return false;
            default:
                return false;
        }
    }

    public boolean runHelp(User sender, String[] args) {

        Integer page = null;
        if (args.length > 0 && NumberUtil.isNumeric(args[0]))
            page = Integer.valueOf(args[0]);
        if (page == null)
            page = 1;

        // Convert to SQL-readable page - is one minus user-readable.
        page--;
        if (page < 0)
            return false;

        int count = getPlugin().REGISTERED_COMMANDS.size();
        int pageSize = 8;
        if (count < page * pageSize)
            return false;

        List<TextComponent> commands = new ArrayList<TextComponent>();

        // TODO: Paginate this bullshit
        for (AsyncCommand command : getPlugin().REGISTERED_COMMANDS.values()) {
            // if (commands.size() > pageSize)
                // continue;
            // Let's create a cool hover message!
            if (command == null) continue;
            if (command.getName() == null || command.getDescription() == null || command.getPermission() == null || command.getSyntax() == null) continue;
            TextComponent message = new TextComponent(Translation.translateColors("&", "&f» &b/" + command.getName() + " &7(" + command.getPermission()+")"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Translation.translateColors("&", "&bCommand: &f" + command.getName() + "\n" 
                                                                                                                                  + "&bDescription: &f" + command.getDescription() + "\n"
                                                                                                                                  + "&bUsage: &f" + command.getSyntax().replace("Usage: ", "") )).create()));
            commands.add(message);
        }

        sender.sendMessage(Translation.translateColors("&", Messages.prefix + "Command Help" + "&7 (Hover for info)"));
        for (TextComponent tclist : commands) {
            sender.sendMessage(tclist);
        }
        return true;
    }

}