package me.zacherycoleman.lolbans.Hacks;

import java.util.Arrays;

import me.zacherycoleman.lolbans.Main;
import me.zacherycoleman.lolbans.Utils.ReflectionUtil;
import me.zacherycoleman.lolbans.Hacks.LolbansPluginManager;
import me.zacherycoleman.lolbans.Listeners.PlayerEventListener;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;

public class Hacks implements Listener
{
    private static boolean ReflectedInjected = true;
    private static Hacks hacks = new Hacks();
    // Inject
    public static void HackIn(JavaPlugin plugin)
    {   
        // Get spigot's command map and the plugin manager fields as read/writable fields
        PluginManager pluginManager = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "pluginManager");
        // Allocate our new command map.
        LolbansPluginManager lolPluginManager = new LolbansPluginManager(pluginManager, Hacks.hacks);

        try 
        {
            // Thanks to how md_5's retardation programming, after this commit you can no longer use reflection
            // to trap the callEvent function of PluginManager. This effectively makes it near
            // impossible to get things to work the way they should and the event API is absolute garbage by design.
            ReflectionUtil.setProtectedValue(Bukkit.getServer(), "pluginManager", (PluginManager)lolPluginManager);
        }
        catch (IllegalArgumentException ex)
        {
            ReflectedInjected = false;
            // So instead, we must manually iterate every class in the plugin and request to be subscribed to it
            // using a class graph walker. We use a spigot instance that was written after this commit was made.:
            // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/commits/af1c0139f56e444dbcaf191664829a33eb4dfd59#src/main/java/org/bukkit/craftbukkit/CraftServer.java
            //
            // The problem with scanning all loaded classes to register them is just that.
            // The classes have to be loaded, we might be able to re-scan when a plugin is loaded
            // but I'm not sure how much I want to support this behavior.
            ClassInfoList events = new ClassGraph()
                                        .enableClassInfo()
                                        .scan() //you should use try-catch-resources instead
                                        .getClassInfo(Event.class.getName())
                                        .getSubclasses()
                                        .filter(info -> !info.isAbstract());

            // Our dummy event listener we use
            Listener listener = new Listener() {};
            EventExecutor executor = (ignored, event) -> Hacks.hacks.CallEvent(event);

            // Attempt to listen to all events.
            try 
            {
                for (ClassInfo event : events) 
                {
                    //noinspection unchecked
                    Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(event.getName());
               
                    if (Arrays.stream(eventClass.getDeclaredMethods()).anyMatch(method ->
                            method.getParameterCount() == 0 && method.getName().equals("getHandlers"))) 
                    {
                        //We could do this further filtering on the ClassInfoList instance instead,
                        //but that would mean that we have to enable method info scanning.
                        //I believe the overhead of initializing ~20 more classes
                        //is better than that alternative.
                   
                        Bukkit.getPluginManager().registerEvent(eventClass, listener,
                                EventPriority.NORMAL, executor, plugin);
                    }
                }
            } 
            catch (ClassNotFoundException e) 
            {
                throw new AssertionError("Scanned class wasn't found", e);
            }
            
            //String[] eventNames = events.stream()
            //        .map(info -> info.getName().substring(info.getName().lastIndexOf('.') + 1))
            //        .toArray(String[]::new);
        }
    }

    // Eject
    public static void GetCaught()
    {
        // If we didn't inject via reflection, just return.
        if (!Hacks.ReflectedInjected)
            return;

        // Get our plugin manager
        PluginManager pluginManager = ReflectionUtil.getProtectedValue(Bukkit.getServer(), "pluginManager");

        // if it's one of our plugin managers, restore the old one.
        if (pluginManager instanceof LolbansPluginManager)
            ReflectionUtil.setProtectedValue(Bukkit.getServer(), "pluginManager", ((LolbansPluginManager)pluginManager).origpm);
    }

    public void CallEvent(Event event) throws IllegalStateException
    {
        // Get our main plugin class.
        final Main self = Main.getPlugin(Main.class);

        //self.getLogger().info(String.format("Executing %s...", event.getEventName()));

        // Listen to our player event.
        if (event instanceof PlayerEvent)
            PlayerEventListener.OnPlayerEvent((PlayerEvent)event);

        if (event instanceof EntityEvent)
            PlayerEventListener.OnEntityEvent((EntityEvent)event);
    }
}