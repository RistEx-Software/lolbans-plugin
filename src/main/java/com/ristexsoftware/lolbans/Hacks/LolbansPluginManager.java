package com.ristexsoftware.lolbans.Hacks;

import java.io.File;
import java.util.Set;

// All the stupid dependencies needed for this dumb class.

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;

import com.ristexsoftware.lolbans.Hacks.Hacks;

/**
 * Synopsys: According to md_5 (see 
 * https://bukkit.org/threads/event-handler-to-listen-to-all-events-in-one-method.99068/#post-1323406)
 * we can replace the PluginManager with our own which uses reflection to replace it with our
 * own plugin manager that can catch **ALL** events and therefore we can follow them into C++.
 * This is a fancy trick to get all C++ sub-plugins to load the way we want. This file
 * implements the Pluginmanager class that will be reflected off of in Bukkit/Spigot and will
 * call the C++ event handler classes.
 * 
 * The way this works is we implement our own PluginManager class and our own Server class.
 * The server class replaces the global static in the org.bukkit.Bukkit class so we can override
 * the getPluginManager() function on it which returns our plugin manager.
 * 
 * I had to reimplement the entire class because SOMEONE wanted to make SimplePluginManager be
 * fucking fInAL so I can't reimplement it in this class and simply cast it oh nooooo that'd be
 * too fucking easy for me. Thanks md_5.
 */

class LolbansPluginManager implements PluginManager
{
	public Hacks lolhacks;
	public PluginManager origpm;

	LolbansPluginManager(PluginManager pm, Hacks hack)
	{
		this.origpm = pm;
		this.lolhacks = hack;
	}

	// Override the plugin manager's callEvent function.
	public void callEvent(Event event) throws IllegalStateException
	{
		// Send it to our C++ core
		lolhacks.CallEvent(event);

		// Send it to the real plugin manager.
		this.origpm.callEvent(event);
	}

	// Everything else is just implemented to make it all

	public void registerInterface(Class<? extends PluginLoader> loader) throws IllegalArgumentException
	{
		this.origpm.registerInterface(loader);
	}

	public Plugin getPlugin(String name)
	{
		return this.origpm.getPlugin(name);
	}

	public Plugin[] getPlugins()
	{
		return this.origpm.getPlugins();
	}

	public boolean isPluginEnabled(String name)
	{
		return this.origpm.isPluginEnabled(name);
	}

	public boolean isPluginEnabled(Plugin plugin)
	{
		return this.origpm.isPluginEnabled(plugin);
	}

	public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException
	{
		return this.origpm.loadPlugin(file);
	}

	public Plugin[] loadPlugins(File directory)
	{
		return this.origpm.loadPlugins(directory);
	}

	public void disablePlugins()
	{
		this.origpm.disablePlugins();
	}

	public void clearPlugins()
	{
		this.origpm.clearPlugins();
	}

	public void registerEvents(Listener listener, Plugin plugin)
	{
		this.origpm.registerEvents(listener, plugin);
	}

	public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin)
	{
		this.origpm.registerEvent(event, listener, priority, executor, plugin);
	}

	public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin, boolean ignoreCancelled)
	{
		this.origpm.registerEvent(event, listener, priority, executor, plugin, ignoreCancelled);
	}

	public void enablePlugin(Plugin plugin)
	{
		this.origpm.enablePlugin(plugin);
	}

	public void disablePlugin(Plugin plugin)
	{
		this.origpm.disablePlugin(plugin);
	}

	public Permission getPermission(String name)
	{
		return this.origpm.getPermission(name);
	}

	public void addPermission(Permission perm)
	{
		this.origpm.addPermission(perm);
	}

	public void removePermission(Permission perm)
	{
		this.origpm.removePermission(perm);
	}

	public void removePermission(String name)
	{
		this.origpm.removePermission(name);
	}

	public Set<Permission> getDefaultPermissions(boolean op)
	{
		return this.origpm.getDefaultPermissions(op);
	}

	public void recalculatePermissionDefaults(Permission perm)
	{
		this.origpm.recalculatePermissionDefaults(perm);
	}

	public void subscribeToPermission(String permission, Permissible permissible)
	{
		this.origpm.subscribeToPermission(permission, permissible);
	}

	public void unsubscribeFromPermission(String permission, Permissible permissible)
	{
		this.origpm.unsubscribeFromPermission(permission, permissible);
	}

	public Set<Permissible> getPermissionSubscriptions(String permission)
	{
		return this.origpm.getPermissionSubscriptions(permission);
	}

	public void subscribeToDefaultPerms(boolean op, Permissible permissible)
	{
		this.origpm.subscribeToDefaultPerms(op, permissible);
	}

	public void unsubscribeFromDefaultPerms(boolean op, Permissible permissible)
	{
		this.origpm.unsubscribeFromDefaultPerms(op, permissible);
	}

	public Set<Permissible> getDefaultPermSubscriptions(boolean op)
	{
		return this.origpm.getDefaultPermSubscriptions(op);
	}

	public Set<Permission> getPermissions()
	{
		return this.origpm.getPermissions();
	}

	public boolean useTimings()
	{
		return this.origpm.useTimings();
	}

}