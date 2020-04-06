package com.ristexsoftware.lolbans.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


/**
 * This is a variant of the CommandExecutor which handles syntax errors
 * better than the stupid spigot syntax handler. This allows us to make
 * syntax messages more configurable with messages.yml and offers more
 * flexibility than the retarded spigot interface. This interface may
 * be made async as well to allow all commands in lolbans become async.
 */
public abstract class RistExCommand implements CommandExecutor
{   
    public abstract void onSyntaxError(CommandSender sender, Command command, String label, String[] args);
    public abstract boolean Execute(CommandSender sender, Command command, String label, String[] args);

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!this.Execute(sender, command, label, args))
            this.onSyntaxError(sender, command, label, args);
        return true;
    }
}