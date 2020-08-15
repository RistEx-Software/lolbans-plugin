/* 
 *  LolBans - An advanced punishment management system made for Minecraft
 *  Copyright (C) 2019-2020 Justin Crawford <Justin@Stacksmash.net>
 *  Copyright (C) 2019-2020 Zachery Coleman <Zachery@Stacksmash.net>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ristexsoftware.lolbans.api.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ristexsoftware.lolbans.api.LolBans;
import com.ristexsoftware.lolbans.api.User;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.Setter;

public abstract class AsyncCommand {
	@Getter
	@Setter
	private String name;
	private String nextLabel;
	private String label;
	private List<String> aliases;
	@Getter
	protected String description;
	protected String usageMessage;
	@Getter
	private String permission;
	private String permissionMessage;

	@Getter
	private LolBans plugin = LolBans.getPlugin();

	/**
	 * Create a new command for the associated plugin
	 * 
	 * @param name The name of the command the user will execute
	 */
	public AsyncCommand(String name, LolBans plugin) {
		this.name = name;
		this.plugin = plugin;
	}

	/**
	 * Show a syntax error message when the user fails to enter thr proper syntax
	 * 
	 * @param sender Who failed the command
	 * @param label  The command itself
	 * @param args   Arguments provided to the command
	 */
	public abstract void onSyntaxError(User sender, String label, String[] args);

	public abstract List<String> onTabComplete(User sender, String[] args);

	/**
	 * Execute the command itself (part of the derived class)
	 * 
	 * @param sender       Who is executing the command
	 * @param commandLabel The command string triggering this command
	 * @param args         The arguments provided to this command
	 * @return Whether or not the command succeeded, returning false will trigger
	 *         onSyntaxError()
	 */
	public abstract boolean run(User sender, String commandLabel, String[] args) throws Throwable;

	/**
	 * This is a vastly simplified command class. We only check if the plugin is
	 * enabled before we execute whereas spigot's `PluginCommand` will attempt to
	 * check permissions beforehand.
	 * 
	 * This also allows us to do async commands if we so desire and it nulls the
	 * point of CommandExecutors because they were fucking pointless to begin with.
	 * 
	 * @param sender       The person executing the command
	 * @param commandLabel The command that was executed
	 * @param args         The arguments given to the command.
	 * @return True if the command succeeded, otherwise it will execute
	 *         onSyntaxError().
	 */
	public final boolean execute(User sender, String commandLabel, String[] args) {
		if (!this.plugin.isEnabled())
			throw new CommandException(String.format("Cannot execute command \"%s\" in plugin %s - plugin is disabled.",
					commandLabel, "LolBans"));

		AsyncCommand self = this;
		FutureTask<Boolean> t = new FutureTask<>(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				try {
					if (!self.run(sender, commandLabel, args))
						self.onSyntaxError(sender, commandLabel, args);
				} catch (Throwable ex) {
					getPlugin().getLogger().warning("Unhandled exception executing command '" + commandLabel + "'");
					ex.printStackTrace();
				}
				return true;	
			}
		});

		LolBans.getPlugin().getPool().execute(t);

		return true;
	}

	/**
	 * Convert this command name to a string
	 * 
	 * @return the human readable name of the class
	 */
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder(super.toString());
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		stringBuilder.append(", ").append("LolBans").append(')');
		return stringBuilder.toString();
	}

	/**
     * Sets a brief description of this command.
     *
     * @param description new command description
     * @return this command object, for chaining
     */
    @NotNull
    public AsyncCommand setDescription(@NotNull String description) {
        this.description = (description == null) ? "" : description;
        return this;
	}
	
	    /**
     * Sets the permission required by users to be able to perform this
     * command
     *
     * @param permission Permission name or null
     */
    public void setPermission(@Nullable String permission) {
        this.permission = permission;
	}
	
	/**
     * Sets the list of aliases to request on registration for this command.
     * This is not effective outside of defining aliases in the {@link
     * PluginDescriptionFile#getCommands()} (under the
     * `<code>aliases</code>' node) is equivalent to this method.
     *
     * @param aliases aliases to register to this command
     * @return this command object, for chaining
     */
    @NotNull
    public AsyncCommand setAliases(@NotNull List<String> aliases) {
        this.aliases = aliases;
        return this;
	}
	
	public List<String> getAliases() {
		// BungeeCord wants to complain about this nonsense so I have to do this shit.
		if (aliases == null)
			return Arrays.asList(new String[]{});
		return aliases;
	}

}