package me.zacherycoleman.lolbans.Hacks;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.ReflectionUtil;
import me.zacherycoleman.lolbans.Hacks.LolbansCommandMap;
import me.zacherycoleman.lolbans.Hacks.LolbansPluginManager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;

public class Hacks
{
    // Inject
    public static void HackIn()
    {   
        // Get spigot's command map and the plugin manager fields as read/writable fields
        CommandMap commandMap = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "commandMap");
        PluginManager pluginManager = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "pluginManager");

        Hacks h = new Hacks();
        LolbansCommandMap lolCommandMap = new LolbansCommandMap(Bukkit.getServer(), (SimpleCommandMap)commandMap, h);
        LolbansPluginManager lolPluginManager = new LolbansPluginManager(pluginManager, h);

        // Overwrite md_5's core.
        ReflectionUtil.setProtectedValue(Bukkit.getServer(), "commandMap", lolCommandMap);
        ReflectionUtil.setProtectedValue(Bukkit.getServer(), "pluginManager", lolPluginManager);
    }

    // Eject
    public static void GetCaught()
    {
        // Get our command maps and such
        CommandMap commandMap = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "commandMap");
        PluginManager pluginManager = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "pluginManager");

        // If it's one of our command maps, restore the old one.
        if (commandMap instanceof LolbansCommandMap)
            ReflectionUtil.setProtectedValue(Bukkit.getServer(), "commandMap", ((LolbansCommandMap)commandMap).old);
        // if it's one of our plugin managers, restore the old one.
        if (pluginManager instanceof LolbansPluginManager)
            ReflectionUtil.setProtectedValue(Bukkit.getServer(), "pluginManager", ((LolbansPluginManager)pluginManager).origpm);
    }

    public boolean DispatchCommand(CommandSender sender, String commandLine)
    {
        // Get our main plugin class.
        final Main self = Main.getPlugin(Main.class);

        // TODO: Handle command dispatching.
        return false;
    }

    public void CallEvent(Event event) throws IllegalStateException
    {
        // Get our main plugin class.
        final Main self = Main.getPlugin(Main.class);

        self.getLogger().info(String.format("Executing %s...", event.getEventName()));

        // TODO: Handle all events.
    }
}