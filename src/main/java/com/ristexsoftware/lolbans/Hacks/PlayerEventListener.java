package com.ristexsoftware.lolbans.Hacks;

import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.PlayerInventory;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Objects.User;
import com.ristexsoftware.lolbans.Utils.Messages;

public class PlayerEventListener implements Listener
{

    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent event) 
    {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;

        PlayerMoveEvent E = (PlayerMoveEvent)event;
        Player p = E.getPlayer();
        // Put them in a box
        u.SpawnBox(false, null);
        
        Location from = E.getFrom();
        Location to = E.getTo();

        double x = Math.floor(from.getX());
        double z = Math.floor(from.getZ());
        double y = Math.floor(from.getY());

        Location loc = p.getLocation();
        loc.setY(loc.getY() - 2);

        // This will enforce their location server-side.
        if (Math.floor(to.getX()) != x || Math.floor(to.getZ()) != z || Math.floor(to.getY()) != y)
        {
            E.setCancelled(true);
            // TODO: Add {ISSUER}, {PLAYER}, and {REASON} to this message.
            try
            {
                p.sendMessage(Messages.Translate("Warn.WarnedOnAction", 
                    new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
                    {{
                    }}
                ));
            }
            catch (InvalidConfigurationException ex)
            {
                ex.printStackTrace();
            }
        }
    }

/*     @EventHandler
    public static void OnPlayerEvent(PlayerEvent event) throws InterruptedException, ExecutionException, InvalidConfigurationException
    {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
            
        // If they're trying to do literally anything, cancel it.
        if (event instanceof Cancellable)
        {
            // Ignore these events, they're critical to this event working.
            if (event instanceof PlayerKickEvent || event instanceof PlayerCommandPreprocessEvent || event instanceof PlayerCommandSendEvent)
                return;

            // Remind the player they're in a warned state.
            if (event instanceof AsyncPlayerChatEvent || event instanceof PlayerInventory)
                u.SendMessage(u.GetWarnMessage());

            // Cancel everything else.
            ((Cancellable)event).setCancelled(true);
        }
    } */

    // To ANYONE who has the pleasure of reading the source code of this plugin, i'm sorry.
    // I'm sorry that you have to read what's below, because it's honestly fucking dumb
    // and thanks to how the bukkit event API works I can't just register PlayerEvent because
    // who would do that anyway.........
    // And thanks to java 11 the hacks that allowed this no longer work! 
    // Fuck MD_5 and fuck Java
    @EventHandler
    public static void OnPlayerEvent2(AsyncPlayerChatEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
        u.SendMessage(u.GetWarnMessage());
    }

    @EventHandler
    public static void OnPlayerEvent3(PlayerInteractEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent4(PlayerDropItemEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
        u.SendMessage(u.GetWarnMessage());
    }

    @EventHandler
    public static void OnPlayerEvent5(PlayerItemConsumeEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent6(PlayerInteractEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent6(PlayerBedEnterEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent7(PlayerTeleportEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        if (event.getCause() != TeleportCause.PLUGIN)
            event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent8(PlayerGameModeChangeEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent9(PlayerToggleFlightEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public static void OnPlayerEvent10(PlayerEditBookEvent event) {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        
        // Ignore players not warned
        if (u == null || !u.IsWarn())
            return;
        event.setCancelled(true);
        u.SendMessage(u.GetWarnMessage());
    }

    @EventHandler
    public static void OnEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity e = event.getEntity();

        // Ensure the player is not an entity of some kind. They're currently exempt.
        if (e instanceof Player)
        {
            User u = Main.USERS.get(e.getUniqueId());
        
            // Ignore players not warned
            if (u == null || !u.IsWarn())
                return;

            if (event instanceof Cancellable)
            {
                u.SendMessage(u.GetWarnMessage());
                ((Cancellable)event).setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e)
    {

        if (e.getEntity() instanceof Player)
        {
            User u = Main.USERS.get(e.getEntity().getUniqueId());
        
            // Ignore players not warned
            if (u == null || !u.IsWarn())
                return;

            if (e.getCause() == EntityDamageEvent.DamageCause.VOID || e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION ||
             e.getCause() == EntityDamageEvent.DamageCause.FALL || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || 
             e.getCause() == EntityDamageEvent.DamageCause.CONTACT || e.getCause() == EntityDamageEvent.DamageCause.DROWNING)
                e.setCancelled(true);
        }
    }
}