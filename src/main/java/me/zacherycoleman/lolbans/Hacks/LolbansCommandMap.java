package me.zacherycoleman.lolbans.Hacks;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.Server;

import me.zacherycoleman.lolbans.Hacks.Hacks;

public class LolbansCommandMap extends SimpleCommandMap
{
	public Hacks lolhacks;
	public SimpleCommandMap old;

	// We basically ignore everything about SimpleCommandMap and only use it
	// to make the JVM get a boner about type safety therefore we don't need
	// to call the SimpleCommandMap constructor (as it causes crashes anyway)
	public LolbansCommandMap(Server server, SimpleCommandMap fuckingjava, Hacks x)
	{
		super(server);

		this.old = fuckingjava;
		this.lolhacks = x;
	}

	// Stupid overrides we have to do
	@Override public void clearCommands() { this.old.clearCommands(); }

	@Override
	public boolean register(String fallbackPrefix, Command command)
	{
		return this.register(command.getName(), fallbackPrefix, command);
	}

	@Override
	public boolean register(String label, String fallbackPrefix, Command command)
	{
		// More md_5 fuckery: Don't register the fucking default commands on startup.
		if (this.old == null)
			return false;

		return this.old.register(label, fallbackPrefix, command);
	}

	@Override public void registerAll(String fallbackPrefix, List<Command> commands) { this.old.registerAll(fallbackPrefix, commands); }
	@Override public void registerServerAliases() { this.old.registerServerAliases(); }
	@Override public void setFallbackCommands() { this.old.setFallbackCommands(); }
	
	// TODO: Add the C++ commands to this map.
	@Override public Collection<Command> getCommands() { return this.old.getCommands(); }

	// Actual overrides we wanted to do in the first place
	@Override
	public boolean dispatch(CommandSender sender, String commandLine)
	{
		// C++ commands will always override Java commands because
		// we want to make these commands faster, this may include
		// things like /help, /version, and /plugins or whatever.
		boolean lolreturn = this.lolhacks.DispatchCommand(sender, commandLine);
		if (lolreturn)
			return lolreturn;

		return this.old.dispatch(sender, commandLine);
	}

	@Override
	public Command getCommand(String name)
	{
        return this.old.getCommand(name);
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String cmdLine)
	{
/* 		// TODO: return list of tab completes from C++ too
		List<String> cpptabby = Arrays.asList(this.xenic.OnTabComplete(sender, cmdLine));
		List<String> tabby = this.old.tabComplete(sender, cmdLine);

		List<String> ret = new ArrayList<String>(cpptabby);
		ret.addAll(tabby);

		for (int i = 0; i < ret.size(); ++i)
			System.out.println(ret.get(i));

        return ret; */
        return this.old.tabComplete(sender, cmdLine);
	}
}