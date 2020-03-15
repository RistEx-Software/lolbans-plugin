package com.ristexsoftware.lolbans.Listeners;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.PlayerInventory;

import com.ristexsoftware.lolbans.Main;
import com.ristexsoftware.lolbans.Utils.Configuration;
import com.ristexsoftware.lolbans.Utils.DatabaseUtil;
import com.ristexsoftware.lolbans.Utils.User;
import com.ristexsoftware.lolbans.Utils.Messages;
import com.ristexsoftware.lolbans.Utils.TimeUtil;

public class PlayerEventListener 
{
    private static Main self = Main.getPlugin(Main.class);

    public static void OnPlayerEvent(PlayerEvent event) throws InterruptedException, ExecutionException, InvalidConfigurationException
    {
        User u = Main.USERS.get(event.getPlayer().getUniqueId());
        // Ignore players not warned

        if (u == null || !u.IsWarn())
            return;

        if (event instanceof PlayerMoveEvent)
        {
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
            // Exit
            return;
        }

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
    }

    // Cancel most events that involve the player (they may not attack or damage people/entities)
    public static void OnEntityEvent(EntityEvent event)
    {
        Entity e = event.getEntity();

        // Ensure the player is not an entity of some kind. They're currently exempt.
        if (e instanceof Player)
        {
            User u = Main.USERS.get(((Player)e).getUniqueId());
            // Ignore players not warned
            if (u == null || !u.IsWarn())
                return;

            if (event instanceof Cancellable)
            {
                u.SendMessage(u.GetWarnMessage());
                ((Cancellable)event).setCancelled(true);
            }
        }

        // Ensure the player is not being affected in some way by a few events
        if (event instanceof EntityDamageByEntityEvent)
        {
            Entity damager = ((EntityDamageByEntityEvent)event).getDamager();

            if (damager instanceof Player)
            {
                User u = Main.USERS.get(((Player)damager).getUniqueId());
                // Ignore players not warned
                if (u == null || !u.IsWarn())
                    return;
    
                if (event instanceof Cancellable)
                {
                    u.SendMessage(u.GetWarnMessage());
                    ((EntityDamageByEntityEvent)event).setCancelled(true);
                }
            }
        }
    }
}