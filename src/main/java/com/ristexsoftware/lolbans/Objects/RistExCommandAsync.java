package com.ristexsoftware.lolbans.Objects;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabCompleter;

import com.ristexsoftware.lolbans.Main;

public abstract class RistExCommandAsync extends Command implements PluginIdentifiableCommand
{
	private Plugin owner;
	private TabCompleter completer;

	/**
	 * Create a new command for the associated plugin
	 * @param CommandName The name of the command the user will execute
	 * @param owner The plugin that owns this command.
	 */
	public RistExCommandAsync(String CommandName, Plugin owner)
	{
		super(CommandName);
		this.owner = owner;
	}

	/**
	 * Show a syntax error message when the user fails to enter thr proper syntax
	 * @param sender Who failed the command
	 * @param label The command itself
	 * @param args Arguments provided to the command
	 */
	public abstract void onSyntaxError(CommandSender sender, String label, String[] args);
	/**
	 * Execute the command itself (part of the derived class)
	 * @param sender Who is executing the command
	 * @param commandLabel The command string triggering this command
	 * @param args The arguments provided to this command
	 * @return Whether or not the command succeeded, returning false will trigger onSyntaxError()
	 */
	public abstract boolean Execute(CommandSender sender, String commandLabel, String[] args);

	/**
	 * This is a vastly simplified command class. We only check if the plugin is enabled before
	 * we execute whereas spigot's `PluginCommand` will attempt to check permissions beforehand.
	 * 
	 * This also allows us to do async commands if we so desire and it nulls the point of
	 * CommandExecutors because they were fucking pointless to begin with.
	 * @param sender The person executing the command
	 * @param commandLabel The command that was executed
	 * @param args The arguments given to the command.
	 * @return True if the command succeeded, otherwise it will execute onSyntaxError().
	 */
	@Override
	public final boolean execute(CommandSender sender, String commandLabel, String[] args)
	{
		if (!this.owner.isEnabled())
			throw new CommandException(String.format("Cannot execute command \"%s\" in plugin %s - plugin is disabled.", commandLabel, this.owner.getDescription().getFullName()));
			
		RistExCommandAsync self = this;
		FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() 
		{
			@Override
			public Boolean call()
			{
				try
				{
					if (!self.Execute(sender, commandLabel, args))
						self.onSyntaxError(sender, commandLabel, args);
				}
				catch (Throwable ex)
				{
					throw new CommandException("Unhandled exception executing command '" + commandLabel + "' in plugin " + self.owner.getDescription().getFullName(), ex);
				}
				return true;
			}
		});

		Main.pool.execute(t);
		
		return true;
	}
	
	/**
	 * Gets the owner of this PluginCommand
	 *
	 * @return Plugin that owns this command
	 */
    @Override
	public Plugin getPlugin() 
	{
		return this.owner;
	}

	/**
     * Sets the {@link CommandExecutor} to run when parsing this command
     * NOTE: This function does nothing.
     * @param executor New executor to run
     */
	public void setExecutor(CommandExecutor executor) 
	{
		// no-op
    }

    /**
	 * Gets the {@link CommandExecutor} associated with this command
	 * NOTE: These commands will never have CommandExecutors and will
	 * *ALWAYS* return the plugin itself.
	 * @return Plugin.
	 */
	public CommandExecutor getExecutor() 
	{
		return this.owner;
    }

    /**
     * Sets the {@link TabCompleter} to run when tab-completing this command.
     * <p>
     * If no TabCompleter is specified, and the command's executor implements
     * TabCompleter, then the executor will be used for tab completion.
     *
     * @param completer New tab completer
     */
	public void setTabCompleter(TabCompleter completer) 
	{
        this.completer = completer;
    }

    /**
     * Gets the {@link TabCompleter} associated with this command.
     *
     * @return TabCompleter object linked to this command
     */
	public TabCompleter getTabCompleter() 
	{
        return this.completer;
    }
	
	/**
     * {@inheritDoc}
     * <p>
     * Delegates to the tab completer if present.
     * <p>
     * If it is not present or returns null, will delegate to the current
     * command executor if it implements {@link TabCompleter}. If a non-null
     * list has not been found, will default to standard player name
     * completion in {@link
     * Command#tabComplete(CommandSender, String, String[])}.
     * <p>
     * This method does not consider permissions.
     *
     * @throws CommandException if the completer or executor throw an
     *     exception during the process of tab-completing.
     * @throws IllegalArgumentException if sender, alias, or args is null
     */
    @Override
	public java.util.List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws CommandException, IllegalArgumentException 
	{
		if (sender == null || alias == null || args == null)
			throw new NullPointerException("arguments to tabComplete cannot be null");

        List<String> completions = null;
		try 
		{
			if (completer != null)
				completions = completer.onTabComplete(sender, this, alias, args);
		} 
		catch (Throwable ex) 
		{
            StringBuilder message = new StringBuilder();
            message.append("Unhandled exception during tab completion for command '/").append(alias).append(' ');
            for (String arg : args) 
				message.append(arg).append(' ');
				
            message.deleteCharAt(message.length() - 1).append("' in plugin ").append(owner.getDescription().getFullName());
            throw new CommandException(message.toString(), ex);
        }

        if (completions == null) 
			return super.tabComplete(sender, alias, args);
			
        return completions;
    }

	/**
	 * Convert this command name to a string
	 * @return the human readable name of the class
	 */
	@Override
	public String toString()
	{
        StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(", ").append(owner.getDescription().getFullName()).append(')');
        return stringBuilder.toString();
    }
}